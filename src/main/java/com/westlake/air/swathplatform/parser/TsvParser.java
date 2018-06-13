package com.westlake.air.swathplatform.parser;

import com.westlake.air.swathplatform.constants.ResultCode;
import com.westlake.air.swathplatform.domain.ResultDO;
import com.westlake.air.swathplatform.domain.bean.Annotation;
import com.westlake.air.swathplatform.domain.db.LibraryDO;
import com.westlake.air.swathplatform.domain.db.TransitionDO;
import com.westlake.air.swathplatform.parser.model.chemistry.Residue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-07 11:07
 */
@Component
public class TsvParser {

    public final Logger logger = LoggerFactory.getLogger(TsvParser.class);

    private static String PrecursorMz = "precursormz";
    private static String ProductMz = "productmz";
    private static String NormalizedRetentionTime = "tr_recalibrated";
    private static String TransitionName = "transition_name";
    private static String IsDecoy = "decoy";
    private static String ProductIonIntensity = "libraryintensity";
    private static String PeptideSequence = "peptidesequence";
    private static String ProteinName = "proteinname";
    private static String Annotation = "annotation";
    private static String FullUniModPeptideName = "fullunimodpeptidename";
    private static String PrecursorCharge = "precursorcharge";
    private static String PeptideGroupLabel = "peptidegrouplabel";
    private static String TransitionGroupId = "transition_group_id";
//    private static String FragmentType = "fragmenttype";
//    private static String FragmentCharge = "fragmentcharge";
//    private static String FragmentSeriesNumber = "fragmentseriesnumber";

    public ResultDO<List<TransitionDO>> parse(InputStream in, LibraryDO library) {
        List<TransitionDO> transitions = new ArrayList<>();
        ResultDO<List<TransitionDO>> tranResult = new ResultDO<>(true);
        try {
            InputStreamReader isr = new InputStreamReader(in, "UTF-8");
            BufferedReader reader = new BufferedReader(isr);
            String line = reader.readLine();
            HashMap<String, Integer> columnMap = parseColumns(line);

            while ((line = reader.readLine()) != null) {
                ResultDO<TransitionDO> resultDO = parseTransition(line, columnMap, library);
                if(resultDO.isFailured()){
                    tranResult.addErrorMsg(resultDO.getMsgInfo());
                }else{
                    transitions.add(resultDO.getModel());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        tranResult.setModel(transitions);
        return tranResult;
    }

    private HashMap<String, Integer> parseColumns(String line) {
        String[] columns = line.split("\t");
        HashMap<String, Integer> columnMap = new HashMap<>();
        for (int i = 0; i < columns.length; i++) {
            columnMap.put(columns[i].toLowerCase(), i);
        }
        return columnMap;
    }

    /**
     * 从TSV文件中解析出每一行数据
     *
     * @param line
     * @param columnMap
     * @param library
     * @return
     */
    private ResultDO<TransitionDO> parseTransition(String line, HashMap<String, Integer> columnMap, LibraryDO library) {
        ResultDO<TransitionDO> resultDO = new ResultDO<>(true);
        String[] row = line.split("\t");
        TransitionDO transitionDO = new TransitionDO();
        transitionDO.setLibraryId(library.getId());
        transitionDO.setLibraryName(library.getName());
        transitionDO.setPrecursorMz(Double.parseDouble(row[columnMap.get(PrecursorMz)]));
        transitionDO.setProductMz(Double.parseDouble(row[columnMap.get(ProductMz)]));
        transitionDO.setNormalizedRetentionTime(Double.parseDouble(row[columnMap.get(NormalizedRetentionTime)]));
        transitionDO.setTransitionName(row[columnMap.get(TransitionName)]);
        transitionDO.setIsDecoy(!row[columnMap.get(IsDecoy)].equals("0"));
        transitionDO.setProductIonIntensity(Double.parseDouble(row[columnMap.get(ProductIonIntensity)]));
        transitionDO.setPeptideSequence(row[columnMap.get(PeptideSequence)]);
        transitionDO.setProteinName(row[columnMap.get(ProteinName)]);
        transitionDO.setAnnotation(row[columnMap.get(Annotation)].replaceAll("\"", ""));
        transitionDO.setFullUniModPeptideName(row[columnMap.get(FullUniModPeptideName)]);
        transitionDO.setPrecursorCharge(Integer.parseInt(row[columnMap.get(PrecursorCharge)]));
        //兼容在某些TSV格式中decoy的PeptideGroupLabel被描述为TransitionGroupId
        if (columnMap.get(PeptideGroupLabel) != null) {
            transitionDO.setPeptideGroupLabel(row[columnMap.get(PeptideGroupLabel)]);
        } else if (columnMap.get(TransitionGroupId) != null) {
            transitionDO.setPeptideGroupLabel(row[columnMap.get(TransitionGroupId)]);
        }
        try {
            ResultDO<List<Annotation>> annotationResult = parseAnnotation(transitionDO.getAnnotation());
            transitionDO.setAnnotations(annotationResult.getModel());
            if(annotationResult.isFailured()){
                resultDO.setSuccess(false);
                resultDO.setMsgInfo("Line插入错误:"+transitionDO.getPeptideSequence()+";"+annotationResult.getMsgInfo());
                return resultDO;
            }
            resultDO.setModel(transitionDO);
        } catch (Exception e) {
            resultDO.setSuccess(false);
            resultDO.setMsgInfo("Line插入错误(Sequence未知):"+line+";");
            logger.error(transitionDO.getLibraryId() + ":" + transitionDO.getAnnotation(), e);
        }


        return resultDO;
    }

    private ResultDO<List<Annotation>> parseAnnotation(String annotations) {
        ResultDO<List<Annotation>> resultDO = new ResultDO<>(true);
        List<Annotation> annotationList = new ArrayList<>();
        String[] annotationStrs = annotations.split(",");
        try {
            for (String annotationStr : annotationStrs) {
                Annotation annotation = new Annotation();
                String[] forDeviation = annotationStr.split("/");
                annotation.setDeviation(Double.parseDouble(forDeviation[1]));

                if (forDeviation[0].endsWith("i")) {
                    annotation.setIsotope(true);
                    forDeviation[0] = forDeviation[0].replace("i", "");
                }

                String[] forCharge = forDeviation[0].split("\\^");
                if (forCharge.length == 2) {
                    annotation.setCharge(Integer.parseInt(forCharge[1]));
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
                String location = finalStr.substring(1, finalStr.length());
                if (!location.isEmpty()) {
                    annotation.setLocation(Integer.parseInt(location));
                }
                annotationList.add(annotation);
            }
        } catch (Exception e) {
            resultDO.setSuccess(false);
            resultDO.setErrorResult(ResultCode.PARSE_ERROR.getCode(), "解析Annotation错误,Annotation:"+annotations);
        } finally {
            resultDO.setModel(annotationList);
        }
        return resultDO;
    }
}
