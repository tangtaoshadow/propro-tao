package com.westlake.air.pecs.parser;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.transition.Annotation;
import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.domain.db.TransitionDO;
import com.westlake.air.pecs.service.TaskService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-07 11:07
 */
@Component("tsvParser")
public class TransitionTsvParser extends BaseTransitionParser {

    @Autowired
    TaskService taskService;

    public final Logger logger = LoggerFactory.getLogger(TransitionTsvParser.class);

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
    private static String Detecting = "detecting_transition";
    private static String Identifying = "identifying_transition";
    private static String Quantifying = "quantifying_transition";

    @Override
    public ResultDO parseAndInsert(InputStream in, LibraryDO library, boolean justReal, TaskDO taskDO) {
        List<TransitionDO> transitions = new ArrayList<>();
        ResultDO<List<TransitionDO>> tranResult = new ResultDO<>(true);
        try {
            InputStreamReader isr = new InputStreamReader(in, "UTF-8");
            BufferedReader reader = new BufferedReader(isr);
            String line = reader.readLine();
            if (line == null) {
                return ResultDO.buildError(ResultCode.LINE_IS_EMPTY);
            }
            HashMap<String, Integer> columnMap = parseColumns(line);

            //开始插入前先清空原有的数据库数据
            ResultDO resultDOTmp = transitionService.deleteAllByLibraryId(library.getId());
            taskDO.addLog("删除旧数据完毕,开始文件解析");
            taskService.update(taskDO);

            if (resultDOTmp.isFailed()) {
                logger.error(resultDOTmp.getMsgInfo());
                return ResultDO.buildError(ResultCode.DELETE_ERROR);
            }

            int count = 0;
            HashSet<String> proteinNameSet = new HashSet<>();
            HashSet<String> peptideRefSet = new HashSet<>();
            while ((line = reader.readLine()) != null) {
                ResultDO<TransitionDO> resultDO = parseTransition(line, columnMap, library, justReal);
                if (resultDO == null) {
                    continue;
                }
                if (resultDO.isFailed()) {
                    tranResult.addErrorMsg(resultDO.getMsgInfo());
                } else {

                    if(!proteinNameSet.contains(resultDO.getModel().getProteinName())){
                        proteinNameSet.add(resultDO.getModel().getProteinName());
                        resultDO.getModel().setMarkProtein(true);
                    }
                    if(!peptideRefSet.contains(resultDO.getModel().getPeptideRef())){
                        peptideRefSet.add(resultDO.getModel().getPeptideRef());
                        resultDO.getModel().setMarkPeptide(true);
                    }
                    transitions.add(resultDO.getModel());
                }
                //每存储满50000条存储一次,由于之前已经删除过原有的数据,因此不再删除原有数据
                if (transitions.size() > Constants.MAX_INSERT_RECORD_FOR_TRANSITION) {
                    count += Constants.MAX_INSERT_RECORD_FOR_TRANSITION;
                    transitionService.insertAll(transitions, false);
                    taskDO.addLog(count + "条数据插入成功");
                    taskService.update(taskDO);
                    transitions = new ArrayList<>();
                }
            }
            transitionService.insertAll(transitions, false);
            count += transitions.size();
            taskDO.addLog(count + "条数据插入成功");
            taskService.update(taskDO);
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
            columnMap.put(StringUtils.deleteWhitespace(columns[i].toLowerCase()), i);
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
    private ResultDO<TransitionDO> parseTransition(String line, HashMap<String, Integer> columnMap, LibraryDO library, boolean justReal) {
        ResultDO<TransitionDO> resultDO = new ResultDO<>(true);
        String[] row = line.split("\t");
        TransitionDO transitionDO = new TransitionDO();
        boolean isDecoy = !row[columnMap.get(IsDecoy)].equals("0");
        if (justReal && isDecoy) {
            return null;
        }
        transitionDO.setIsDecoy(isDecoy);
        transitionDO.setLibraryId(library.getId());
        transitionDO.setLibraryName(library.getName());
        transitionDO.setPrecursorMz(Double.parseDouble(row[columnMap.get(PrecursorMz)]));
        transitionDO.setProductMz(Double.parseDouble(row[columnMap.get(ProductMz)]));
        transitionDO.setRt(Double.parseDouble(row[columnMap.get(NormalizedRetentionTime)]));
        transitionDO.setName(row[columnMap.get(TransitionName)]);
        transitionDO.setIntensity(Double.parseDouble(row[columnMap.get(ProductIonIntensity)]));
        transitionDO.setSequence(row[columnMap.get(PeptideSequence)]);
        transitionDO.setProteinName(row[columnMap.get(ProteinName)]);

        if (columnMap.get(Detecting) != null) {
            transitionDO.setDetecting(!row[columnMap.get(Detecting)].equals("0"));
        }
        if (columnMap.get(Identifying) != null) {
            transitionDO.setIdentifying(!row[columnMap.get(Identifying)].equals("0"));
        }
        if (columnMap.get(Quantifying) != null) {
            transitionDO.setQuantifying(!row[columnMap.get(Quantifying)].equals("0"));
        }

        String annotations = row[columnMap.get(Annotation)].replaceAll("\"", "");
        if (annotations.contains("[")) {
            transitionDO.setWithBrackets(true);
            annotations = annotations.replace("[", "").replace("]", "");
        }
        transitionDO.setAnnotations(annotations);
        transitionDO.setFullName(row[columnMap.get(FullUniModPeptideName)]);
        try{
            transitionDO.setPrecursorCharge(Integer.parseInt(row[columnMap.get(PrecursorCharge)]));
        }catch (Exception e){
            logger.error(e.getMessage());
        }
        transitionDO.setPeptideRef(transitionDO.getFullName() + "_" + transitionDO.getPrecursorCharge());
        try {
            ResultDO<Annotation> annotationResult = parseAnnotation(transitionDO.getAnnotations());
            Annotation annotation = annotationResult.getModel();
            transitionDO.setAnnotation(annotation);
            transitionDO.setCutInfo(annotation.getType() + annotation.getLocation() + (annotation.getCharge() == 1 ? "" : ("^" + annotation.getCharge())));
            resultDO.setModel(transitionDO);
        } catch (Exception e) {
            resultDO.setSuccess(false);
            resultDO.setMsgInfo("Line插入错误(Sequence未知):" + line + ";");
            logger.error(transitionDO.getLibraryId() + ":" + transitionDO.getAnnotation(), e);
            return resultDO;
        }

        parseModification(transitionDO);

        return resultDO;
    }


}
