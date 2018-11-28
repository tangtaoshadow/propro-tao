package com.westlake.air.pecs.algorithm;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.airus.ErrorStat;
import com.westlake.air.pecs.domain.bean.airus.Params;
import com.westlake.air.pecs.domain.bean.airus.Pi0Est;
import com.westlake.air.pecs.domain.bean.airus.StatMetrics;
import com.westlake.air.pecs.domain.bean.score.SimpleFeatureScores;
import com.westlake.air.pecs.utils.AirusUtil;
import com.westlake.air.pecs.utils.ArrayUtil;
import com.westlake.air.pecs.utils.MathUtil;
import com.westlake.air.pecs.utils.SortUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-13 16:55
 */
@Component
public class Stats {

    public static final Logger logger = LoggerFactory.getLogger(Stats.class);

    public void pNormalizer(List<SimpleFeatureScores> targetScores, List<SimpleFeatureScores> decoyScores) {
        Double[] decoyScoresArray = AirusUtil.buildMainScoreArray(decoyScores, false);
        double mean = MathUtil.mean(decoyScoresArray);
        double std = MathUtil.std(decoyScoresArray, mean);
        double args;
        for (SimpleFeatureScores sfs : targetScores) {
            args = (sfs.getMainScore() - mean) / std;
            sfs.setPValue(1 - (0.5 * (1.0 + MathUtil.erf(args / Math.sqrt(2.0)))));
        }
    }

    public Double[] pNormalizer(Double[] targetScores, Double[] decoyScores) {
        double mean = MathUtil.mean(decoyScores);
        double std = MathUtil.std(decoyScores, mean);
        int targetScoresLength = targetScores.length;
        Double[] results = new Double[targetScoresLength];
        double args;
        for (int i = 0; i < targetScoresLength; i++) {
            args = (targetScores[i] - mean) / std;
            results[i] = 1 - (0.5 * (1.0 + MathUtil.erf(args / Math.sqrt(2.0))));
        }
        return results;
    }

    public void pEmpirical(List<SimpleFeatureScores> targetScores, List<SimpleFeatureScores> decoyScores) {
        List<SimpleFeatureScores> totalScores = new ArrayList<>();
        totalScores.addAll(targetScores);
        totalScores.addAll(decoyScores);

        totalScores = SortUtil.sortByMainScore(totalScores, true);
        int decoyCount = 0;
        int decoyTotal = decoyScores.size();
        double fix = 1.0 / decoyTotal;
        for (SimpleFeatureScores sfs : totalScores) {
            if (sfs.getIsDecoy()) {
                decoyCount++;
            } else {
                double pValue = (double) decoyCount / decoyTotal;
                if (pValue < fix) {
                    sfs.setPValue(fix);
                } else {
                    sfs.setPValue(pValue);
                }
            }
        }
    }

    /**
     * 经验概率计算
     *
     * @param targetScores
     * @param decoyScores
     * @return
     */
    public Double[] pEmpirical(Double[] targetScores, Double[] decoyScores) {
        int targetScoreLength = targetScores.length;
        int decoyScoreLength = decoyScores.length;
        int targetDecoyScoreLength = targetScoreLength + decoyScoreLength;
        //计算每一个真肽段前的伪肽段数目占伪肽段总数目的比例,越小越好
        double[] p = new double[targetScoreLength];
        Double[] targetDecoyScores = ArrayUtils.addAll(targetScores, decoyScores);
        //排序前的真伪标记
        boolean[] vmid = new boolean[targetDecoyScoreLength];
        //排序后的真伪标记
        boolean[] v = new boolean[targetDecoyScoreLength];
        //记录真实肽段在排序后数组中的位置
        int[] u = new int[targetScoreLength];
        //从大到小排序
        Integer[] perm = ArrayUtil.indexBeforeReversedSort(targetDecoyScores);
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
            //u[i]-i=在该真实肽段前有多少个伪肽段,数组p中将存放一个由小到大的数值
            p[i] = (u[i] - i) / (double) decoyScoreLength;
        }

        int ranks;
        double[] averageRankReversed = ArrayUtil.averageRankReverse(targetScores);
        Double[] pFinal = new Double[targetScoreLength];
        for (int i = 0; i < targetScoreLength; i++) {
            //舍去排名中小数点后的位置,然后减1.例如平均排名5.5,那就认为是5
            ranks = (int) Math.floor(averageRankReversed[i]) - 1;
            pFinal[i] = p[ranks];
        }
        double fix = 1.0 / decoyScoreLength;
        for (int i = 0; i < pFinal.length; i++) {
            if (pFinal[i] < fix) {
                pFinal[i] = fix;
            }
        }
        return pFinal;
    }

    /**
     * Calculate P relative scores.
     */
    private Pi0Est pi0Est(List<SimpleFeatureScores> targets, Double[] lambda, String pi0Method, boolean smoothLogPi0) {

        Pi0Est pi0EstResults = new Pi0Est();
        int numOfPvalue = targets.size();
        int numOfLambda = 1;
        if (lambda != null) {
            numOfLambda = lambda.length;
//            Arrays.sort(lambda);
        }
        Double[] meanPL = new Double[numOfPvalue];
        Double[] pi0Lambda = new Double[numOfLambda];
        Double pi0;
        Double[] pi0Smooth = new Double[numOfLambda];
        Double[] pi0s = new Double[numOfLambda];
        if (numOfLambda < 4) {
            logger.error("Pi0Est lambda Error, numOfLambda < 4");
            return null;
        }
        for (int i = 0; i < numOfLambda; i++) {
            for (int j = 0; j < numOfPvalue; j++) {
                if (targets.get(j).getPValue() < lambda[i]) {
                    meanPL[j] = 0d;
                } else {
                    meanPL[j] = 1d;
                }
            }
            pi0Lambda[i] = MathUtil.mean(meanPL) / (1 - lambda[i]);
        }
        if (pi0Method.equals("smoother")) {
            if (smoothLogPi0) {
                for (int i = 0; i < numOfLambda; i++) {
                    pi0s[i] = Math.log(pi0Lambda[i]);
                }
                ResultDO<Double[]> pi0SmoothResult = MathUtil.lagrangeInterpolation(lambda, pi0s);
                if (pi0SmoothResult.isSuccess()) {
                    pi0Smooth = pi0SmoothResult.getModel();
                }
                for (int i = 0; i < numOfLambda; i++) {
                    pi0Smooth[i] = Math.exp(pi0Smooth[i]);
                }
            } else {
                ResultDO<Double[]> pi0SmoothResult = MathUtil.lagrangeInterpolation(lambda, pi0s);
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
                w = AirusUtil.countOverThreshold(targets, lambda[i]);
                mse[i] = (w / (Math.pow(numOfPvalue, 2) * Math.pow((1 - lambda[i]), 2))) * (1 - (double) w / numOfPvalue) + Math.pow((pi0Lambda[i] - sortedPi0Lambda[0]), 2);
            }
            double min = 100;
            int index = 0;
            for (int i = 0; i < mse.length; i++) {
                if (pi0Lambda[i] > 0 && mse[i] < min) {
                    min = mse[i];
                    index = i;
                }
            }
            pi0 = Math.min(pi0Lambda[index], 1);
            pi0Smooth = null;
        } else {
            logger.error("Pi0Est Method Error.No Method Called " + pi0Method);
            return null;
        }
        if (pi0 <= 0) {
            logger.error("Pi0Est Pi0 Error -- pi0<=0");
            return null;
        }
        pi0EstResults.setPi0(pi0);
        pi0EstResults.setPi0Smooth(pi0Smooth);
        pi0EstResults.setLambda(lambda);
        pi0EstResults.setPi0Lambda(pi0Lambda);
        return pi0EstResults;
    }

    /**
     * Calculate P relative scores.
     */
    private Pi0Est pi0Est(Double[] pvalues, Double[] lambda, String pi0Method, boolean smoothLogPi0) {

        Pi0Est pi0EstResults = new Pi0Est();
        int numOfPvalue = pvalues.length;
        int numOfLambda = 1;
        if (lambda != null) {
            numOfLambda = lambda.length;
//            Arrays.sort(lambda);
        }
        Double[] meanPL = new Double[numOfPvalue];
        Double[] pi0Lambda = new Double[numOfLambda];
        Double pi0;
        Double[] pi0Smooth = new Double[numOfLambda];
        Double[] pi0s = new Double[numOfLambda];
        if (numOfLambda < 4) {
            logger.error("Pi0Est lambda Error, numOfLambda < 4");
            return null;
        }
        for (int i = 0; i < numOfLambda; i++) {
            for (int j = 0; j < numOfPvalue; j++) {
                if (pvalues[j] < lambda[i]) {
                    meanPL[j] = 0d;
                } else {
                    meanPL[j] = 1d;
                }
            }
            pi0Lambda[i] = MathUtil.mean(meanPL) / (1 - lambda[i]);
        }
        if (pi0Method.equals("smoother")) {
            if (smoothLogPi0) {
                for (int i = 0; i < numOfLambda; i++) {
                    pi0s[i] = Math.log(pi0Lambda[i]);
                }
                ResultDO<Double[]> pi0SmoothResult = MathUtil.lagrangeInterpolation(lambda, pi0s);
                if (pi0SmoothResult.isSuccess()) {
                    pi0Smooth = pi0SmoothResult.getModel();
                }
                for (int i = 0; i < numOfLambda; i++) {
                    pi0Smooth[i] = Math.exp(pi0Smooth[i]);
                }
            } else {
                ResultDO<Double[]> pi0SmoothResult = MathUtil.lagrangeInterpolation(lambda, pi0s);
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
                w = MathUtil.countOverThreshold(pvalues, lambda[i]);
                mse[i] = (w / (Math.pow(numOfPvalue, 2) * Math.pow((1 - lambda[i]), 2))) * (1 - (double) w / numOfPvalue) + Math.pow((pi0Lambda[i] - sortedPi0Lambda[0]), 2);
            }
            double min = 100;
            int index = 0;
            for (int i = 0; i < mse.length; i++) {
                if (pi0Lambda[i] > 0 && mse[i] < min) {
                    min = mse[i];
                    index = i;
                }
            }
            pi0 = Math.min(pi0Lambda[index], 1);
            pi0Smooth = null;
        } else {
            logger.error("Pi0Est Method Error.No Method Called " + pi0Method);
            return null;
        }
        if (pi0 <= 0) {
            logger.error("Pi0Est Pi0 Error -- pi0<=0");
            return null;
        }
        pi0EstResults.setPi0(pi0);
        pi0EstResults.setPi0Smooth(pi0Smooth);
        pi0EstResults.setLambda(lambda);
        pi0EstResults.setPi0Lambda(pi0Lambda);
        return pi0EstResults;
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
        pi0Lambda = MathUtil.mean(meanPL);

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
     * targets的qvalue需要从大到小排序
     */
    public void qvalue(List<SimpleFeatureScores> targets, double pi0, boolean pfdr) {
        Double[] pValues = AirusUtil.buildPValueArray(targets, false);
        int pValueLength = targets.size();
        double[] v = ArrayUtil.rank(pValues);
        for (int i = 0; i < pValueLength; i++) {
            if (pfdr) {
                targets.get(i).setQValue((pi0 * pValueLength * targets.get(i).getPValue()) / (v[i] * (1 - Math.pow((1 - targets.get(i).getPValue()), pValueLength))));
            } else {
                targets.get(i).setQValue((pi0 * pValueLength * targets.get(i).getPValue()) / v[i]);
            }
        }
        targets.get(0).setQValue(Math.min(targets.get(0).getQValue(), 1));

        for (int i = 1; i < pValueLength - 1; i++) {
            targets.get(i).setQValue(Math.min(targets.get(i).getQValue(), targets.get(i - 1).getQValue()));
        }
    }

    /**
     * Calculate qvalues.
     */
    public Double[] qvalue(Double[] pvalues, double pi0, boolean pfdr) {
        int pvalueLength = pvalues.length;
        Integer[] u = ArrayUtil.indexAfterSort(pvalues);
        double[] v = ArrayUtil.rank(pvalues);
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

    private StatMetrics statMetrics(List<SimpleFeatureScores> scores, Double pi0, boolean pfdr) {
        StatMetrics results = new StatMetrics();
        int numOfPvalue = scores.size();
        int[] numPositives = AirusUtil.countPValueNumPositives(scores);
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
        double[] svalues;
        for (int i = 0; i < numOfPvalue; i++) {
            tp[i] = (double) numPositives[i] - numNull * scores.get(i).getPValue();
            fp[i] = numNull * scores.get(i).getPValue();
            tn[i] = numNull * (1.0 - scores.get(i).getPValue());
            fn[i] = (double) numNegatives[i] - numNull * (1.0 - scores.get(i).getPValue());
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
                fdr[i] /= (1.0 - (1.0 - Math.pow(scores.get(i).getPValue(), numOfPvalue)));
                fnr[i] /= 1.0 - Math.pow(scores.get(i).getPValue(), numOfPvalue);
                if (scores.get(i).getPValue() == 0) {
                    fdr[i] = 1.0 / numOfPvalue;
                    fnr[i] = 1.0 / numOfPvalue;
                }
            }
            sens[i] = tp[i] / ((double) numOfPvalue - numNull);
            if (sens[i] < 0.0) {
                sens[i] = 0.0;
            }
            if (sens[i] > 1.0) {
                sens[i] = 1.0;
            }
            if (fdr[i] < 0.0) {
                fdr[i] = 0.0;
            }
            if (fdr[i] > 1.0) {
                fdr[i] = 1.0;
            }
            if (fnr[i] < 0.0) {
                fnr[i] = 0.0;
            }
            if (fnr[i] > 1.0) {
                fnr[i] = 1.0;
            }
            scores.get(i).setFdr(fdr[i]);
        }

        svalues = ArrayUtil.reverse(MathUtil.cumMax(ArrayUtil.reverse(sens)));
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

    private StatMetrics statMetrics(Double[] pvalues, Double pi0, boolean pfdr) {
        StatMetrics results = new StatMetrics();
        int numOfPvalue = pvalues.length;
        int[] numPositives = MathUtil.countNumPositives(pvalues);
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
        double[] svalues;
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

        svalues = ArrayUtil.reverse(MathUtil.cumMax(ArrayUtil.reverse(sens)));
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
    public ErrorStat errorStatistics(Double[] targetScoresOriginal, Double[] decoyScoresOriginal, Params params) {

        ErrorStat errorStat = new ErrorStat();
        Double[] targetScores = targetScoresOriginal.clone();
        Double[] decoyScores = decoyScoresOriginal.clone();
        Double[] targetPvalues;
        Arrays.sort(targetScores);
        Arrays.sort(decoyScores);

        //compute p-values using decoy scores;
        if (params.isParametric()) {
            targetPvalues = pNormalizer(targetScores, decoyScores);
        } else {
            targetPvalues = pEmpirical(targetScores, decoyScores);
        }

        //estimate pi0;
        Pi0Est pi0Est = pi0Est(targetPvalues, params.getPi0Lambda(), params.getPi0Method(), params.isPi0SmoothLogPi0());
//        logger.info("Pi0:" + pi0Est.getPi0());
        //compute q-value;
        Double[] targetQvalues = qvalue(targetPvalues, pi0Est.getPi0(), params.isPFdr());
        //compute other metrics;
        StatMetrics statMetrics = statMetrics(targetPvalues, pi0Est.getPi0(), params.isPFdr());

        errorStat.setCutoff(targetScores);
        errorStat.setPvalue(targetPvalues);
        errorStat.setQvalue(targetQvalues);
        errorStat.setStatMetrics(statMetrics);
        errorStat.setPi0Est(pi0Est);

        return errorStat;
    }

    /**
     * Estimate final results.
     * TODO 没有实现 pep(lfdr);
     */
    public ErrorStat errorStatistics(List<SimpleFeatureScores> scores, Params params) {

        List<SimpleFeatureScores> targets = new ArrayList<>();
        List<SimpleFeatureScores> decoys = new ArrayList<>();
        for (SimpleFeatureScores featureScores : scores) {
            if (featureScores.getIsDecoy()) {
                decoys.add(featureScores);
            } else {
                targets.add(featureScores);
            }
        }

        return errorStatistics(targets, decoys, params);
    }

    /**
     * Estimate final results.
     * TODO 没有实现 pep(lfdr);
     */
    public ErrorStat errorStatistics(List<SimpleFeatureScores> targets, List<SimpleFeatureScores> decoys, Params params) {

        ErrorStat errorStat = new ErrorStat();
        List<SimpleFeatureScores> sortedTargets = SortUtil.sortByMainScore(targets, false);
        List<SimpleFeatureScores> sortedDecoys = SortUtil.sortByMainScore(decoys, false);

        //compute p-values using decoy scores;
        if (params.isParametric()) {
            pNormalizer(sortedTargets, sortedDecoys);
        } else {
            pEmpirical(sortedTargets, sortedDecoys);
        }

        //estimate pi0;
        Pi0Est pi0Est = pi0Est(sortedTargets, params.getPi0Lambda(), params.getPi0Method(), params.isPi0SmoothLogPi0());
        if (pi0Est == null) {
            return null;
        }
        //compute q-value;
        qvalue(sortedTargets, pi0Est.getPi0(), params.isPFdr());
        //compute other metrics;
        StatMetrics statMetrics = statMetrics(sortedTargets, pi0Est.getPi0(), params.isPFdr());

        errorStat.setBestFeatureScoresList(targets);
        errorStat.setStatMetrics(statMetrics);
        errorStat.setPi0Est(pi0Est);

        return errorStat;
    }


    /**
     * Finds cut-off target score for specified false discovery rate(fdr).
     */
    public Double findCutoff(Double[] topTargetScores, Double[] topDecoyScores, Double cutoffFdr, Params params) {
        ErrorStat errorStat = errorStatistics(topTargetScores, topDecoyScores, params);
        Double[] qvalues = errorStat.getQvalue();
        double[] qvalue_CutoffAbs = new double[qvalues.length];
        for (int i = 0; i < qvalues.length; i++) {
            qvalue_CutoffAbs[i] = Math.abs(qvalues[i] - cutoffFdr);
        }
        int i0 = MathUtil.argmin(qvalue_CutoffAbs);
        return errorStat.getCutoff()[i0];
    }

    /**
     * Finds cut-off target score for specified false discovery rate(fdr).
     */
    public Double findCutoff(List<SimpleFeatureScores> topTargets, List<SimpleFeatureScores> topDecoys, Params params) {
        ErrorStat errorStat = errorStatistics(topTargets, topDecoys, params);

        List<SimpleFeatureScores> bestScores = errorStat.getBestFeatureScoresList();
        double[] qvalue_CutoffAbs = new double[bestScores.size()];
        for (int i = 0; i < bestScores.size(); i++) {
            qvalue_CutoffAbs[i] = Math.abs(bestScores.get(i).getQValue() - params.getSsInitialFdr());
        }
        int i0 = MathUtil.argmin(qvalue_CutoffAbs);
        return bestScores.get(i0).getMainScore();
    }
}
