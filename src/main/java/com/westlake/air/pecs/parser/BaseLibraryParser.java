package com.westlake.air.pecs.parser;

import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.transition.Annotation;
import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.domain.db.PeptideDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.parser.xml.AirXStream;
import com.westlake.air.pecs.service.PeptideService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.util.HashMap;
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

    public static final Pattern unimodPattern = Pattern.compile("([a-z])[\\(]unimod[\\:](\\d*)[\\)]");

    public abstract ResultDO parseAndInsert(InputStream in, LibraryDO library, TaskDO taskDO);

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
}
