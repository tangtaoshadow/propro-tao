package com.westlake.air.swathplatform.parser;

import com.westlake.air.swathplatform.domain.bean.Annotation;
import com.westlake.air.swathplatform.domain.db.TransitionDO;
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
//    private static String FragmentType = "fragmenttype";
//    private static String FragmentCharge = "fragmentcharge";
//    private static String FragmentSeriesNumber = "fragmentseriesnumber";

    public List<TransitionDO> parse(InputStream in, String libraryId) {
        List<TransitionDO> transitions = new ArrayList<>();

        try {
            InputStreamReader isr = new InputStreamReader(in, "UTF-8");
            BufferedReader reader = new BufferedReader(isr);
            String line = reader.readLine();
            HashMap<String, Integer> columnMap = parseColumns(line);

            while ((line = reader.readLine()) != null) {
                transitions.add(parseTransition(line, columnMap, libraryId));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return transitions;
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
     * @param line
     * @param columnMap
     * @param libraryId
     * @return
     */
    private TransitionDO parseTransition(String line, HashMap<String, Integer> columnMap, String libraryId) {
        String[] row = line.split("\t");
        TransitionDO transitionDO = new TransitionDO();
        transitionDO.setLibraryId(libraryId);
        transitionDO.setPrecursorMz(row[columnMap.get(PrecursorMz)]);
        transitionDO.setProductMz(row[columnMap.get(ProductMz)]);
        transitionDO.setNormalizedRetentionTime(row[columnMap.get(NormalizedRetentionTime)]);
        transitionDO.setTransitionName(row[columnMap.get(TransitionName)]);
        transitionDO.setIsDecoy(!row[columnMap.get(IsDecoy)].equals("0"));
        transitionDO.setProductIonIntensity(row[columnMap.get(ProductIonIntensity)]);
        transitionDO.setPeptideSequence(row[columnMap.get(PeptideSequence)]);
        transitionDO.setProteinName(row[columnMap.get(ProteinName)]);
        transitionDO.setAnnotation(row[columnMap.get(Annotation)].replaceAll("\"", ""));
        transitionDO.setFullUniModPeptideName(row[columnMap.get(FullUniModPeptideName)]);
        transitionDO.setPrecursorCharge(Integer.parseInt(row[columnMap.get(PrecursorCharge)]));
        transitionDO.setPeptideGroupLabel(row[columnMap.get(PeptideGroupLabel)]);
        try {
            transitionDO.setAnnotations(parseAnnotation(transitionDO.getAnnotation()));
        } catch (Exception e) {
            logger.error(transitionDO.getLibraryId() + ":" + transitionDO.getAnnotation(), e);
        }

        return transitionDO;
    }

    private List<Annotation> parseAnnotation(String annotations) {
        List<Annotation> annotationList = new ArrayList<>();
        String[] annotationStrs = annotations.split(",");
        for (String annotationStr : annotationStrs) {
            Annotation annotation = new Annotation();
            String[] forDeviation = annotationStr.split("/");
            annotation.setDeviation(Double.parseDouble(forDeviation[1]));
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
            }else if (forCharge[0].contains("-")) {
                forAdjust = forCharge[0].split("-");
                if (forAdjust.length == 2) {
                    annotation.setAdjust(Integer.parseInt(nOrP + forAdjust[1]));
                }
            }else{
                forAdjust = forCharge;
            }

            String finalStr = forAdjust[0];
            //第一位必定是字母,代表fragment类型
            annotation.setType(finalStr.substring(0, 1));
            //最后一位如果是i则表示是同位素
            if(finalStr.endsWith("i")){
                annotation.setIsotope(true);
                annotation.setLocation(Integer.parseInt(finalStr.substring(1, finalStr.length()-1)));
            }else{
                annotation.setLocation(Integer.parseInt(finalStr.substring(1, finalStr.length())));
            }
            annotationList.add(annotation);
        }
        return annotationList;
    }
}
