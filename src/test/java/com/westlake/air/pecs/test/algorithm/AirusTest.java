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
    public void airusTest() {
        ScoreData scoreDataMap = scoreTsvParser.getScoreData(new File(this.getClass().getClassLoader().getResource("scores_data.tsv").getPath()));
        if (scoreDataMap != null) {
            FinalResult finalResult = airus.buildResult(scoreDataMap);
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
            double[] tpResult={332.951550, 368.414729, 366.296512, 368.346899, 368.346899, 368.346899, 368.346899, 368.346899, 368.346899};
            double[] fpResult={0.048450, 3.585271, 7.703488, 18.653101, 18.653101, 18.653101, 18.653101, 18.653101, 18.653101};
            double[] tnResult={18.701550, 15.164729, 11.046512, 0.096899, 0.096899, 0.096899, 0.096899, 0.096899, 0.096899};
            double[] fnResult={35.298450, -0.164729, 1.953488, -0.096899, -0.096899, -0.096899, -0.096899, -0.096899, -0.096899};
            double[] fprResult={0.002584, 0.191214, 0.410853, 0.994832, 0.994832, 0.994832, 0.994832, 0.994832, 0.994832};
            double[] fdrResult={0.000145, 0.009638, 0.020598, 0.048199, 0.048199, 0.048199, 0.048199, 0.048199, 0.048199};
            double[] fnrResult={0.653675, 0, 0.150268, 0, 0, 0, 0, 0, 0};
            double[] svalueResult={0.904145, 1, 1, 1, 1, 1, 1, 1, 1};

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

    private boolean isSimilar(Double[] array1, Double[] array2, Double tolerance){
        if(array1.length != array2.length) return false;
        boolean result = true;
        for(int i=0; i<array1.length; i++){
            if (Math.abs(array1[i] - array2[i]) > tolerance) {
                result = false;
            }
        }
        return result;
    }

    private boolean isSimilar(double[] array1, double[] array2, Double tolerance){
        if(array1.length != array2.length) return false;
        boolean result = true;
        for(int i=0; i<array1.length; i++){
            if (Math.abs(array1[i] - array2[i]) > tolerance) {
                result = false;
            }
        }
        return result;
    }
}
