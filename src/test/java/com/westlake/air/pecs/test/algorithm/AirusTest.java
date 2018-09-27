package com.westlake.air.pecs.test.algorithm;

import com.alibaba.fastjson.JSON;
import com.westlake.air.pecs.algorithm.Airus;
import com.westlake.air.pecs.domain.bean.airus.FinalResult;
import com.westlake.air.pecs.domain.bean.airus.ScoreData;
import com.westlake.air.pecs.parser.ScoreTsvParser;
import com.westlake.air.pecs.test.BaseTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.HashSet;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-12 21:18
 */
public class AirusTest extends BaseTest {

    @Autowired
    Airus airus;
    @Autowired
    ScoreTsvParser scoreTsvParser;

    @Test
    public void airusFinalNumberTest() {
        ScoreData scoreData = scoreTsvParser.getScoreData(new File(this.getClass().getClassLoader().getResource("SGSScoreResult.csv").getPath()), ScoreTsvParser.SPLIT_COMMA);
        //这里需要特殊处理一下这份入参的group_id,因为它并不适配pyprophet的系统
        String[] newGroupIds = new String[scoreData.getGroupId().length];
        HashSet<String> existedGroupId = new HashSet<>();
        int indexCount = 0;
        for (int i = 0; i < scoreData.getGroupId().length; i++) {
            String oldGroupId = scoreData.getGroupId()[i];
            String newGroupId = "";
            if (oldGroupId.contains("DECOY")) {
                oldGroupId = oldGroupId.replace("DECOY_", "");
                newGroupId = "DECOY_";
            }
            oldGroupId = oldGroupId.replace("_run0", "");
            if (!existedGroupId.contains(oldGroupId)) {
                indexCount++;
                existedGroupId.add(oldGroupId);
            }
            newGroupId = newGroupId + indexCount + "_run0";
            newGroupIds[i] = newGroupId;
        }
        scoreData.setGroupId(newGroupIds);

        Double[][] realDatas = scoreData.getScoreData();
        for (int i = 0; i < realDatas.length; i++) {
            realDatas[i][0] =
                    realDatas[i][1] * -0.19011762 +
                            realDatas[i][2] * 2.47298914 +
                            realDatas[i][7] * 5.63906731 +
                            realDatas[i][11] * -0.62640133 +
                            realDatas[i][12] * 0.36006925 +
                            realDatas[i][13] * 0.08814003 +
                            realDatas[i][3] * 0.13978311 +
                            realDatas[i][5] * -1.16475032 +
                            realDatas[i][16] * -0.19267813 +
                            realDatas[i][9] * -0.61712054;
        }

        FinalResult finalResult = airus.buildResult(scoreData);
        int count = 0;
        for (double d : finalResult.getAllInfo().getStatMetrics().getFdr()) {
            if (d < 0.01) {
                count++;
            }
        }

        System.out.println(count);

        assert count >= 322;

    }

    @Test
    public void airusTest() {
        ScoreData scoreData = scoreTsvParser.getScoreData(new File(this.getClass().getClassLoader().getResource("scores_data.tsv").getPath()), ScoreTsvParser.SPLIT_CHANGE_LINE);
        if (scoreData != null) {
            FinalResult finalResult = airus.buildResult(scoreData);
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
}
