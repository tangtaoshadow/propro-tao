package com.westlake.air.propro.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.westlake.air.propro.algorithm.Airus;
import com.westlake.air.propro.algorithm.FragmentFactory;
import com.westlake.air.propro.algorithm.Tric;
import com.westlake.air.propro.constants.PositionType;
import com.westlake.air.propro.constants.ScoreType;
import com.westlake.air.propro.constants.TaskTemplate;
import com.westlake.air.propro.dao.AnalyseDataDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.propro.domain.bean.compressor.AirdInfo;
import com.westlake.air.propro.domain.bean.score.FeatureScores;
import com.westlake.air.propro.domain.params.LumsParams;
import com.westlake.air.propro.domain.db.*;
import com.westlake.air.propro.parser.AirdFileParser;
import com.westlake.air.propro.parser.MzXMLParser;
import com.westlake.air.propro.service.*;
import com.westlake.air.propro.utils.CompressUtil;
import com.westlake.air.propro.utils.FileUtil;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.*;
import java.nio.ByteOrder;
import java.util.*;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-07 13:30
 */
@Controller
@RequestMapping("test")
public class TestController extends BaseController {

    @Autowired
    ExperimentService experimentService;
    @Autowired
    ScanIndexService scanIndexService;
    @Autowired
    TaskService taskService;
    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    AnalyseDataDAO analyseDataDAO;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    ScoreService scoreService;
    @Autowired
    Airus airus;
    @Autowired
    PeptideService peptideService;
    @Autowired
    FragmentFactory fragmentFactory;
    @Autowired
    AirdFileParser airdFileParser;
    @Autowired
    MzXMLParser mzXMLParser;
    @Autowired
    Tric tric;

    public static float MZ_EXTRACT_WINDOW = 0.05f;
    public static float RT_EXTRACT_WINDOW = 1200f;
    public static float SIGMA = 6.25f;
    public static float SPACING = 0.01f;

    @RequestMapping("test")
    @ResponseBody
    String test(Model model, RedirectAttributes redirectAttributes) {

        try {
            ResultDO<ExperimentDO> expResult = experimentService.getById("5c75f7d9fc6f9e20a85e961a");
            ExperimentDO exp = expResult.getModel();
            RandomAccessFile raf = new RandomAccessFile(new File(exp.getAirdPath()), "r");
            HashMap<Float, ScanIndexDO> scanIndexMap = scanIndexService.getSwathIndexList("5c75f7d9fc6f9e20a85e961a");
            for (ScanIndexDO scanIndexDO : scanIndexMap.values()) {
                if (scanIndexDO.getPrecursorMzStart() == 400) {
                    try {
                        TreeMap map = airdFileParser.parseSwathBlockValues(raf, scanIndexDO, ByteOrder.LITTLE_ENDIAN);
                        System.out.println(scanIndexDO.getPrecursorMzStart() + ":" + map.size());
                    } catch (Exception e) {
                        logger.error("PrecursorMzStart: " + scanIndexDO.getPrecursorMzStart());
                        logger.error("Blocks: " + JSONArray.toJSONString(scanIndexDO.getBlocks()));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    //计算iRT
    @RequestMapping("test2")
    @ResponseBody
    String test2(Model model, RedirectAttributes redirectAttributes) {
        int[] mzArray = new int[21];
        mzArray[0] = 84960;
        mzArray[1] = 123015;
        mzArray[2] = 136919;
        mzArray[3] = 149015;
        mzArray[4] = 161057;
        mzArray[5] = 165694;
        mzArray[6] = 195918;
        mzArray[7] = 243131;
        mzArray[8] = 254097;
        mzArray[9] = 290156;
        mzArray[10] = 333144;
        mzArray[11] = 348147;
        mzArray[12] = 357035;
        mzArray[13] = 357232;
        mzArray[14] = 365206;
        mzArray[15] = 367164;
        mzArray[16] = 379980;
        mzArray[17] = 387205;
        mzArray[18] = 388134;
        mzArray[19] = 413090;
        mzArray[20] = 620326;
        int[] mzCompressedArray = CompressUtil.compressForSortedInt(mzArray);
        return "";

    }

    @RequestMapping("test6")
    @ResponseBody
    String test6(Model model, RedirectAttributes redirectAttributes) throws IOException {
        String jsonTest = "{\"start\": 482.14798,\"end\": 483.348,\"interval\": 1.20001221}";
        HashMap test = JSONObject.parseObject(jsonTest, HashMap.class);
        System.out.println(test.size());
        return null;
    }

    @RequestMapping("test8")
    @ResponseBody
    String test8(Model model, RedirectAttributes redirectAttributes) throws Exception {
        System.out.println("------ Aird Read Test ------");
        String filePath = "\\\\ProproNas\\ProproNAS\\data\\SGS\\aird\\napedro_L120420_010_SW.aird";
        String indexFilePath = "\\\\ProproNas\\ProproNAS\\data\\SGS\\aird\\napedro_L120420_010_SW.json";
        String jsonIndex = FileUtil.readFile(indexFilePath);
        AirdInfo airdInfo = JSONObject.parseObject(jsonIndex, AirdInfo.class);
        File jsonFile = new File(filePath);
        RandomAccessFile raf = new RandomAccessFile(jsonFile, "r");
        long start = System.currentTimeMillis();
        int i = 1;
        System.out.println("napedro_L120224_010_SW.aird");
        for (ScanIndexDO index : airdInfo.getSwathIndexList()) {
            TreeMap<Float, MzIntensityPairs> result = airdFileParser.parseSwathBlockValues(raf, index, ByteOrder.LITTLE_ENDIAN);
            System.out.println("第" + i + "批数据,读取耗时:" + (System.currentTimeMillis() - start));
            start = System.currentTimeMillis();
            i++;
        }

        FileUtil.close(raf);
        return null;
    }

    @RequestMapping("test9")
    @ResponseBody
    String test9(Model model, RedirectAttributes redirectAttributes) throws IOException {

        File file = new File("\\\\ProproNas\\ProproNAS\\data\\SGS\\mzxml\\napedro_L120224_010_SW.mzxml");
        List<ScanIndexDO> scanIndexes = mzXMLParser.index(file, 1);
        HashMap<Float, List<ScanIndexDO>> swathMap = new HashMap<>();
        for (ScanIndexDO index : scanIndexes) {
            if (index.getMsLevel().equals(2)) {
                List<ScanIndexDO> indexes = swathMap.get(index.getPrecursorMzStart());
                if (indexes == null) {
                    indexes = new ArrayList<>();
                    swathMap.put(index.getPrecursorMzStart(), indexes);
                }
                indexes.add(index);
            }
        }
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");

            int i = 1;
            for (List<ScanIndexDO> indexes : swathMap.values()) {
                long start = System.currentTimeMillis();
                for (ScanIndexDO scanIndex : indexes) {
                    String value = mzXMLParser.parseValue(raf, scanIndex.getPosStart(PositionType.MZXML), scanIndex.getPosEnd(PositionType.MZXML));
                    Float[] values = mzXMLParser.getValues(new Base64().decode(value), 32, true, ByteOrder.BIG_ENDIAN);
                    List<Float> mzList = new ArrayList<>();
                    List<Float> intensityList = new ArrayList<>();
                    for (int peakIndex = 0; peakIndex < values.length - 1; peakIndex += 2) {
                        Float mz = values[peakIndex];
                        Float intensity = values[peakIndex + 1];
                        mzList.add(mz);
                        intensityList.add(intensity);
                    }
                }
                logger.info("第" + i + "批,耗时:" + (System.currentTimeMillis() - start));
                i++;
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        } finally {
            FileUtil.close(raf);
        }

        return null;
    }

    @RequestMapping("scoreTest")
    @ResponseBody
    String scoreTest(){
        List<String> experimentIdList = new ArrayList<>();
        experimentIdList.add("5c7e6d13dfdfdd2e3430a110");
        experimentIdList.add("5c7e6d13dfdfdd2e3430a112");
        experimentIdList.add("5c7e6d13dfdfdd2e3430a114");
        experimentIdList.add("5c7e6d13dfdfdd2e3430a116");
        experimentIdList.add("5c7e6d13dfdfdd2e3430a118");
        experimentIdList.add("5c7e6d13dfdfdd2e3430a11a");
        experimentIdList.add("5c7e6d13dfdfdd2e3430a11c");
        experimentIdList.add("5c7e6d13dfdfdd2e3430a11e");
        List<Double> targetScoreList = new ArrayList<>();
        for (String expId: experimentIdList) {
            String overviewId = analyseOverviewService.getAllByExpId(expId).get(0).getId();
            List<AnalyseDataDO> analyseDataDOList = analyseDataService.getAllByOverviewId(overviewId);
            for (AnalyseDataDO analyseDataDO: analyseDataDOList){
                if (analyseDataDO.getIdentifiedStatus() != AnalyseDataDO.IDENTIFIED_STATUS_SUCCESS){
                    continue;
                }
                List<FeatureScores> featureScoresList = analyseDataDO.getFeatureScoresList();
                for (FeatureScores featureScores: featureScoresList){
                    if (featureScores.getRt().equals(analyseDataDO.getBestRt())){
                        double score = featureScores.getScoresMap().get(ScoreType.XcorrShapeWeighted.getTypeName());
                        if (score < 0.7 && score > 0.6){
                            System.out.println(expId + " + " + analyseDataDO.getPeptideRef() + " + " + score);
                        }
                        targetScoreList.add(score);
                    }
                }
            }
        }
//        List<String> badList = new ArrayList<>();
//        for (String peptideRef: targetScoreMap.keySet()){
//            if (targetScoreMap.get(peptideRef) < 0.6){
//                badList.add(peptideRef);
//            }
//        }

        System.out.println(targetScoreList);
        return null;
    }

    @RequestMapping("hyeTest")
    @ResponseBody
    String hyeTest() throws IOException {
        List<String> analyseOverviewIdList = new ArrayList<>();
//        List<ExperimentDO> experimentDOList = experimentService.getAllByProjectName("HYE110_TTOF6600_64var");
//        for (ExperimentDO experimentDO : experimentDOList) {
//            analyseOverviewIdList.add(analyseOverviewService.getAllByExpId(experimentDO.getId()).get(0).getId());
//        }
//        analyseOverviewIdList.add("5c9c3280dfdfdd7078dabb1d");//006
//        analyseOverviewIdList.add("5c9c393ddfdfdd7078dd8507");//004
//        analyseOverviewIdList.add("5c9c3f0cdfdfdd7078e04f2a");//002
//        analyseOverviewIdList.add("5c9c3658dfdfdd7078dc2050");//005
//        analyseOverviewIdList.add("5c9c3c27dfdfdd7078deea5d");//003
//        analyseOverviewIdList.add("5c9c41fadfdfdd7078e1b4ad");//001
        //32var-B
//        analyseOverviewIdList.add("5ca07216dfdfdd6c5806fd83");//006
//        analyseOverviewIdList.add("5ca0781ddfdfdd6c5809b3a5");//004
//        analyseOverviewIdList.add("5ca07e2fdfdfdd6c580c6a17");//002
        //32var-A
        analyseOverviewIdList.add("5ca0751edfdfdd6c5808599b");//005
        analyseOverviewIdList.add("5ca07b2cdfdfdd6c580b0fdc");//003
        analyseOverviewIdList.add("5ca08169dfdfdd6c580dc75a");//001

        HashMap<String, Integer> proproPepRef = tric.parameterEstimation(analyseOverviewIdList, 0.01d);


        // HYE110_TTOF6600_64var    C:/E1603141345_feature_alignment.tsv
        //       file: 38567 + 1224
        //     propro: 41419 + 5093
        //      match: 37470 + 264
        //propro only: 3949 + 4829
        //  file only: 1097 + 960
        //match file target:   [1862,3597,10959,3295,4558,13199]
        //match propro target: [825,1448,9670,3137,2590,19800]
        //match file decoy:    [105,49,60,25,11,14]
        //match propro decoy:  [140,60,32,11,10,11]
        //only file target:    [262,240,354,71,68,102]
        //only file decoy:     [474,239,123,53,49,22]
        //only propro target:  [1386,970,1127,198,92,176]
        //only propro decoy:   [3468,792,317,119,80,53]

//         HYE110_TTOF6600_32var    C:/E1603141355_feature_alignment.tsv

        // HYE110_TTOF6600_64fix    C:/E1603141357_feature_alignment.tsv
        //       file: 33700 + 843
        //     propro: 37683 + 3453
        //      match: 32252 + 164
        //propro only: 5431 + 3289
        //  file only: 1448 + 679
        //match file target:   [1835,2784,10252,2371,2379,12631]
        //match propro target: [1157,2011,8957,2683,2983,14461]
        //match file decoy:    [76,29,21,15,13,10]
        //match propro decoy:  [82,34,18,11,8,11]
        //only file target:    [398,284,450,66,68,182]
        //only file decoy:     [350,122,115,43,28,21]
        //only propro target:  [2212,1389,1313,233,133,151]
        //only propro decoy:   [2411,480,213,95,45,45]

        // HYE110_TTOF6600_32fix    C:/E1603141346_feature_alignment.tsv
        //       file: 32053 + 1184
        //     propro: 34425 + 1298
        //      match: 29952 + 81
        //propro only: 4473 + 1217
        //  file only: 2101 + 1103
        //match file target:   [1652,2859,9401,2417,2343,11280]
        //match propro target: [1517,3866,7271,2361,7493,7444]
        //match file decoy:    [36,14,17,3,6,5]
        //match propro decoy:  [16,3,18,8,9,27]
        //only file target:    [655,448,559,112,70,257]
        //only file decoy:     [514,247,190,66,50,36]
        //only propro target:  [1968,1239,890,175,126,75]
        //only propro decoy:   [333,185,185,152,162,200]
        BufferedReader reader = new BufferedReader(new FileReader("C:/E1603141355_feature_alignment.tsv"));
        reader.readLine();//第一行信息，为标题信息
        String line;
        HashMap<String, Integer> fileTargetMap = new HashMap<>();
        HashMap<String, Integer> fileDecoyMap = new HashMap<>();
        //32var 0_02 0_11 0_24 0_35 0_46 0_53
        //64var 0_05 0_14 0_22 0_31 0_43 0_56
        List<String> aList = new ArrayList<>();
//        evenList.add("0_1");
//        evenList.add("0_2");
//        evenList.add("0_5");
        aList.add("0_0");
        aList.add("0_2");
        aList.add("0_4");
        while((line=reader.readLine())!=null) {
            if (aList.contains(line.split("\t")[2])){
                continue;
            }
            String item[] = line.split("\t")[0].split("_");
            if (item[0].equals("DECOY")){
                String peptideRef = item[0] + "_" + item[2] +"_" + item[3];
                if (fileDecoyMap.containsKey(peptideRef)){
                    fileDecoyMap.put(peptideRef, fileDecoyMap.get(peptideRef) + 1);
                }else {
                    fileDecoyMap.put(peptideRef, 1);
                }
            }else {
                String peptideRef = item[1] + "_" + item[2];
                if (fileTargetMap.containsKey(peptideRef)){
                    fileTargetMap.put(peptideRef, fileTargetMap.get(peptideRef) + 1);
                }else {
                    fileTargetMap.put(peptideRef, 1);
                }
            }
        }

        HashMap<String, Integer> proproMatchTarget = new HashMap<>();
        HashMap<String, Integer> proproMatchDecoy = new HashMap<>();
        HashMap<String, Integer> proproOnlyTarget = new HashMap<>();
        HashMap<String, Integer> proproOnlyDecoy = new HashMap<>();
        HashMap<String, Integer> fileMatchTarget = new HashMap<>();
        HashMap<String, Integer> fileMatchDecoy = new HashMap<>();
        HashMap<String, Integer> fileOnlyTarget = new HashMap<>();
        HashMap<String, Integer> fileOnlyDecoy = new HashMap<>();

        for (String peptideRef: proproPepRef.keySet()){
            if (peptideRef.startsWith("DECOY_")){
                if (fileDecoyMap.containsKey(peptideRef)){
                    proproMatchDecoy.put(peptideRef, proproPepRef.get(peptideRef));
                    fileMatchDecoy.put(peptideRef, fileDecoyMap.get(peptideRef));
                }else {
                    proproOnlyDecoy.put(peptideRef, proproPepRef.get(peptideRef));
                }
            }else {
                if (fileTargetMap.containsKey(peptideRef)){
                    proproMatchTarget.put(peptideRef, proproPepRef.get(peptideRef));
                    fileMatchTarget.put(peptideRef, fileTargetMap.get(peptideRef));
                }else {
                    proproOnlyTarget.put(peptideRef, proproPepRef.get(peptideRef));
                }
            }
        }
        for (String peptideRef: fileTargetMap.keySet()){
            if (!fileMatchTarget.containsKey(peptideRef)){
                fileOnlyTarget.put(peptideRef, fileTargetMap.get(peptideRef));
            }
        }
        for (String peptideRef: fileDecoyMap.keySet()){
            if (!fileMatchDecoy.containsKey(peptideRef)){
                fileOnlyDecoy.put(peptideRef, fileDecoyMap.get(peptideRef));
            }
        }

        int dimension = 3;
        System.out.println("file: " + fileTargetMap.size() + " + " + fileDecoyMap.size());
        System.out.println("propro: " + (proproMatchTarget.size() + proproOnlyTarget.size()) + " + " + (proproMatchDecoy.size() + proproOnlyDecoy.size()));
        System.out.println("match: " + proproMatchTarget.size() + " + " + proproMatchDecoy.size());
        System.out.println("propro only: " + proproOnlyTarget.size() + " + " + proproOnlyDecoy.size());
        System.out.println("file only: " + fileOnlyTarget.size() + " + " + fileOnlyDecoy.size());
        System.out.println("match file target:   " + JSON.toJSON(getHitMap(fileMatchTarget, dimension)));
        System.out.println("match propro target: " + JSON.toJSON(getHitMap(proproMatchTarget, dimension)));
        System.out.println("match file decoy:    " + JSON.toJSON(getHitMap(fileMatchDecoy, dimension)));
        System.out.println("match propro decoy:  " + JSON.toJSON(getHitMap(proproMatchDecoy, dimension)));
        System.out.println("only file target:    " + JSON.toJSON(getHitMap(fileOnlyTarget, dimension)));
        System.out.println("only file decoy:     " + JSON.toJSON(getHitMap(fileOnlyDecoy, dimension)));
        System.out.println("only propro target:  " + JSON.toJSON(getHitMap(proproOnlyTarget, dimension)));
        System.out.println("only propro decoy:   " + JSON.toJSON(getHitMap(proproOnlyDecoy, dimension)));
//        printScoreDistribution(analyseOverviewIdList, proproMatchTarget, ScoreType.XcorrShape.getTypeName(), 0);
//        printScoreDistribution(analyseOverviewIdList, proproMatchTarget, ScoreType.XcorrShape.getTypeName(), 0);
        return null;
    }

    private int[] getHitMap(HashMap<String, Integer> countMap, int dimension){
        int[] hitMap = new int[dimension];
//        List<Integer> errorCountList = new ArrayList<>();
        for (String peptideRef: countMap.keySet()){
            int count = countMap.get(peptideRef);
//            if (count > 6){
//                errorCountList.add(count);
//            }
            int i=2;
            while (count > dimension){
                count = count / i;
                i++;
            }
            hitMap[count - 1] ++;
        }
//        System.out.println("error List: " + errorCountList.size() + errorCountList);
        return hitMap;
    }

    private void printScoreDistribution(List<String> analyseOverviewIdList, HashMap<String, Integer> peptideRefMap, String scoreType, int cutOff) throws IOException {
        List<Double> decoyScoreList = new ArrayList<>();
        List<Double> matchTargetScoreList = new ArrayList<>();
        for (String analyseOverviewId: analyseOverviewIdList){
            List<AnalyseDataDO> dataDOList = analyseDataService.getAllByOverviewId(analyseOverviewId);
            for (AnalyseDataDO dataDO: dataDOList){
                if (dataDO.getIsDecoy()){
                    List<FeatureScores> featureScoresList = dataDO.getFeatureScoresList();
                    double bestRt = dataDO.getBestRt();
                    for (FeatureScores featureScores: featureScoresList){
                        if (featureScores.getRt() == bestRt){
                            decoyScoreList.add(featureScores.getScoresMap().get(scoreType));
                        }
                    }
                }else {
                    if (dataDO.getIdentifiedStatus() == AnalyseDataDO.IDENTIFIED_STATUS_SUCCESS && peptideRefMap.containsKey(dataDO.getPeptideRef()) && peptideRefMap.get(dataDO.getPeptideRef()) > cutOff){
                        List<FeatureScores> featureScoresList = dataDO.getFeatureScoresList();
                        double bestRt = dataDO.getBestRt();
                        for (FeatureScores featureScores: featureScoresList){
                            if (featureScores.getRt() == bestRt){
                                matchTargetScoreList.add(featureScores.getScoresMap().get(scoreType));
                            }
                        }
                    }
                }
            }
        }
        File file = new File("D:/test.tsv");
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
        writer.append("decoy");
        for (Double value: decoyScoreList){
            writer.append("\t").append(value.toString());
        }
        writer.append("\r").append("target");
        for (Double value: matchTargetScoreList){
            writer.append("\t").append(value.toString());
        }
        writer.append("\r");
        writer.close();

//        System.out.println("decoy: " + decoyScoreList);
//        System.out.println("matchT:" + matchTargetScoreList);
    }

}