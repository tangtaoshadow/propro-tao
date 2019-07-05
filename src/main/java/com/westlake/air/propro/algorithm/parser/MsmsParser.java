package com.westlake.air.propro.algorithm.parser;

import com.westlake.air.propro.algorithm.formula.FragmentFactory;
import com.westlake.air.propro.algorithm.parser.model.chemistry.AminoAcid;
import com.westlake.air.propro.constants.Constants;
import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.constants.Unimod;
import com.westlake.air.propro.dao.AminoAcidDAO;
import com.westlake.air.propro.dao.UnimodDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.peptide.Annotation;
import com.westlake.air.propro.domain.bean.score.BYSeries;
import com.westlake.air.propro.domain.db.FragmentInfo;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.PeptideDO;
import com.westlake.air.propro.domain.db.TaskDO;
import com.westlake.air.propro.service.LibraryService;
import com.westlake.air.propro.service.TaskService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import scala.Int;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.westlake.air.propro.utils.PeptideUtil.parseModification;

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
                        libPepMap.get(peptideDO.getPeptideRef()).setProteinName("iRT");
                        continue;
                    }
//                    if (prmPeptideRefSet.contains(peptideDO.getPeptideRef())){
//                        prmPeptideRefSet.remove(peptideDO.getPeptideRef());
//                    }
                    peptideDO.setProteinName("iRT");
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
            String protName = row[columnMap.get("proteins")];
            peptideDO.setProteinName(protName.startsWith("sp|iRT") ? "iRT" : protName);
            peptideDO.setLibraryName(library.getName());
            peptideDO.setLibraryId(library.getId());
            peptideDO.setSequence(row[columnMap.get("sequence")]);
            peptideDO.setTargetSequence(row[columnMap.get("sequence")]);
            peptideDO.setIsDecoy(false);
            peptideDO.setCharge(Integer.parseInt(row[columnMap.get("charge")]));
            peptideDO.setRt(Double.parseDouble(row[columnMap.get("retentiontime")]));
            parseModification(peptideDO);
            verifyUnimod(ionArray, massArray, peptideDO.getUnimodMap(), peptideDO.getSequence(), Double.parseDouble(row[columnMap.get("mass")]));
            peptideDO.setPeptideRef(getPeptideRef(peptideDO.getSequence(), peptideDO.getUnimodMap()));
            peptideDO.setFullName(peptideDO.getPeptideRef().split("_")[0]);
            peptideDOMap.put(peptideDO.getPeptideRef(), peptideDO);
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

    private String getPeptideRef(String sequence, HashMap<Integer, String> unimodMap){
        int length = sequence.length();
        int offset = 0;
        for (int i=0; i<length; i++){
            if (unimodMap.get(i) != null){
                sequence = sequence.substring(0, i + offset + 1) + "(UniMod:" + unimodMap.get(i) + ")" + sequence.substring(i + offset + 1);
                offset = unimodMap.get(i).length() + 9;
            }
        }
        return sequence;
    }

    public boolean verifyUnimod(String[] ionArray, String[] massArray, HashMap<Integer, String> unimodMap, String sequence, Double mass){
        //check total mass
        double massDiff = mass - fragmentFactory.getTheoryMass(unimodMap, sequence);
        if (massDiff < Constants.ELEMENT_TOLERANCE){
            return true;
        }

        BYSeries bySeries = fragmentFactory.getBYSeries(unimodMap, sequence, 1);
        List<Double> bSeries = bySeries.getBSeries();
        List<Double> ySeries = bySeries.getYSeries();
        String[] bModInfoArray = new String[sequence.length()];
        String[] yModInfoArray = new String[sequence.length()];
        Double bCompensateMz = 0d, yCompensateMz = 0d;
        int lastBPosition = 0, lastYPosition = 0;
        int bMax = 0, yMax = 0;
        //get b,y map; default y2,y3,b2,b3
        for (int i = 0; i < ionArray.length; i++){
            String cutInfo = ionArray[i];
            if (cutInfo.contains("-") || cutInfo.contains("+")){
                continue;
            }
            double fragmentMz = Double.parseDouble(massArray[i]);
            int position = Integer.parseInt(ionArray[i].substring(1));
            if (cutInfo.startsWith("b")){
                if (position > bMax){
                    bMax = position;
                }
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
                if (position > yMax){
                    yMax = position;
                }
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
        //deal with unknown part
        int unknownBMz = (int) Math.round(massDiff - bCompensateMz);
        int unknownYMz = (int) Math.round(massDiff - yCompensateMz);
        if (bMax < sequence.length() && unknownBMz > 0){
            for (int i = bMax; i < sequence.length(); i++){
                bModInfoArray[i] = "2;" + bMax + ";" + unknownBMz;
            }
        }
        if (yMax < sequence.length() && unknownYMz > 0){
            for (int i = yMax; i < sequence.length(); i++){
                yModInfoArray[sequence.length() - i - 1] = "2;" + yMax + ";" + unknownYMz;
            }
        }

        HashMap<String, Integer> positionMzDiffMap = new HashMap<>();
        boolean isSuccess = true;
        for (int i = 0; i < sequence.length(); i++){
            if (bModInfoArray[i] == null || yModInfoArray[i] == null){
                continue;
            }
            if (bModInfoArray[i].startsWith("1") && yModInfoArray[i].startsWith("1")){
                int roundModMz = Integer.parseInt(bModInfoArray[i].split(";")[1]);
                boolean success = certainIntepreter(i, roundModMz, unimodMap);
                if (!success){
                    isSuccess = false;
                }
                continue;
            }
            if (bModInfoArray[i].startsWith("1") && yModInfoArray[i].startsWith("2")){
                boolean success = semicertainIntepreter(bModInfoArray, yModInfoArray, unimodMap, i);
                if (!success){
                    isSuccess = false;
                }
                continue;
            }
            if (bModInfoArray[i].startsWith("2") && yModInfoArray[i].startsWith("1")){
                boolean success = semicertainIntepreter(yModInfoArray, bModInfoArray, unimodMap, i);
                if (!success){
                    isSuccess = false;
                }
                continue;
            }
            if (bModInfoArray[i].startsWith("2") && yModInfoArray[i].startsWith("2")){
                int bRoundModMz = Integer.parseInt(bModInfoArray[i].split(";")[2]);
                int yRoundModMz = Integer.parseInt(yModInfoArray[i].split(";")[2]);
                if (bRoundModMz == yRoundModMz){
                    int groupIter = i + 1;
                    String bInfo = bModInfoArray[i].split(";")[1];
                    String yInfo = yModInfoArray[i].split(";")[1];
                    while (groupIter < bModInfoArray.length
                            && bModInfoArray[groupIter] != null && bModInfoArray[groupIter].split(";")[1].equals(bInfo)
                            && yModInfoArray[groupIter] != null && yModInfoArray[groupIter].split(";")[1].equals(yInfo)){
                        groupIter ++;
                    }
                    positionMzDiffMap.put(i + ";" + (groupIter-1), bRoundModMz);
                    i = groupIter - 1;
                } else if(bRoundModMz > yRoundModMz){
                    i = findModPosition(bModInfoArray, yModInfoArray, bRoundModMz, yRoundModMz, positionMzDiffMap, i);
                } else {
                    i = findModPosition(yModInfoArray, bModInfoArray, yRoundModMz, bRoundModMz, positionMzDiffMap, i);
                }
            }
        }
        boolean success = analysePosMzDiffMap(positionMzDiffMap, sequence, unimodMap);
        if (!success){
            isSuccess = false;
        }
        return isSuccess;
    }

    private boolean certainIntepreter(int index, int roundModMz, HashMap<Integer, String> unimodMap){
        if (roundModMz == 57){
            unimodMap.put(index, "4");
            return true;
        }else {
            logger.info("Modification is not UniMod:4");
            return false;
        }
    }

    private boolean semicertainIntepreter(String[] certainList, String[] uncertainList, HashMap<Integer, String> unimodMap, int i){
        int cRoundModMz = Integer.parseInt(certainList[i].split(";")[1]);
        int uncRoundModMz = Integer.parseInt(uncertainList[i].split(";")[2]);
        int newRoundModMz = 0;
        if (cRoundModMz != uncRoundModMz){
            newRoundModMz = uncRoundModMz - cRoundModMz;
        }
        String groupIdentifier = uncertainList[i].split(";")[1];
        int groupIter = i + 1;
        while (groupIter < certainList.length && uncertainList[groupIter] != null &&  uncertainList[groupIter].startsWith("2")){
            String[] modInfo = uncertainList[groupIter].split(";");
            if (!modInfo[1].equals(groupIdentifier)){
                break;
            }
            if (newRoundModMz == 0){
                uncertainList[groupIter] = null;
            }else {
                uncertainList[groupIter] = "2;" + groupIdentifier + ";" + newRoundModMz;
            }
            groupIter ++;
        }
        return certainIntepreter(i, cRoundModMz, unimodMap);
    }

    private boolean uncertainIntepreter(int start, int end, int roundMzDiff, String sequence, HashMap<Integer, String> unimodMap){
        //count C
        int count = 0;
        char[] charList = sequence.toCharArray();
        for (int i = start; i <= end; i++){
            if (charList[i] == 'C'){
                count ++;
            }
        }
        if (roundMzDiff/57 == count){
            for (int i = start; i <= end; i++){
                if (charList[i] == 'C'){
                    unimodMap.put(i, "4");
                }
            }
            return true;
        }else {
            logger.info("Multi Modification is not UniMod:4");
            return false;
        }
    }

    /**
     *
     * @param bigInfoArray      String[] bigInfoArray = new String[]{"2;1;114","2;1;114","2;1;114","2;1;114"};
     * @param smallInfoArray    String[] smallInfoArray = new String[]{"2;1;57","2;1;57",null,null};
     * @param bigRoundMz        114
     * @param smallRoundMz      57
     * @param positionMzDiffMap
     * @param i                 begin index of info arrays
     * @return                  new start position for info arrays
     */
    private int findModPosition(String[] bigInfoArray, String[] smallInfoArray, int bigRoundMz, int smallRoundMz, HashMap<String, Integer> positionMzDiffMap, int i){
        int groupIter = i;
        int rightBoundary = i;
        String bigInfo = bigInfoArray[i].split(";")[1];
        String smallInfo = smallInfoArray[i].split(";")[1];
        while (groupIter < bigInfoArray.length && bigInfoArray[groupIter] != null && bigInfoArray[groupIter].split(";")[1].equals(bigInfo)){
            bigInfoArray[groupIter] = "2;" + bigInfo + ";" + (bigRoundMz - smallRoundMz);
            if (smallInfoArray[groupIter] != null && smallInfoArray[groupIter].split(";")[1].equals(smallInfo)){
                rightBoundary = groupIter;
            }
            groupIter ++;
        }
        positionMzDiffMap.put(i + ";" + rightBoundary, smallRoundMz);
        return rightBoundary;
    }

    private boolean analysePosMzDiffMap(HashMap<String, Integer> positionMzDiffMap, String sequence, HashMap<Integer, String> unimodMap){
        boolean isSuccess = true;
        for (String key: positionMzDiffMap.keySet()){
            String[] startEnd = key.split(";");
            int startPosition = Integer.parseInt(startEnd[0]);
            int endPosition = Integer.parseInt(startEnd[1]);
            boolean success = analysePosMzDiff(startPosition, endPosition, positionMzDiffMap.get(key), sequence, unimodMap);
            if (!success){
                isSuccess = false;
            }
        }
        return isSuccess;
    }

    private boolean analysePosMzDiff(int start, int end, int roundMzDiff, String sequence, HashMap<Integer, String> unimodMap){
        if (start == end){
            return certainIntepreter(start, roundMzDiff, unimodMap);
        }else {
            return uncertainIntepreter(start, end, roundMzDiff, sequence, unimodMap);
        }
    }

//    private Integer[] getCutIndex(String[] ionArray, int seqLen){
//
//        HashSet<Integer> cutIndexSet = new HashSet<>();
//        for (int i = 0; i < ionArray.length; i++) {
//            String cutInfo = ionArray[i];
//            if (cutInfo.contains("-") || cutInfo.contains("+")) {
//                continue;
//            }
//            if (cutInfo.startsWith("b")){
//                cutIndexSet.add(Integer.parseInt(ionArray[i].substring(1)) - 1);
//            }
//            if (cutInfo.startsWith("y")){
//                cutIndexSet.add(seqLen - Integer.parseInt(ionArray[i].substring(1)) - 1);
//            }
//        }
//        cutIndexSet.add(seqLen - 1);
//        Integer[] cutIndexArray = cutIndexSet.toArray(new Integer[0]);
//        Arrays.sort(cutIndexArray);
//        return cutIndexArray;
//    }
//
//    private double[] getTheoCutMz(Integer[] cutIndexArray, String sequence, HashMap<Integer, String> unimodMap){
//        double[] theoMzArray = new double[cutIndexArray.length];
//        double[] aaMzArray = fragmentFactory.getAaMzArray(unimodMap, sequence);
//        int startIndex = 0;
//        for (int i = 0; i < cutIndexArray.length; i++){
//            double fragMz = 0d;
//            for (int j = startIndex; j <= cutIndexArray[i]; j++){
//                fragMz += aaMzArray[j];
//            }
//            theoMzArray[i] = fragMz;
//            startIndex = cutIndexArray[i] + 1;
//        }
//        return theoMzArray;
//    }

//    private void locateUnimod(Integer[] cutIndexArray, double[] theoMzArray, String[] ionArray, String[] massArray){
//
//        List<Integer> bIndexArray = new ArrayList<>();
//        List<Integer> yIndexArray = new ArrayList<>();
//        for (int i = 0; i < ionArray.length; i++){
//            String cutInfo = ionArray[i];
//            if (cutInfo.contains("-") || cutInfo.contains("+")) {
//                continue;
//            }
//            if (ionArray[i].startsWith("b")){
//                bIndexArray.add(i);
//            }
//            if (ionArray[i].startsWith("y")){
//                yIndexArray.add(i);
//            }
//        }
//        int lastBNum = 0, startCutIndex = 0, endCutIndex = 0;
//        double lastBMass = 0d;
//        for (int index: bIndexArray){
//            int bNum = Integer.parseInt(ionArray[index].substring(1));
//            double mass = Double.parseDouble(massArray[index]) - Constants.B_SIDE_MASS - lastBMass;
//            double massAcc = 0d;
//            for (int cutIndex = startCutIndex; cutIndexArray[cutIndex] < bNum; cutIndex ++){
//                massAcc += theoMzArray[cutIndex];
//                endCutIndex = cutIndex;
//            }
//            lastBMass = mass;
//            int massRoundDiff = (int) Math.round(mass - massAcc);
//            for (int i = startCutIndex; i<= endCutIndex; i++){
//                cutMzDiff[i] = massRoundDiff
//            }
//            for (int num = lastBNum; num <= bNum; )
//        }
//        for (int index: yIndexArray){
//
//        }
//
//        for (int i = 0; i < expCutIndex.length; i++){
//
//        }
//    }




}
