package com.westlake.air.propro.algorithm.comparator;

import com.westlake.air.propro.domain.bean.file.TableFile;
import com.westlake.air.propro.domain.db.AnalyseDataDO;
import com.westlake.air.propro.domain.db.AnalyseOverviewDO;
import com.westlake.air.propro.domain.db.ExperimentDO;
import com.westlake.air.propro.domain.query.AnalyseDataQuery;
import com.westlake.air.propro.service.*;
import com.westlake.air.propro.utils.FileUtil;
import org.apache.shiro.crypto.hash.Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by Nico Wang
 * Time: 2019-05-31 15:36
 */
@Component("resultComparator")
public class ResultComparator {

    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    ExperimentService experimentService;
    //    @Autowired
//    ProjectService projectService;
    @Autowired
    PeptideService peptideService;

    private HashMap<String, HashSet<String>> getMatrixFileMap(String projectId, String filePath) {
        List<ExperimentDO> experimentDOList = experimentService.getAllByProjectId(projectId);
        HashMap<String, HashSet<String>> matPepSetMap = new HashMap<>();
        for (ExperimentDO experimentDO : experimentDOList) {
            matPepSetMap.put(experimentDO.getName(), new HashSet<>());
        }
        try {
            TableFile ppFile = FileUtil.readTableFile(filePath);
            HashMap<String, Integer> columnMap = ppFile.getColumnMap();
            List<String[]> fileData = ppFile.getFileData();
            for (String[] line : fileData) {
                for (String expName : matPepSetMap.keySet()) {
                    Integer index = columnMap.get(expName.toLowerCase() + "_with_dscore_filtered");
                    if (index != null && index < line.length && !line[index].isEmpty()) {
                        String[] pepInfo = line[0].split("_");
                        matPepSetMap.get(expName).add(pepInfo[1] + "_" + pepInfo[2]);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return matPepSetMap;
    }

    public void compareReplicate(String projectId, String filePath, String expNameA, String expNameB) {
        HashMap<String, HashSet<String>> matPepSetMap = getMatrixFileMap(projectId, filePath);
        HashSet<String> aFileSet = matPepSetMap.get(expNameA);
        HashSet<String> bFileSet = matPepSetMap.get(expNameB);
        System.out.println("FileA: " + aFileSet.size());
        System.out.println("FileB: " + bFileSet.size());
        printIntersectionCount(aFileSet, bFileSet);

        AnalyseOverviewDO analyseOverviewDO = analyseOverviewService.getFirstByExpId(experimentService.getByName(expNameA).getModel().getId()).getModel();
        HashSet<String> aProproSet = getProproPeptideRefs(analyseOverviewDO.getId());
        analyseOverviewDO = analyseOverviewService.getFirstByExpId(experimentService.getByName(expNameB).getModel().getId()).getModel();
        HashSet<String> bProproSet = getProproPeptideRefs(analyseOverviewDO.getId());
        System.out.println("ProproA: " + aProproSet.size());
        System.out.println("ProproB: " + bProproSet.size());
        printIntersectionCount(aProproSet, bProproSet);
        printIntersectionCount(aFileSet, aProproSet);
        printIntersectionCount(bFileSet, bProproSet);
    }


    public void silacResults(String analyseOverviewId, String filePath) {
        HashSet<String> filePepSet = getFilePeptideRefs(filePath);
        int fileHeavy = 0, fileLight = 0;
        for (String pepRef : filePepSet) {
            if (pepRef.split("_")[0].endsWith("(UniMod:188)")) {
                fileHeavy++;
            } else {
                fileLight++;
            }
        }
        System.out.println("File light peptide count: " + fileLight);
        System.out.println("File heavy peptide count: " + fileHeavy);

        HashSet<String> proproPepSet = getProproPeptideRefs(analyseOverviewId);
        int proproHeavy = 0, proproLight = 0;
        for (String pepRef : proproPepSet) {
            if (pepRef.split("_")[0].endsWith("(Unimod:188)")) {
                proproHeavy++;
            } else {
                proproLight++;
            }
        }
        System.out.println("Propro light peptide count: " + proproLight);
        System.out.println("Propro heavy peptide count: " + proproHeavy);
    }

    /**
     * 比较ProPro与pyprophet, 给出protein层面结果, 只考虑unique的protein
     *
     * @param analyseOverviewId
     * @param filePath
     */
    public void proteinResults(String analyseOverviewId, String filePath) {
        //Step 1: load pyprophet result
        HashSet<String> fileProtSet = getFileProteins(filePath, true);
        System.out.println("File unique protein count: " + fileProtSet.size());

        //Step 2: load propro result
        HashSet<String> proproProtSet = getProproProteins(analyseOverviewId, true);
        System.out.println("Propro unique protein count: " + proproProtSet.size());

        printIntersectionCount(fileProtSet, proproProtSet);
    }

    /**
     * 比较ProPro与pyprophet, peptideRef的结果
     *
     * @param analyseOverviewId
     * @param filePath
     */
    public void peptideRefResults(String analyseOverviewId, String filePath) {
        //Step 1: load pyprophet result
        HashSet<String> filePepSet = getFilePeptideRefs(filePath);
        System.out.println("File peptideRef count: " + filePepSet.size());

        //Step 2: load propro result
        HashSet<String> proproPepSet = getProproPeptideRefs(analyseOverviewId);
        System.out.println("Propro peptideRef count: " + proproPepSet.size());

        printIntersectionCount(filePepSet, proproPepSet);
    }

    /**
     * 比较ProPro与pyprophet, sequence的结果
     *
     * @param analyseOverviewId
     * @param filePath
     */
    public void peptideSeqResults(String analyseOverviewId, String filePath) {
        //Step 1: load pyprophet result
        HashSet<String> filePepSet = getFilePeptideSeqs(filePath);
        System.out.println("File sequence count: " + filePepSet.size());

        //Step 2: load propro result
        HashSet<String> proproPepSet = getProproPeptideSeqs(analyseOverviewId);
        System.out.println("Propro sequence count: " + proproPepSet.size());

        printIntersectionCount(filePepSet, proproPepSet);
    }

    public HashSet<String> getProproOnlyPepRef(String analyseOverviewId, String filePath) {
        HashSet<String> filePepSet = getFilePeptideSeqs(filePath);

        HashSet<String> proproPepSet = getProproPeptideSeqs(analyseOverviewId);

        HashSet<String> proproOnly = new HashSet<>();
        for (String pepRef : proproPepSet) {
            if (!filePepSet.contains(pepRef)) {
                proproOnly.add(pepRef);
            }
        }
        return proproOnly;
    }
    public HashSet<String> getFileOnlyPepRef(HashSet<String> filePepSet, HashSet<String> proproPepSet) {
        HashSet<String> fileOnly = new HashSet<>();
        for (String pepRef : filePepSet) {
            if (!proproPepSet.contains(pepRef)) {
                fileOnly.add(pepRef);
            }
        }
        return fileOnly;
    }

    public HashSet<String> getFileOnlyPepRef(String analyseOverviewId, String filePath) {
        HashSet<String> filePepSet = getFilePeptideSeqs(filePath);

        HashSet<String> proproPepSet = getProproPeptideSeqs(analyseOverviewId);

        HashSet<String> fileOnly = new HashSet<>();
        for (String pepRef : filePepSet) {
            if (!proproPepSet.contains(pepRef)) {
                fileOnly.add(pepRef);
            }
        }
        return fileOnly;
    }

    private HashSet<String> getFileProteins(String filePath, boolean isUnique) {
        HashSet<String> uniqueProt = new HashSet<>();
        try {
            TableFile ppFile = FileUtil.readTableFile(filePath);
            HashMap<String, Integer> columnMap = ppFile.getColumnMap();
            List<String[]> fileData = ppFile.getFileData();
            for (String[] lineSplit : fileData) {
                if (columnMap.containsKey("decoy") && lineSplit[columnMap.get("decoy")].equals("1")) {
                    continue;
                }
                if (columnMap.containsKey("m_score") && Double.parseDouble(lineSplit[columnMap.get("m_score")]) > 0.01d) {
                    continue;
                }
                String proteinName = lineSplit[columnMap.get("proteinname")];
                if (isUnique && !proteinName.startsWith("1/")) {
                    continue;
                }
                uniqueProt.add(proteinName);
            }
            return uniqueProt;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashSet<>();
        }
    }

    private HashSet<String> getFilePeptideRefs(String filePath) {
        HashSet<String> uniquePep = new HashSet<>();
        try {
            TableFile ppFile = FileUtil.readTableFile(filePath);
            HashMap<String, Integer> columnMap = ppFile.getColumnMap();
            List<String[]> fileData = ppFile.getFileData();
            for (String[] lineSplit : fileData) {
                if (columnMap.containsKey("decoy") && lineSplit[columnMap.get("decoy")].equals("1")) {
                    continue;
                }
                if (columnMap.containsKey("m_score") && Double.parseDouble(lineSplit[columnMap.get("m_score")]) > 0.01d) {
                    continue;
                }
                String[] peptideGroupInfo = lineSplit[columnMap.get("transition_group_id")].split("_");
                uniquePep.add(peptideGroupInfo[1] + "_" + peptideGroupInfo[2]);
            }
            return uniquePep;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashSet<>();
        }
    }

    private HashSet<String> getFilePeptideSeqs(String filePath) {
        HashSet<String> uniqueSeq = new HashSet<>();
        try {
            TableFile ppFile = FileUtil.readTableFile(filePath);
            HashMap<String, Integer> columnMap = ppFile.getColumnMap();
            List<String[]> fileData = ppFile.getFileData();
            for (String[] lineSplit : fileData) {
                if (columnMap.containsKey("decoy") && lineSplit[columnMap.get("decoy")].equals("1")) {
                    continue;
                }
                if (columnMap.containsKey("m_score") && Double.parseDouble(lineSplit[columnMap.get("m_score")]) > 0.01d) {
                    continue;
                }
                uniqueSeq.add(lineSplit[columnMap.get("sequence")]);
            }
            return uniqueSeq;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashSet<>();
        }
    }

    public HashSet<String> getProproProteins(String overviewId, boolean isUnique) {
        AnalyseDataQuery query = new AnalyseDataQuery();
        query.setOverviewId(overviewId);
        query.setIsDecoy(false);
        query.addIndentifiedStatus(AnalyseDataDO.IDENTIFIED_STATUS_SUCCESS);
        List<AnalyseDataDO> analyseDataDOList = analyseDataService.getAll(query);

        HashSet<String> uniqueProt = new HashSet<>();
        for (AnalyseDataDO dataDO : analyseDataDOList) {
            if (isUnique && (!dataDO.getIsUnique() || !dataDO.getProteinName().startsWith("1/"))) {
                continue;
            } else {
                uniqueProt.add(dataDO.getProteinName());
            }
        }
        return uniqueProt;
    }

    private HashSet<String> getProproPeptideRefs(String overviewId) {
        AnalyseDataQuery query = new AnalyseDataQuery();
        query.setOverviewId(overviewId);
        query.setIsDecoy(false);
        query.addIndentifiedStatus(AnalyseDataDO.IDENTIFIED_STATUS_SUCCESS);
        List<AnalyseDataDO> analyseDataDOList = analyseDataService.getAll(query);

        HashSet<String> uniquePep = new HashSet<>();
        for (AnalyseDataDO dataDO : analyseDataDOList) {
            uniquePep.add(dataDO.getPeptideRef());
        }
        return uniquePep;
    }

    private HashSet<String> getProproPeptideSeqs(String overviewId) {
        AnalyseDataQuery query = new AnalyseDataQuery();
        query.setOverviewId(overviewId);
        query.setIsDecoy(false);
        query.addIndentifiedStatus(AnalyseDataDO.IDENTIFIED_STATUS_SUCCESS);
        List<AnalyseDataDO> analyseDataDOList = analyseDataService.getAll(query);

        HashSet<String> uniqueSeq = new HashSet<>();
        for (AnalyseDataDO dataDO : analyseDataDOList) {
            uniqueSeq.add(peptideService.getById(dataDO.getPeptideId()).getModel().getSequence());
        }
        return uniqueSeq;
    }

    private void printIntersectionCount(HashSet<String> set1, HashSet<String> set2) {
        int count = 0;
        for (String word : set1) {
            if (set2.contains(word)) {
                count++;
            }
        }
        System.out.println("Intersection count: " + count);
    }
}
