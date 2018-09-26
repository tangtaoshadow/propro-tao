package com.westlake.air.pecs.algorithm;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.airus.ErrorStat;
import com.westlake.air.pecs.domain.bean.airus.Params;
import com.westlake.air.pecs.domain.bean.airus.Pi0Est;
import com.westlake.air.pecs.domain.bean.airus.StatMetrics;
import com.westlake.air.pecs.utils.AirusUtils;
import com.westlake.air.pecs.utils.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-13 16:55
 */
@Component
public class Stats {

    public static final Logger logger = LoggerFactory.getLogger(Stats.class);

    private Double[] pNormalizer(Double[] targetScores, Double[] decoyScores) {
        double mean = ArrayUtils.mean(decoyScores);
        double std = ArrayUtils.std(decoyScores);
        int targetScoresLength = targetScores.length;
        Double[] results = new Double[targetScoresLength];
        double args;
        for (int i = 0; i < targetScoresLength; i++) {
            args = (targetScores[i] - mean) / std;
            results[i] = 1 - (0.5 * (1.0 + ArrayUtils.erf(args / Math.sqrt(2.0))));
        }
        return results;
    }

    /**
     * 经验概率计算
     * @param targetScores
     * @param decoyScores
     * @return
     */
    public Double[] pEmpirical(Double[] targetScores, Double[] decoyScores) {
        int targetScoreLength = targetScores.length;
        int decoyScoreLength = decoyScores.length;
        int targetDecoyScoreLength = targetScoreLength + decoyScoreLength;
        double[] p = new double[targetScoreLength];
        Double[] targetDecoyScores = ArrayUtils.concat2d(targetScores, decoyScores);
        boolean[] vmid = new boolean[targetDecoyScoreLength];
        boolean[] v = new boolean[targetDecoyScoreLength];
        int[] u = new int[targetScoreLength];
        Integer[] perm = ArrayUtils.indexBeforeReversedSort(targetDecoyScores);
        for (int i = 0; i < targetScoreLength; i++) {
            vmid[i] = true;
        }
        for (int i = targetScoreLength; i < decoyScoreLength; i++) {
            vmid[i] = false;
        }
        for (int i = 0; i < v.length; i++) {
            v[i] = vmid[perm[i]];
        }
        int j = 0;
        for (int i = 0; i < targetDecoyScoreLength; i++) {
            if (v[i]) {
                u[j] = i;
                j++;
            }
        }
        for (int i = 0; i < targetScoreLength; i++) {
            p[i] = (u[i] - i) / (double) decoyScoreLength;
        }

        int ranks;
        double[] rankDataReversed = ArrayUtils.rankDataReversed(targetScores);
        Double[] pFinal = new Double[targetScoreLength];
        for (int i = 0; i < targetScoreLength; i++) {
            ranks = (int) Math.floor(rankDataReversed[i]) - 1;
            pFinal[i] = p[ranks];
        }
        double fix = 1.0/decoyScoreLength;
        for(int i=0;i<pFinal.length;i++){
            if(pFinal[i]<fix){
                pFinal[i] = fix;
            }
        }
        return pFinal;
    }

    /**
     * Calculate P relative scores.
     */
    private ResultDO<Pi0Est> pi0Est(Double[] pvalues, Double[] lambda, String pi0Method, boolean smoothLogPi0) {
        ResultDO<Pi0Est> resultDO = new ResultDO<Pi0Est>();
        Pi0Est pi0EstResults = new Pi0Est();
        int numOfPvalue = pvalues.length;
        int numOfLambda = 1;
        if (lambda != null) {
            numOfLambda = lambda.length;
            Arrays.sort(lambda);

        }
        Double[] meanPL = new Double[numOfPvalue];
        Double[] pi0Lambda = new Double[numOfLambda];
        Double pi0;
        Double[] pi0Smooth = new Double[numOfLambda];
        Double[] pi0s = new Double[numOfLambda];
        if (numOfLambda < 4) {
            resultDO.setMsgInfo("Pi0Est lambda Error.");
            return resultDO;
        }
        for (int i = 0; i < numOfLambda; i++) {
            for (int j = 0; j < numOfPvalue; j++) {
                if (pvalues[j] < lambda[i]) {
                    meanPL[j] = (double) 0;
                } else {
                    meanPL[j] = (double) 1;
                }
            }
            pi0Lambda[i] = ArrayUtils.mean(meanPL) / (1 - lambda[i]);
        }
        if (pi0Method.equals("smoother")) {
            if (smoothLogPi0) {
                    for (int i = 0; i < numOfLambda; i++) {
                        pi0s[i] = Math.log(pi0Lambda[i]);
                    }
                ResultDO<Double[]> pi0SmoothResult = ArrayUtils.lagrangeInterpolation(lambda, pi0s);
                if (pi0SmoothResult.isSuccess()) {
                    pi0Smooth = pi0SmoothResult.getModel();
                }
                for (int i = 0; i < numOfLambda; i++) {
                    pi0Smooth[i] = Math.exp(pi0Smooth[i]);
                }
            } else {
                ResultDO<Double[]> pi0SmoothResult = ArrayUtils.lagrangeInterpolation(lambda, pi0s);
                if (pi0SmoothResult.isSuccess()) {
                    pi0Smooth = pi0SmoothResult.getModel();
                }

            }
            pi0 = Math.min(pi0Smooth[numOfLambda - 1], (double) 1);
        } else if (pi0Method.equals("bootstrap")) {
            Double[] sortedPi0Lambda = pi0Lambda.clone();
            Arrays.sort(sortedPi0Lambda);
            int w;
            double[] mse = new double[numOfLambda];
            for (int i = 0; i < numOfLambda; i++) {
                w = ArrayUtils.countOverThreshold(pvalues, lambda[i]);
                mse[i] = (w / (Math.pow(numOfPvalue, 2) * Math.pow((1 - lambda[i]), 2))) * (1 - w / numOfPvalue) + Math.pow((pi0Lambda[i] - sortedPi0Lambda[0]), 2);
            }
            pi0 = Math.min(pi0Lambda[ArrayUtils.argmin(mse)], 1);
            pi0Smooth = null;
        } else {
            resultDO.setMsgInfo("Pi0Est Method Error.\n");
            return resultDO;
        }
        if (pi0 <= 0) {
            resultDO.setMsgInfo("Pi0Est Pi0 Error.\n");
            return resultDO;
        }
        pi0EstResults.setPi0(pi0);
        pi0EstResults.setPi0Smooth(pi0Smooth);
        pi0EstResults.setLambda(lambda);
        pi0EstResults.setPi0Lambda(pi0Lambda);
        resultDO.setSuccess(true);
        resultDO.setModel(pi0EstResults);
        return resultDO;
    }

    /**
     * Calculate P relative score when lambda is a single value.
     */
    private ResultDO<Pi0Est> pi0Est(Double[] pvalues, Double lambda, String pi0Method, boolean smoothLogPi0) {
        Pi0Est result = new Pi0Est();
        ResultDO<Pi0Est> resultDO = new ResultDO<Pi0Est>();
        int numOfPvalue = pvalues.length;
        Double[] meanPL = new Double[numOfPvalue];
        Double pi0Lambda;
        Double pi0;

        for (int j = 0; j < numOfPvalue; j++) {
            if (pvalues[j] < lambda) {
                meanPL[j] = (double) 0;
            } else {
                meanPL[j] = (double) 1;
            }
        }
        pi0Lambda = ArrayUtils.mean(meanPL);

        pi0 = Math.min(pi0Lambda, (double) 1);
        Double[] lamda = new Double[1];
        Double[] pi0Lamda = new Double[1];
        lamda[0] = lambda;
        pi0Lamda[0] = pi0Lambda;
        result.setLambda(lamda);
        result.setPi0(pi0);
        result.setPi0Lambda(pi0Lamda);
        result.setPi0Smooth(null);
        resultDO.setModel(result);
        resultDO.setSuccess(true);
        return resultDO;
    }

    /**
     * Calculate qvalues.
     */
    private Double[] qvalue(Double[] pvalues, double pi0, boolean pfdr) {
        int pvalueLength = pvalues.length;
        Integer[] u = ArrayUtils.indexBeforeSort(pvalues);
        double[] v = ArrayUtils.rankDataMax(pvalues);
        Double[] qvalues = new Double[pvalueLength];
        for (int i = 0; i < pvalueLength; i++) {

            if (pfdr) {
                qvalues[i] = (pi0 * pvalueLength * pvalues[i]) / (v[i] * (1 - Math.pow((1 - pvalues[i]), pvalueLength)));
            } else {
                qvalues[i] = (pi0 * pvalueLength * pvalues[i]) / v[i];
            }
        }
        qvalues[u[pvalueLength - 1]] = Math.min(qvalues[u[pvalueLength - 1]], 1);
        for (int i = pvalueLength - 2; i >= 0; i--) {
            qvalues[u[i]] = Math.min(qvalues[u[i]], qvalues[u[i + 1]]);
        }
        return qvalues;
    }

    private StatMetrics statMetrics(Double[] pvalues, Double pi0, boolean pfdr) {
        StatMetrics results = new StatMetrics();
        int numOfPvalue = pvalues.length;
        int[] numPositives = ArrayUtils.countNumPositives(pvalues);
        int[] numNegatives = new int[numOfPvalue];
        for (int i = 0; i < numOfPvalue; i++) {
            numNegatives[i] = numOfPvalue - numPositives[i];
        }
        double numNull = pi0 * numOfPvalue;
        double[] tp = new double[numOfPvalue];
        double[] fp = new double[numOfPvalue];
        double[] tn = new double[numOfPvalue];
        double[] fn = new double[numOfPvalue];
        double[] fpr = new double[numOfPvalue];
        double[] fdr = new double[numOfPvalue];
        double[] fnr = new double[numOfPvalue];
        double[] sens = new double[numOfPvalue];
        double[] svalues = new double[numOfPvalue];
        for (int i = 0; i < numOfPvalue; i++) {
            tp[i] = (double) numPositives[i] - numNull * pvalues[i];
            fp[i] = numNull * pvalues[i];
            tn[i] = numNull * (1.0 - pvalues[i]);
            fn[i] = (double) numNegatives[i] - numNull * (1.0 - pvalues[i]);
            fpr[i] = fp[i] / numNull;
            if (numPositives[i] == 0) {
                fdr[i] = 0.0;
            } else {
                fdr[i] = fp[i] / (double) numPositives[i];
            }
            if (numNegatives[i] == 0) {
                fnr[i] = 0.0;
            } else {
                fnr[i] = fn[i] / (double) numNegatives[i];
            }
            if (pfdr) {
                fdr[i] /= (1.0 - (1.0 - Math.pow(pvalues[i], numOfPvalue)));
                fnr[i] /= 1.0 - Math.pow(pvalues[i], numOfPvalue);
                if (pvalues[i] == 0) {
                    fdr[i] = 1.0 / numOfPvalue;
                    fnr[i] = 1.0 / numOfPvalue;
                }
            }
            sens[i] = tp[i] / ((double) numOfPvalue - numNull);
            if (sens[i] < 0.0) sens[i] = 0.0;
            if (sens[i] > 1.0) sens[i] = 1.0;
            if (fdr[i] < 0.0) fdr[i] = 0.0;
            if (fdr[i] > 1.0) fdr[i] = 1.0;
            if (fnr[i] < 0.0) fnr[i] = 0.0;
            if (fnr[i] > 1.0) fnr[i] = 1.0;
        }

        svalues = ArrayUtils.reverse(ArrayUtils.cumMax(ArrayUtils.reverse(sens)));
        results.setTp(tp);
        results.setFp(fp);
        results.setTn(tn);
        results.setFn(fn);
        results.setFpr(fpr);
        results.setFdr(fdr);
        results.setFnr(fnr);
        results.setSvalue(svalues);
        return results;
    }

    /**
     * Estimate final results.
     * TODO 没有实现 pep(lfdr);
     */
    public ErrorStat errorStatistics(Double[] targetScoresOriginal, Double[] decoyScoresOriginal) {
        Params params = new Params();
        ErrorStat errorStat = new ErrorStat();
        Double[] targetScores = targetScoresOriginal.clone();
        Double[] decoyScores = decoyScoresOriginal.clone();
        Double[] targetPvalues;
        Arrays.sort(targetScores);
        Arrays.sort(decoyScores);

        /*
        compute p-values using decoy scores;
         */
        if (params.isParametric()) {
            targetPvalues = pNormalizer(targetScores, decoyScores);
        } else {
            targetPvalues = pEmpirical(targetScores, decoyScores);
        }

        /*
        estimate pi0;
         */
        Pi0Est pi0Est = pi0Est(targetPvalues, params.getPi0Lambda(), params.getPi0Method(), params.isPi0SmoothLogPi0()).getModel();

        /*
        compute q-value;
         */
        Double[] targetQvalues = qvalue(targetPvalues, pi0Est.getPi0(), params.isPFdr());

        /*
        compute other metrics;
         */
        StatMetrics statMetrics = statMetrics(targetPvalues, pi0Est.getPi0(), params.isPFdr());

        errorStat.setCutoff(targetScores);
        errorStat.setPvalue(targetPvalues);
        errorStat.setQvalue(targetQvalues);
        errorStat.setStatMetrics(statMetrics);
        errorStat.setPi0Est(pi0Est);

        return errorStat;
    }


    /**
     * Finds cut-off target score for specified false discovery rate(fdr).
     */
    public Double findCutoff(Double[] topTargetScores, Double[] topDecoyScores, Double cutoffFdr) {
        ErrorStat errorStat = errorStatistics(topTargetScores, topDecoyScores);
        Double[] qvalues = errorStat.getQvalue();
        double[] qvalue_CutoffAbs = new double[qvalues.length];
        for(int i=0;i<qvalues.length;i++){
            qvalue_CutoffAbs[i] = Math.abs(qvalues[i]-cutoffFdr);
        }
        int i0 = ArrayUtils.argmin(qvalue_CutoffAbs);
        return errorStat.getCutoff()[i0];
    }
}
