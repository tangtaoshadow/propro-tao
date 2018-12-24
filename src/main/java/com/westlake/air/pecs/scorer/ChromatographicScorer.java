package com.westlake.air.pecs.scorer;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.westlake.air.pecs.constants.ScoreType;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.pecs.domain.bean.math.BisectionLowHigh;
import com.westlake.air.pecs.domain.bean.score.ExperimentFeature;
import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import com.westlake.air.pecs.utils.MathUtil;
import com.westlake.air.pecs.utils.ScoreUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-15 16:06
 * <p>
 * scores.xcorr_coelution_score 互相关偏移的mean + std
 * scores.weighted_coelution_score 带权重的相关偏移sum
 * scores.xcorr_shape_score 互相关序列最大值的平均值
 * scores.weighted_xcorr_shape 带权重的互相关序列最大值的平均值
 * scores.log_sn_score log(距离ApexRt最近点的stn值之和)
 * <p>
 * scores.var_intensity_score 同一个peptideRef下, 所有HullPoints的intensity之和 除以 所有intensity之和
 */
@Component("chromatographicScorer")
public class ChromatographicScorer {

    /**
     * @param experimentFeatures list of features in selected mrmfeature
     */
    public void calculateChromatographicScores(List<ExperimentFeature> experimentFeatures, List<Double> libraryIntensity, FeatureScores scores, HashSet<String> scoreTypes) {
        Table<Integer, Integer, Double[]> xcorrMatrix = initializeXCorrMatrix(experimentFeatures);

        //xcorrCoelutionScore
        //xcorrCoelutionScoreWeighted
        //xcorrShapeScore
        //xcorrShapeScoreWeighted
        double[] normalizedLibraryIntensity = ScoreUtil.normalizeSumDouble(libraryIntensity);
        List<Integer> deltas = new ArrayList<>();
        List<Double> deltasWeighted = new ArrayList<>();
        List<Double> intensities = new ArrayList<>();
        List<Double> intensitiesWeighted = new ArrayList<>();
        Double[] value;
        int max;
        for (int i = 0; i < experimentFeatures.size(); i++) {
            value = xcorrMatrix.get(i, i);
            max = MathUtil.findMaxIndex(value);
            deltasWeighted.add(Math.abs(max - (value.length - 1) / 2) * normalizedLibraryIntensity[i] * normalizedLibraryIntensity[i]);
            intensitiesWeighted.add(value[max] * normalizedLibraryIntensity[i] * normalizedLibraryIntensity[i]);
            for (int j = i; j < experimentFeatures.size(); j++) {
                value = xcorrMatrix.get(i, j);
                max = MathUtil.findMaxIndex(value);
                deltas.add(Math.abs(max - (value.length - 1) / 2)); //first: maxdelay //delta: 偏移量
                intensities.add(value[max]);//value[max] 吻合系数
                if (j != i) {
                    deltasWeighted.add(Math.abs(max - (value.length - 1) / 2) * normalizedLibraryIntensity[i] * normalizedLibraryIntensity[j] * 2d);
                    intensitiesWeighted.add(value[max] * normalizedLibraryIntensity[i] * normalizedLibraryIntensity[j] * 2d);
                }
            }
        }
        double sumDelta = 0.0d, sumDeltaWeighted = 0.0d, sumIntensity = 0.0d, sumIntensityWeighted = 0.0d;
        for (int i = 0; i < deltas.size(); i++) {
            sumDelta += deltas.get(i);
            sumDeltaWeighted += deltasWeighted.get(i);
            sumIntensity += intensities.get(i);
            sumIntensityWeighted += intensitiesWeighted.get(i);
        }
        double meanDelta = sumDelta / deltas.size();
        double meanIntensity = sumIntensity / intensities.size();
        sumDelta = 0;
        for (int delta : deltas) {
            sumDelta += (delta - meanDelta) * (delta - meanDelta);
        }
        //TODO WRM 这里可能会出现deltas.size()==1的情况
        double stdDelta = 0d;
        if (deltas.size() != 1) {
            stdDelta = Math.sqrt(sumDelta / (deltas.size() - 1));
        }
        if (scoreTypes == null || scoreTypes.contains(ScoreType.XcorrCoelution.getTypeName())) {
            scores.put(ScoreType.XcorrCoelution, meanDelta + stdDelta); //时间偏差
        }
        if (scoreTypes == null || scoreTypes.contains(ScoreType.XcorrCoelutionWeighted.getTypeName())) {
            scores.put(ScoreType.XcorrCoelutionWeighted, sumDeltaWeighted);
        }
        if (scoreTypes == null || scoreTypes.contains(ScoreType.XcorrShape.getTypeName())) {
            scores.put(ScoreType.XcorrShape, meanIntensity); // 平均的吻合程度--> 新的吻合系数
        }
        if (scoreTypes == null || scoreTypes.contains(ScoreType.XcorrShapeWeighted.getTypeName())) {
            scores.put(ScoreType.XcorrShapeWeighted, sumIntensityWeighted);
        }
    }

    public void calculateLogSnScore(List<RtIntensityPairsDouble> chromatograms, List<ExperimentFeature> experimentFeatures, List<double[]> signalToNoiseList, FeatureScores scores) {
        //logSnScore
        // log(mean of Apex sn s)
        double rt;
        int leftIndex, rightIndex;
        double snScore = 0.0d;
        if (signalToNoiseList.size() == 0) {
            snScore = 0.0d;
        }
        for (int k = 0; k < signalToNoiseList.size(); k++) {
            rt = experimentFeatures.get(0).getRt(); //max peak rt
            BisectionLowHigh bisectionLowHigh = MathUtil.bisection(chromatograms.get(k), rt);
            leftIndex = bisectionLowHigh.getLow();
            rightIndex = bisectionLowHigh.getHigh();
            if (Math.abs(chromatograms.get(k).getRtArray()[leftIndex] - rt) < Math.abs(chromatograms.get(k).getRtArray()[rightIndex] - rt)) {
                snScore += signalToNoiseList.get(k)[leftIndex];
            } else {
                snScore += signalToNoiseList.get(k)[rightIndex];
            }
        }
        snScore /= signalToNoiseList.size();
        if (snScore < 1) {
            scores.put(ScoreType.LogSnScore, 0d);
        } else {
            scores.put(ScoreType.LogSnScore, Math.log(snScore));
        }
    }


    /**
     * Get the XCorrMatrix with experiment Features
     * 对于一个 mrmFeature，算其中 chromatogramFeature 的 xcorrMatrix
     *
     * @param experimentFeatures features in mrmFeature
     *                           HullInt: redistributed chromatogram in range of (peptideRef constant) leftRt and rightRt
     * @return Table<Integer , Integer , Float [ ]> xcorrMatrix
     */
    private Table<Integer, Integer, Double[]> initializeXCorrMatrix(List<ExperimentFeature> experimentFeatures) {
        int listLength = experimentFeatures.size();
        Table<Integer, Integer, Double[]> xcorrMatrix = HashBasedTable.create();
        double[] intensityi, intensityj;
        HashMap<Integer, double[]> standardizeDataMap = new HashMap<>();
        for (int i = 0; i < listLength; i++) {
            standardizeDataMap.put(i, MathUtil.standardizeData(experimentFeatures.get(i).getHullInt()));
        }
        for (int i = 0; i < listLength; i++) {
            for (int j = i; j < listLength; j++) {
                intensityi = standardizeDataMap.get(i);
                intensityj = standardizeDataMap.get(j);
                xcorrMatrix.put(i, j, calculateCrossCorrelation(intensityi, intensityj));
            }
        }
        return xcorrMatrix;
    }

    /**
     * xcorrMatrix的意义：sum(反斜向的元素)/data.length(3)
     * 0   1   2
     * 0   |0  |1  |2
     * 1   |-1 |0  |1
     * 2   |-2 |-1 |0
     *
     * @param data1 chromatogram feature
     * @param data2 the same length as data1
     * @return value of xcorrMatrix element
     */
    private Double[] calculateCrossCorrelation(double[] data1, double[] data2) {
        int maxDelay = data1.length;
        Double[] output = new Double[maxDelay * 2 + 1];
        double sxy;
        int j;
        for (int delay = -maxDelay; delay <= maxDelay; delay++) {
            sxy = 0;
            for (int i = 0; i < maxDelay; i++) {
                j = i + delay;
                if (j < 0 || j >= maxDelay) {
                    continue;
                }
                sxy += (data1[i] * data2[j]);
            }
            output[delay + maxDelay] = sxy / maxDelay;
        }
        return output;
    }

}
