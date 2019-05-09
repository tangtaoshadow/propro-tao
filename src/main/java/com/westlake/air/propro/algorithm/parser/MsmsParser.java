package com.westlake.air.propro.algorithm.parser;

import com.westlake.air.propro.algorithm.formula.FragmentFactory;
import com.westlake.air.propro.constants.Constants;
import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.constants.Unimod;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.peptide.Annotation;
import com.westlake.air.propro.domain.bean.score.BYSeries;
import com.westlake.air.propro.domain.db.FragmentInfo;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.PeptideDO;
import com.westlake.air.propro.domain.db.TaskDO;
import com.westlake.air.propro.service.LibraryService;
import com.westlake.air.propro.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Created by Nico Wang
 * Time: 2019-04-23 14:52
 */
@Component("msmsParser")
public class MsmsParser extends BaseLibraryParser {

    @Autowired
    TaskService taskService;
    @Autowired
    LibraryService libraryService;
    @Autowired
    FragmentFactory fragmentFactory;

    public final Logger logger = LoggerFactory.getLogger(MsmsParser.class);
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

//            HashSet<String> fastaDropPep = new HashSet<>();
//            HashSet<String> libraryDropPep = new HashSet<>();
//            HashSet<String> fastaDropProt = new HashSet<>();
//            HashSet<String> libraryDropProt = new HashSet<>();
//            HashSet<String> uniqueProt = new HashSet<>();


            HashSet<String> prmSequenceSet = new HashSet<>();
            for (PeptideDO peptideDO : prmPepMap.values()) {
                prmSequenceSet.add(peptideDO.getSequence());
            }
            String lastSequence = "";
            List<String[]> sequenceInExps = new ArrayList<>();
            HashMap<String, PeptideDO> libPepMap = new HashMap<>();
            List<PeptideDO> irtPepList = new ArrayList<>();
            PeptideDO currentPeptideDO;
            while ((line = reader.readLine()) != null) {
                String[] row = line.split("\t");
                String sequence = row[columnMap.get("sequence")];
                if (sequence.equals(lastSequence)) {
                    //not empty means choosed
                    if (!sequenceInExps.isEmpty()) {
                        sequenceInExps.add(row);
                    } else {
                        continue;
                    }
                } else {
                    HashMap<String, PeptideDO> peptideDOMap = parseSequence(sequenceInExps, columnMap, prmPepMap, library);
                    List<PeptideDO> peptideList = new ArrayList<>(peptideDOMap.values());
                    if (!peptideList.isEmpty() && peptideList.get(0).getProteinName().equals("iRT")){
                        irtPepList.addAll(peptideList);
                    }
                    libPepMap.putAll(peptideDOMap);
                    //deal with same sequence
                    sequenceInExps.clear();
                    if (prmSequenceSet.isEmpty() || prmSequenceSet.contains(sequence)) {
                        sequenceInExps.add(row);
                    }
                    lastSequence = sequence;
                }
            }
            LibraryDO irtLibrary = new LibraryDO();
            irtLibrary.setName(library.getName()+"_iRT");
            irtLibrary.setType(LibraryDO.TYPE_IRT);
            libraryService.insert(irtLibrary);



//                ResultDO<PeptideDO> resultDO = parseMsmsLine(line, columnMap, library);

//                PeptideDO peptide = resultDO.getModel();
//                setUnique(peptide, fastaUniqueSet, fastaDropPep, libraryDropPep, fastaDropProt, libraryDropProt, uniqueProt);
//                addFragment(peptide, map);

//            if (!fastaUniqueSet.isEmpty()) {
//                logger.info("fasta额外检出：" + fastaDropPep.size() + "个PeptideSequence");
//            }

//            for (PeptideDO peptideDO: map.values()){
//                prmPeptideRefSet.remove(peptideDO.getPeptideRef());
//            }
//            library.setFastaDeWeightPepCount(fastaDropPep.size());
//            library.setFastaDeWeightProtCount(getDropCount(fastaDropProt, uniqueProt));
//            library.setLibraryDeWeightPepCount(libraryDropPep.size());
//            library.setLibraryDeWeightProtCount(getDropCount(libraryDropProt, uniqueProt));

            if (libraryId != null && !libraryId.isEmpty()) {
                List<PeptideDO> irtPeps = peptideService.getAllByLibraryId(libraryId);
                for (PeptideDO peptideDO: irtPeps){
                    if (libPepMap.containsKey(peptideDO.getPeptideRef())){
                        libPepMap.get(peptideDO.getPeptideRef()).setProteinName("IRT");
                        continue;
                    }
//                    if (prmPeptideRefSet.contains(peptideDO.getPeptideRef())){
//                        prmPeptideRefSet.remove(peptideDO.getPeptideRef());
//                    }
                    peptideDO.setProteinName("IRT");
                    peptideDO.setId(null);
                    peptideDO.setLibraryId(library.getId());
                    peptideDO.setLibraryName(library.getName());
                    libPepMap.put(peptideDO.getPeptideRef(), peptideDO);
                }
            }

            peptideService.insertAll(new ArrayList<>(libPepMap.values()), false);
            for (PeptideDO peptideDO: irtPepList){
                peptideDO.setLibraryId(irtLibrary.getId());
                peptideDO.setLibraryName(irtLibrary.getName());
                peptideDO.setId(null);
            }
            peptideService.insertAll(irtPepList, false);
            libraryService.countAndUpdateForLibrary(irtLibrary);
//            taskDO.addLog(libPepMap.size() + "条肽段数据插入成功,其中蛋白质种类有" + uniqueProt.size() + "个");
            taskService.update(taskDO);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tranResult;
    }

    private HashMap<String, PeptideDO> parseSequence(List<String[]> sequenceInExps, HashMap<String, Integer> columnMap, HashMap<String, PeptideDO> prmPepMap, LibraryDO library){
        HashMap<String, Float> scoreMap = new HashMap<>();
        HashMap<String,String[]> peptideRefMap = new HashMap<>();
        for (String[] row: sequenceInExps){
            String modifiedSequence = row[columnMap.get("modifiedsequence")].replace("_","");
            if (modifiedSequence.contains("(")){
                modifiedSequence = replaceModification(modifiedSequence);
            }

            String charge = row[columnMap.get("charge")];
            String peptideRef = modifiedSequence + "_" + charge;
            Float pepScore = Float.parseFloat(row[columnMap.get("pep")]);
            if (scoreMap.get(peptideRef) == null || scoreMap.get(peptideRef) < pepScore){
                scoreMap.put(peptideRef, pepScore);
                peptideRefMap.put(peptideRef, row);
            }
        }
        HashMap<String, PeptideDO> peptideDOMap = new HashMap<>();
        for (String peptideRef: peptideRefMap.keySet()){
            PeptideDO peptideDO = new PeptideDO();
            String[] row = peptideRefMap.get(peptideRef);
            String[] ionArray = row[columnMap.get("matches")].split(";");
            String[] massArray = row[columnMap.get("masses")].split(";");
            setFragmentInfo(peptideDO, ionArray, row[columnMap.get("intensities")].split(";"), massArray);
            peptideDO.setMz(Double.parseDouble(row[columnMap.get("m/z")]));
            peptideDO.setPeptideRef(peptideRef);
            String protName = row[columnMap.get("proteins")];
            peptideDO.setProteinName(protName.startsWith("sp|iRT") ? "iRT" : protName);
            peptideDO.setLibraryName(library.getName());
            peptideDO.setLibraryId(library.getId());
            peptideDO.setFullName(peptideRef.split("_")[0]);
            peptideDO.setSequence(row[columnMap.get("sequence")]);
            peptideDO.setTargetSequence(row[columnMap.get("sequence")]);
            peptideDO.setIsDecoy(false);
            peptideDO.setCharge(Integer.parseInt(row[columnMap.get("charge")]));
            peptideDO.setRt(Double.parseDouble(row[columnMap.get("retentiontime")]));
            parseModification(peptideDO);
            peptideDOMap.put(peptideRef, peptideDO);
        }
        return peptideDOMap;
    }

    private String replaceModification(String modifiedSequence){
        String replacedSequence = null;
        for (Unimod unimod: Unimod.values()){
            replacedSequence = modifiedSequence.replace(unimod.getMsmsAbbr(), unimod.getSerialName());
        }
        return replacedSequence;
    }

    private void setFragmentInfo(PeptideDO peptideDO, String[] ionArray, String[] intensityArray, String[] massArray){
        //
        peptideDO.setFragmentMap(new HashMap<>());
        for (int i=0; i<ionArray.length; i++){
            String cutInfo = ionArray[i];
            if (!cutInfo.startsWith("b") && !cutInfo.startsWith("y")){
                continue;
            }
            if (cutInfo.contains("-")){
                continue;
            }
            Double intensity = Double.parseDouble(intensityArray[i]);
            FragmentInfo fragmentInfo = new FragmentInfo(cutInfo, Double.parseDouble(massArray[i]), intensity, 1);
            ResultDO<Annotation> resultDO = parseAnnotation(cutInfo);
            fragmentInfo.setAnnotations(cutInfo);
            fragmentInfo.setAnnotation(resultDO.getModel());
            if (fragmentInfo.getAnnotation().getCharge() != 1){
                fragmentInfo.setCharge(fragmentInfo.getAnnotation().getCharge());
            }
            peptideDO.getFragmentMap().put(cutInfo, fragmentInfo);
        }
    }

    private void verifyUnimod(String[] ionArray, String[] massArray, HashMap<Integer, String> unimodMap, String sequence, int charge, Double mass){
        //check total mass
        double theoryMass = fragmentFactory.getTheoryMass(unimodMap, sequence);
        if (Math.abs(theoryMass - mass) < Constants.ELEMENT_TOLERANCE){
            return;
        }
        //get b,y map; default y2,y3,b2,b3
        for (int i = 0; i < ionArray.length; i++){
            if (ionArray[i].contains("-") || ionArray[i].contains("+")){
                continue;
            }
            if (ionArray[i].startsWith("b")){

            }
        }

        BYSeries bySeries = fragmentFactory.getBYSeries(unimodMap, sequence, charge);
        List<Double> bSeries = bySeries.getBSeries();
        List<Double> ySeries = bySeries.getYSeries();
        String[] bModInfoArray = new String[sequence.length()];
        String[] yModInfoArray = new String[sequence.length()];
        Double bCompensateMz = 0d, yCompensateMz = 0d;
        int lastBPosition = 0, lastYPosition = 0;
        HashMap<String, Double> posMzMap = new HashMap<>();
        for (int i = 0; i < ionArray.length; i++){
            String cutInfo = ionArray[i];
            if (cutInfo.contains("-") || cutInfo.contains("+")){
                continue;
            }
            double fragmentMz = Double.parseDouble(massArray[i]);
            int position = Integer.parseInt(ionArray[i].substring(1));
            if (cutInfo.startsWith("b")){
                double theoMz = bSeries.get(position - 1) + bCompensateMz;
                double mzDiff = fragmentMz - theoMz;
                //TODO: @Nico infer all kinds of unimods
                if (mzDiff > Constants.ELEMENT_TOLERANCE){
                    String roundModMz = Long.toString(Math.round(mzDiff));
                    if (position - lastBPosition == 1){
                        bModInfoArray[position - 1] = "1;" + roundModMz;
                    }else {
                        for (int pos = lastBPosition; pos < position; pos ++){
                            bModInfoArray[pos] = "2;" + lastBPosition + ";" + roundModMz;
                        }
                    }
                }
                lastBPosition = position;
                bCompensateMz += mzDiff;
                continue;
            }
            if (cutInfo.startsWith("y")){
                double theoMz = ySeries.get(position - 1) + yCompensateMz;
                double mzDiff = fragmentMz - theoMz;
                if (mzDiff > Constants.ELEMENT_TOLERANCE){
                    String roundModMz = Long.toString(Math.round(mzDiff));
                    if (position - lastYPosition == 1){
                        yModInfoArray[sequence.length() - position] = "1;" + roundModMz;
                    }else {
                        for (int pos = lastYPosition; pos < position; pos ++){
                            yModInfoArray[sequence.length() - lastYPosition - 1] = "2;" + lastYPosition + ";" + roundModMz;
                        }
                    }
                }
                lastYPosition = position;
                yCompensateMz += mzDiff;
            }
        }
        String[] modInfoArray = new String[sequence.length()];
        for (int i = 0; i < sequence.length(); i++){
            if (bModInfoArray[i] == null || yModInfoArray[i] == null){
                continue;
            }
            if (bModInfoArray[i].startsWith("1") && yModInfoArray[i].startsWith("1")){
                int roundModMz = Integer.parseInt(bModInfoArray[i].split(";")[1]);
                unimodIntepreter(i, roundModMz, unimodMap);
                continue;
            }
            if (bModInfoArray[i].startsWith("1") && yModInfoArray[i].startsWith("2")){
                int bRoundModMz = Integer.parseInt(bModInfoArray[i].split(";")[2]);
                int yRoundModMz = Integer.parseInt(yModInfoArray[i].split(";")[2]);
                unimodIntepreter(i, bRoundModMz, unimodMap);
                int newRoundModMz = 0;
                if (bRoundModMz != yRoundModMz){
                    newRoundModMz = yRoundModMz - bRoundModMz;
                }
                String groupIdentifier = yModInfoArray[i].split(";")[1];
                int groupIter = i + 1;
                while (groupIter < sequence.length() && yModInfoArray[groupIter].startsWith("2")){
                    String[] modInfo = yModInfoArray[groupIter].split(";");
                    if (!modInfo[1].equals(groupIdentifier)){
                        break;
                    }
                    if (newRoundModMz == 0){
                        yModInfoArray[groupIter] = null;
                    }else {
                        yModInfoArray[groupIter] = "2;" + groupIdentifier + ";" + newRoundModMz;
                    }
                    groupIter ++;
                }
            }
        }
    }

    private void unimodIntepreter(int index, int roundModMz, HashMap<Integer, String> unimodMap){
        if (roundModMz == 57){
            unimodMap.put(index, "4");
        }else {
            logger.info("Modification is not UniMod:4");
        }
    }

}
