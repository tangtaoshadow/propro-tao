package com.westlake.air.propro.algorithm.parser;

import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.peptide.Annotation;
import com.westlake.air.propro.domain.db.FragmentInfo;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.PeptideDO;
import com.westlake.air.propro.domain.db.TaskDO;
import com.westlake.air.propro.service.TaskService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static com.westlake.air.propro.utils.PeptideUtil.parseModification;
import static com.westlake.air.propro.utils.PeptideUtil.removeUnimod;

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
    private static String TransitionGroupId = "transition_group_id";
    private static String UniprotId = "uniprotid";
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
    public ResultDO parseAndInsert(InputStream in, LibraryDO library, HashSet<String> fastaUniqueSet, HashMap<String, PeptideDO> prmPepMap, String libraryId, TaskDO taskDO) {

        ResultDO<List<PeptideDO>> tranResult = new ResultDO<>(true);
        try {
            InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
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

            HashMap<String, PeptideDO> map = new HashMap<>();
            HashSet<String> fastaDropPep = new HashSet<>();
            HashSet<String> libraryDropPep = new HashSet<>();
            HashSet<String> fastaDropProt = new HashSet<>();
            HashSet<String> libraryDropProt = new HashSet<>();
            HashSet<String> uniqueProt = new HashSet<>();
            HashSet<String> prmPeptideRefSet = new HashSet<>(prmPepMap.keySet());
            while ((line = reader.readLine()) != null) {
                if (!prmPeptideRefSet.isEmpty() && !isPrmPeptideRef(line, columnMap, prmPeptideRefSet)) {
                    continue;
                }
                ResultDO<PeptideDO> resultDO = parseTransition(line, columnMap, library);

                if (resultDO.isFailed()) {
                    tranResult.addErrorMsg(resultDO.getMsgInfo());
                    continue;
                }

                PeptideDO peptide = resultDO.getModel();
                setUnique(peptide, fastaUniqueSet, fastaDropPep, libraryDropPep, fastaDropProt, libraryDropProt, uniqueProt);
                addFragment(peptide, map);
            }
            if (!fastaUniqueSet.isEmpty()) {
                logger.info("fasta额外检出：" + fastaDropPep.size() + "个PeptideSequence");
            }

            for (PeptideDO peptideDO : map.values()) {
                prmPeptideRefSet.remove(peptideDO.getPeptideRef());
            }
            library.setFastaDeWeightPepCount(fastaDropPep.size());
            library.setFastaDeWeightProtCount(getDropCount(fastaDropProt, uniqueProt));
            library.setLibraryDeWeightPepCount(libraryDropPep.size());
            library.setLibraryDeWeightProtCount(getDropCount(libraryDropProt, uniqueProt));

            List<PeptideDO> peptides = new ArrayList<>(map.values());
            if (libraryId != null && !libraryId.isEmpty()) {
                List<PeptideDO> irtPeps = peptideService.getAllByLibraryId(libraryId);
                for (PeptideDO peptideDO : irtPeps) {
                    if (map.containsKey(peptideDO.getPeptideRef() + "_" + peptideDO.getIsDecoy())) {
                        map.get(peptideDO.getPeptideRef() + "_" + peptideDO.getIsDecoy()).setProteinName("IRT");
                        continue;
                    }

                    prmPeptideRefSet.remove(peptideDO.getPeptideRef());
                    peptideDO.setProteinName("IRT");
                    peptideDO.setId(null);
                    peptideDO.setLibraryId(library.getId());
                    peptideDO.setLibraryName(library.getName());
                    peptides.add(peptideDO);
                }
            }

            peptideService.insertAll(peptides, false);
            tranResult.setModel(peptides);
            taskDO.addLog(peptides.size() + "条肽段数据插入成功,其中蛋白质种类有" + uniqueProt.size() + "个");
            taskService.update(taskDO);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tranResult;
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
        String[] row = StringUtils.splitByWholeSeparator(line, "\t");
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
        peptideDO.setProteinName(row[columnMap.get(ProteinName)].replace("DECOY_", ""));

        String annotations = row[columnMap.get(Annotation)].replaceAll("\"", "");
        fi.setAnnotations(annotations);
        String fullName = row[columnMap.get(FullUniModPeptideName)];//no target sequence
        String[] transitionGroupId = row[columnMap.get(TransitionGroupId)].split("_");
        if (fullName == null) {
            logger.info("Full Peptide Name cannot be empty");
        } else {
            if (!isDecoy) {
                peptideDO.setFullName(transitionGroupId[1]);
            } else {
                peptideDO.setFullName(transitionGroupId[2]);
            }
        }
        peptideDO.setTargetSequence(removeUnimod(peptideDO.getFullName()));
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
            fi.setCutInfo(annotation.toCutInfo());
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

    public ResultDO<HashMap<String, PeptideDO>> getPrmPeptideRef(InputStream in) {
        try {
            InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(isr);
            String line = reader.readLine();
            if (line == null) {
                return ResultDO.buildError(ResultCode.PRM_FILE_IS_EMPTY);
            }
            String[] columns = line.split(",");
            HashMap<String, Integer> columnMap = new HashMap<>();
            for (int i = 0; i < columns.length; i++) {
                columnMap.put(StringUtils.deleteWhitespace(columns[i].toLowerCase()), i);
            }

            /**
             * format check
             * peptide  charge  start[min]
             */
            if (!columnMap.containsKey("peptide") || !columnMap.containsKey("charge") || !columnMap.containsKey("start[min]")) {
                return ResultDO.buildError(ResultCode.PRM_FILE_FORMAT_NOT_SUPPORTED);
            }

            HashMap<String, PeptideDO> prmPepMap = new HashMap<>();
            while ((line = reader.readLine()) != null) {
                columns = line.split(",");
                String modSequence = columns[columnMap.get("peptide")]
                        .replace("[CAM]", "(UniMod:4)")
                        .replace(" (light)", "")
                        .replace("[+57.021464]", "(UniMod:4)")
                        .replace("[+57]", "(UniMod:4)")
                        .replace("[+15.994915]", "(UniMod:35)");
                if (prmPepMap.containsKey(modSequence)) {
                    continue;
                }
                PeptideDO peptideDO = new PeptideDO();
//                peptideDO.setPrmRtStart(Double.parseDouble(columns[columnMap.get("start[min]")]));
                if (columnMap.containsKey("rt[min]")){
                    peptideDO.setPrmRt(Double.parseDouble(columns[columnMap.get("rt[min]")]));
                }
                peptideDO.setIsDecoy(false);
                peptideDO.setFullName(modSequence);
                peptideDO.setSequence(removeUnimod(modSequence));
                peptideDO.setTargetSequence(peptideDO.getSequence());
                peptideDO.setCharge(Integer.parseInt(columns[columnMap.get("charge")]));
                peptideDO.setPeptideRef(modSequence + "_" + peptideDO.getCharge());
                prmPepMap.put(peptideDO.getPeptideRef() , peptideDO);
            }
            return new ResultDO<HashMap<String, PeptideDO>>(true).setModel(prmPepMap);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultDO.buildError(ResultCode.PRM_FILE_FORMAT_NOT_SUPPORTED);
        }
    }

    private boolean isPrmPeptideRef(String line, HashMap<String, Integer> columnMap, HashSet<String> peptideRefList) {
        String[] row = line.split("\t");
        String fullName = row[columnMap.get(FullUniModPeptideName)];
        String charge = row[columnMap.get(PrecursorCharge)];
        return peptideRefList.contains(fullName + "_" + charge);
    }

}
