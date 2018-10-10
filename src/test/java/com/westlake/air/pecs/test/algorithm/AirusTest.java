package com.westlake.air.pecs.test.algorithm;

import com.alibaba.fastjson.JSON;
import com.westlake.air.pecs.algorithm.Airus;
import com.westlake.air.pecs.domain.bean.airus.FinalResult;
import com.westlake.air.pecs.domain.bean.airus.ScoreData;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.pecs.domain.db.ScoresDO;
import com.westlake.air.pecs.parser.ScoreTsvParser;
import com.westlake.air.pecs.service.ScoresService;
import com.westlake.air.pecs.test.BaseTest;
import com.westlake.air.pecs.utils.AirusUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-12 21:18
 */
public class AirusTest extends BaseTest {


    @Autowired
    Airus airus;
    @Autowired
    ScoreTsvParser scoreTsvParser;
    @Autowired
    ScoresService scoresService;

    @Test
    public void scoreFromFileWork() {
        ScoreData scoreData = scoreTsvParser.getScoreData(new File(this.getClass().getClassLoader().getResource("SGSScoreResultUni.csv").getPath()), ScoreTsvParser.SPLIT_CHANGE_LINE);
        FinalResult finalResult = airus.doAirus(scoreData);

        int count = AirusUtils.checkFdr(finalResult);
        System.out.println(count);

        assert count >= 322;
    }

    @Test
    public void scoreFromDBWork() {
        HashMap<String, ScoresDO> scoreMap = scoresService.getAllMapByOverviewId("5bbe0031fc6f9e297cccf05d");
        ScoreData scoreData = airus.trans(new ArrayList(scoreMap.values()));
        FinalResult finalResult = airus.doAirus(scoreData);

        int count = AirusUtils.checkFdr(finalResult);
        System.out.println(count);

        assert count >= 322;
    }

    @Test
    public void isScoresSame() {
        HashMap<String, ScoresDO> scoresMapFromFile = scoreTsvParser.getScoreMap(new File(this.getClass().getClassLoader().getResource("SGSScoreResultUni.csv").getPath()), ScoreTsvParser.SPLIT_COMMA);
        //黄金数据集,Water-10
        HashMap<String, ScoresDO> scoresMapFromDB = scoresService.getAllMapByOverviewId("5bab4316fc6f9e34a888a3d5");

        assert scoresMapFromDB.size() == 690;
        assert scoresMapFromFile.size() == scoresMapFromDB.size();
        int sameScoresCount = 0;
        for (String key : scoresMapFromFile.keySet()) {
            ScoresDO fileScore = scoresMapFromFile.get(key);
            ScoresDO dbScore = scoresMapFromDB.get(key);
            if (fileScore.getIsDecoy() && fileScore.getFeatureScoresList().size() != dbScore.getFeatureScoresList().size()) {
                sameScoresCount++;
                logger.info(key + "/" + fileScore.getFeatureScoresList().size() + "/" + dbScore.getFeatureScoresList().size());
            } else {
//                System.out.println(key);
            }
        }
        logger.info(sameScoresCount + "/690");
    }

    @Test
    public void isGetScoreMapMethodSame() {
        ScoreData scoreData1 = scoreTsvParser.getScoreData(new File(this.getClass().getClassLoader().getResource("SGSScoreResultUni.csv").getPath()), ScoreTsvParser.SPLIT_COMMA);
        HashMap<String, ScoresDO> scoreMap = scoreTsvParser.getScoreMap(new File(this.getClass().getClassLoader().getResource("SGSScoreResultUni.csv").getPath()), ScoreTsvParser.SPLIT_COMMA);
        ScoreData scoreData2 = airus.trans(new ArrayList(scoreMap.values()));

        AirusUtils.fakeSortTgId(scoreData1);
        AirusUtils.fakeSortTgId(scoreData2);
        for (int i = 0; i < scoreData2.getGroupId().length; i++) {
            String fullPeptide = scoreData2.getGroupId()[i].replace("_2", "");
            boolean isHit = false;
            for (int j = 0; j < scoreData1.getGroupId().length; j++) {
                if (scoreData2.getIsDecoy()[i].equals(scoreData1.getIsDecoy()[j])
                        && scoreData1.getGroupId()[j].contains(fullPeptide)
                        && scoreData1.getScoreData()[j][0].equals(scoreData2.getScoreData()[i][0])
                ) {
                    isHit = true;
                }
            }
            if (!isHit) {
                System.out.println(fullPeptide);
            }
        }

    }

    @Test
    public void airusTest() {
        ScoreData scoreData = scoreTsvParser.getScoreData(new File(this.getClass().getClassLoader().getResource("scores_data.tsv").getPath()), ScoreTsvParser.SPLIT_CHANGE_LINE);
        if (scoreData != null) {
            FinalResult finalResult = airus.doAirus(scoreData);
            System.out.println(JSON.toJSONString(finalResult));

            Double[] cutoff = finalResult.getSummaryErrorTable().getCutoff();
            Double[] pvalue = finalResult.getSummaryErrorTable().getPvalue();
            Double[] qvalue = finalResult.getSummaryErrorTable().getQvalue();
            double[] tp = finalResult.getSummaryErrorTable().getStatMetrics().getTp();
            double[] fp = finalResult.getSummaryErrorTable().getStatMetrics().getFp();
            double[] tn = finalResult.getSummaryErrorTable().getStatMetrics().getTn();
            double[] fn = finalResult.getSummaryErrorTable().getStatMetrics().getFn();
            double[] fpr = finalResult.getSummaryErrorTable().getStatMetrics().getFpr();
            double[] fdr = finalResult.getSummaryErrorTable().getStatMetrics().getFdr();
            double[] fnr = finalResult.getSummaryErrorTable().getStatMetrics().getFnr();
            double[] svalue = finalResult.getSummaryErrorTable().getStatMetrics().getSvalue();

            Double[] cutoffResult = {3.081365, 0.741210, 0.140612, -2.503086, -2.503086, -2.503086, -2.503086, -2.503086, -2.503086};
            Double[] pvalueResult = {0.002584, 0.191214, 0.410853, 0.994832, 0.994832, 0.994832, 0.994832, 0.994832, 0.994832};
            Double[] qvalueResult = {0.0, 0.01, 0.02, 0.05, 0.1, 0.2, 0.3, 0.4, 0.5};
            double[] tpResult = {332.951550, 368.414729, 366.296512, 368.346899, 368.346899, 368.346899, 368.346899, 368.346899, 368.346899};
            double[] fpResult = {0.048450, 3.585271, 7.703488, 18.653101, 18.653101, 18.653101, 18.653101, 18.653101, 18.653101};
            double[] tnResult = {18.701550, 15.164729, 11.046512, 0.096899, 0.096899, 0.096899, 0.096899, 0.096899, 0.096899};
            double[] fnResult = {35.298450, -0.164729, 1.953488, -0.096899, -0.096899, -0.096899, -0.096899, -0.096899, -0.096899};
            double[] fprResult = {0.002584, 0.191214, 0.410853, 0.994832, 0.994832, 0.994832, 0.994832, 0.994832, 0.994832};
            double[] fdrResult = {0.000145, 0.009638, 0.020598, 0.048199, 0.048199, 0.048199, 0.048199, 0.048199, 0.048199};
            double[] fnrResult = {0.653675, 0, 0.150268, 0, 0, 0, 0, 0, 0};
            double[] svalueResult = {0.904145, 1, 1, 1, 1, 1, 1, 1, 1};

            assert isSimilar(cutoff, cutoffResult, Math.pow(10, -5));
            assert isSimilar(pvalue, pvalueResult, Math.pow(10, -5));
            assert isSimilar(qvalue, qvalueResult, Math.pow(10, -5));
            assert isSimilar(tp, tpResult, Math.pow(10, -5));
            assert isSimilar(fp, fpResult, Math.pow(10, -5));
            assert isSimilar(tn, tnResult, Math.pow(10, -5));
            assert isSimilar(fn, fnResult, Math.pow(10, -5));
            assert isSimilar(fpr, fprResult, Math.pow(10, -5));
            assert isSimilar(fdr, fdrResult, Math.pow(10, -5));
            assert isSimilar(fnr, fnrResult, Math.pow(10, -5));
            assert isSimilar(svalue, svalueResult, Math.pow(10, -5));
        }
    }

    private boolean isSimilar(Double[] array1, Double[] array2, Double tolerance) {
        if (array1.length != array2.length) return false;
        boolean result = true;
        for (int i = 0; i < array1.length; i++) {
            if (Math.abs(array1[i] - array2[i]) > tolerance) {
                result = false;
            }
        }
        return result;
    }

    private boolean isSimilar(double[] array1, double[] array2, Double tolerance) {
        if (array1.length != array2.length) return false;
        boolean result = true;
        for (int i = 0; i < array1.length; i++) {
            if (Math.abs(array1[i] - array2[i]) > tolerance) {
                result = false;
            }
        }
        return result;
    }

    @Test
    public void test() {
        List<RtIntensityPairsDouble> list = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            RtIntensityPairsDouble rtIntensityPairsDouble = new RtIntensityPairsDouble();
            Double[] rt = {(double) i};
            rtIntensityPairsDouble.setRtArray(rt);
            rtIntensityPairsDouble.setIntensityArray(rt);
            list.add(rtIntensityPairsDouble);
        }
        System.out.println("list ready");
        RtIntensityPairsDouble rtInt = list.get(1);
        Double[] intensity = {0d};
        rtInt.setIntensityArray(intensity);
        System.out.println("what now");
    }
}
