package com.westlake.air.pecs.parser;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.transition.Annotation;
import com.westlake.air.pecs.domain.db.FragmentInfo;
import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.domain.db.PeptideDO;
import com.westlake.air.pecs.domain.db.TaskDO;
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
public class LibraryTsvParser extends BaseLibraryParser {

    @Autowired
    TaskService taskService;

    public final Logger logger = LoggerFactory.getLogger(LibraryTsvParser.class);

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
    private static String Detecting = "detecting_transition";
    private static String Identifying = "identifying_transition";
    private static String Quantifying = "quantifying_transition";

    @Override
    public ResultDO parseAndInsert(InputStream in, LibraryDO library, TaskDO taskDO) {
        List<PeptideDO> transitions = new ArrayList<>();
        ResultDO<List<PeptideDO>> tranResult = new ResultDO<>(true);
        try {
            InputStreamReader isr = new InputStreamReader(in, "UTF-8");
            BufferedReader reader = new BufferedReader(isr);
            String line = reader.readLine();
            if (line == null) {
                return ResultDO.buildError(ResultCode.LINE_IS_EMPTY);
            }
            HashMap<String, Integer> columnMap = parseColumns(line);

            //开始插入前先清空原有的数据库数据
            ResultDO resultDOTmp = peptideService.deleteAllByLibraryId(library.getId());
            taskDO.addLog("删除旧数据完毕,开始文件解析");
            taskService.update(taskDO);

            if (resultDOTmp.isFailed()) {
                logger.error(resultDOTmp.getMsgInfo());
                return ResultDO.buildError(ResultCode.DELETE_ERROR);
            }

            int count = 0;
            PeptideDO lastPeptide = null;
            while ((line = reader.readLine()) != null) {
                ResultDO<PeptideDO> resultDO = parseTransition(line, columnMap, library);

                if (resultDO.isFailed()) {
                    tranResult.addErrorMsg(resultDO.getMsgInfo());
                    continue;
                }

                PeptideDO peptide = resultDO.getModel();
                if (lastPeptide == null) {
                    lastPeptide = peptide;
                } else {
                    //如果是同一个肽段下的不同离子片段
                    if (lastPeptide.getPeptideRef().equals(peptide.getPeptideRef()) && lastPeptide.getIsDecoy().equals(peptide.getIsDecoy())) {
                        for (String key : peptide.getFragmentMap().keySet()) {
                            lastPeptide.putFragment(key, peptide.getFragmentMap().get(key));
                        }
                    } else {
                        transitions.add(lastPeptide);
                        lastPeptide = peptide;
                    }
                }
            }
            peptideService.insertAll(transitions, false);
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
    private ResultDO<PeptideDO> parseTransition(String line, HashMap<String, Integer> columnMap, LibraryDO library) {
        ResultDO<PeptideDO> resultDO = new ResultDO<>(true);
        String[] row = line.split("\t");
        PeptideDO peptideDO = new PeptideDO();
        boolean isDecoy = !row[columnMap.get(IsDecoy)].equals("0");

        FragmentInfo fi = new FragmentInfo();
        peptideDO.setIsDecoy(isDecoy);
        peptideDO.setLibraryId(library.getId());
        peptideDO.setLibraryName(library.getName());
        peptideDO.setMz(Double.parseDouble(row[columnMap.get(PrecursorMz)]));
        fi.setMz(Double.parseDouble(row[columnMap.get(ProductMz)]));
        peptideDO.setRt(Double.parseDouble(row[columnMap.get(NormalizedRetentionTime)]));

        fi.setIntensity(Double.parseDouble(row[columnMap.get(ProductIonIntensity)]));
        peptideDO.setSequence(row[columnMap.get(PeptideSequence)]);
        peptideDO.setProteinName(row[columnMap.get(ProteinName)]);

        String annotations = row[columnMap.get(Annotation)].replaceAll("\"", "");
        if (annotations.contains("[")) {
            fi.setWithBrackets(true);
            annotations = annotations.replace("[", "").replace("]", "");
        }
        fi.setAnnotations(annotations);
        String fullName = row[columnMap.get(FullUniModPeptideName)];
        if (fullName == null) {
            logger.info("Full Peptide Name cannot be empty");
        } else {
            peptideDO.setFullName(row[columnMap.get(FullUniModPeptideName)]);
        }
        try {
            peptideDO.setCharge(Integer.parseInt(row[columnMap.get(PrecursorCharge)]));
        } catch (Exception e) {
            logger.error("Line插入错误(PrecursorCharge未知):" + line + ";");
            logger.error(e.getMessage());
        }
        peptideDO.setPeptideRef(peptideDO.getFullName() + "_" + peptideDO.getCharge());
        try {
            ResultDO<Annotation> annotationResult = parseAnnotation(fi.getAnnotations());
            Annotation annotation = annotationResult.getModel();
            fi.setAnnotation(annotation);
            fi.setCharge(annotation.getCharge());
            fi.setCutInfo(annotation.getType() + annotation.getLocation() + (annotation.getCharge() == 1 ? "" : ("^" + annotation.getCharge())));
            peptideDO.putFragment(fi.getCutInfo(), fi);
            resultDO.setModel(peptideDO);
        } catch (Exception e) {
            resultDO.setSuccess(false);
            logger.error("Line插入错误(Sequence未知):" + line + ";");
            resultDO.setMsgInfo("Line插入错误(Sequence未知):" + line + ";");
            logger.error(peptideDO.getLibraryId() + ":" + fi.getAnnotation(), e);
            return resultDO;
        }

        parseModification(peptideDO);

        return resultDO;
    }


}