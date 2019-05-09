package com.westlake.air.propro.algorithm.parser;

import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.peptide.Annotation;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.PeptideDO;
import com.westlake.air.propro.domain.db.TaskDO;
import com.westlake.air.propro.algorithm.parser.xml.AirXStream;
import com.westlake.air.propro.service.PeptideService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-26 21:13
 */
public abstract class BaseLibraryParser {

    public final Logger logger = LoggerFactory.getLogger(BaseLibraryParser.class);

    @Autowired
    AirXStream airXStream;
    @Autowired
    PeptideService peptideService;

    boolean drop = true;
    public static final Pattern unimodPattern = Pattern.compile("([a-z])[\\(]unimod[\\:](\\d*)[\\)]");

    public abstract ResultDO parseAndInsert(InputStream in, LibraryDO library, HashSet<String> fastaUniqueSet, HashMap<String, PeptideDO> prmPepMap, String libraryId, TaskDO taskDO);

    protected ResultDO<Annotation> parseAnnotation(String annotations) {
        ResultDO<Annotation> resultDO = new ResultDO<>(true);
        String[] annotationStrs = annotations.split(",");
        Annotation annotation = new Annotation();

        try {
            String annotationStr = annotationStrs[0];
            if(StringUtils.startsWith(annotationStr,"[")){
                annotation.setIsBrotherIcon(true);
                annotationStr = annotationStr.replace("[","");
                annotationStr = annotationStr.replace("]","");
            }
            String[] forDeviation = annotationStr.split("/");
            if (forDeviation.length > 1) {
                annotation.setDeviation(Double.parseDouble(forDeviation[1]));
            }

            if (forDeviation[0].endsWith("i")) {
                annotation.setIsotope(true);
                forDeviation[0] = forDeviation[0].replace("i", "");
            }

            String[] forCharge = forDeviation[0].split("\\^");
            if (forCharge.length == 2) {
                annotation.setCharge(Integer.parseInt(forCharge[1]));
            }else if (forDeviation[0].contains("(")){
                String[] msmsCutoff = forDeviation[0].split("\\(");
                annotation.setCharge(Integer.parseInt(msmsCutoff[1].substring(0,1)));
                forCharge[0] = msmsCutoff[0];
            }
            //默认为负,少数情况下校准值为正
            String nOrP = "-";
            String[] forAdjust;
            if (forCharge[0].contains("+")) {
                nOrP = "+";
                forAdjust = forCharge[0].split("\\+");
                if (forAdjust.length == 2) {
                    annotation.setAdjust(Integer.parseInt(nOrP + forAdjust[1]));
                }
            } else if (forCharge[0].contains("-")) {
                forAdjust = forCharge[0].split("-");
                if (forAdjust.length == 2) {
                    annotation.setAdjust(Integer.parseInt(nOrP + forAdjust[1]));
                }
            } else {
                forAdjust = forCharge;
            }

            String finalStr = forAdjust[0];
            //第一位必定是字母,代表fragment类型
            annotation.setType(finalStr.substring(0, 1));
            String location = finalStr.substring(1);
            if (!location.isEmpty()) {
                annotation.setLocation(Integer.parseInt(location));
            }
        } catch (Exception e) {
            resultDO.setSuccess(false);
            resultDO.setErrorResult(ResultCode.PARSE_ERROR.getCode(), "解析Annotation错误,Annotation:" + annotations);
        } finally {
            resultDO.setModel(annotation);
        }
        return resultDO;
    }

    /**
     * 解析出Modification的位置
     *
     * @param peptideDO
     */
    protected void parseModification(PeptideDO peptideDO) {
        //不论是真肽段还是伪肽段,fullUniModPeptideName字段都是真肽段的完整版
        String peptide = peptideDO.getFullName();
        peptide = peptide.toLowerCase();
        HashMap<Integer, String> unimodMap = new HashMap<>();

        while (peptide.contains("(unimod:") && peptide.indexOf("(unimod:") != 0) {
            Matcher matcher = unimodPattern.matcher(peptide);
            if (matcher.find()) {
                unimodMap.put(matcher.start(), matcher.group(2));
                peptide = StringUtils.replaceOnce(peptide, matcher.group(0), matcher.group(1));
            }
        }
        if (unimodMap.size() > 0) {
            peptideDO.setUnimodMap(unimodMap);
        }
    }

    /**
     * 从去除的Peptide推断的Protein中删除含有未去除Peptide的Protein
     * @param dropSet 非Unique的Peptide推断得到的ProtSet
     * @param uniqueSet Unique的Peptide推断得到的ProtSet
     * @return 非Unique蛋白的数量
     */
    protected int getDropCount(HashSet<String> dropSet, HashSet<String> uniqueSet){
        List<String> dropList = new ArrayList<>(dropSet);
        int dropCount = dropList.size();
        for(String prot: dropList){
            if (uniqueSet.contains(prot)){
                dropCount--;
            }
        }
        return dropCount;
    }

    protected void setUnique(PeptideDO peptide, HashSet<String> fastaUniqueSet, HashSet<String> fastaDropPep, HashSet<String> libraryDropPep, HashSet<String> fastaDropProt, HashSet<String> libraryDropProt, HashSet<String> uniqueProt){
        if(peptide.getProteinName().startsWith("1/")){
            if(!fastaUniqueSet.isEmpty() && !fastaUniqueSet.contains(peptide.getTargetSequence())){
                peptide.setIsUnique(false);
                fastaDropProt.add(peptide.getProteinName());
                fastaDropPep.add(peptide.getPeptideRef());
            }else {
                uniqueProt.add(peptide.getProteinName());
            }
        }else {
            peptide.setIsUnique(false);
            libraryDropProt.add(peptide.getProteinName());
            libraryDropPep.add(peptide.getPeptideRef());
        }
    }

    protected String removeUnimod(String fullName){
        if (fullName.contains("(")){
            String[] parts = fullName.replaceAll("\\(","|(").replaceAll("\\)","|").split("\\|");
            String sequence = "";
            for(String part: parts){
                if (part.startsWith("(")){
                    continue;
                }
                sequence += part;
            }
            return sequence;
        }else {
            return fullName;
        }
    }

    protected void addFragment(PeptideDO peptide, HashMap<String, PeptideDO> map){
        PeptideDO existedPeptide = map.get(peptide.getPeptideRef()+"_"+peptide.getIsDecoy());
        if(existedPeptide == null){
            map.put(peptide.getPeptideRef()+"_"+peptide.getIsDecoy(), peptide);
        }else{
            for (String key : peptide.getFragmentMap().keySet()) {
                existedPeptide.putFragment(key, peptide.getFragmentMap().get(key));
            }
        }
    }

    protected HashMap<String, Integer> parseColumns(String line) {
        String[] columns = line.split("\t");
        HashMap<String, Integer> columnMap = new HashMap<>();
        for (int i = 0; i < columns.length; i++) {
            columnMap.put(StringUtils.deleteWhitespace(columns[i].toLowerCase()), i);
        }
        return columnMap;
    }
}
