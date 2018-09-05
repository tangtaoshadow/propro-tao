package com.westlake.air.pecs.scorer;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairs;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.pecs.domain.bean.math.BisectionLowHigh;
import com.westlake.air.pecs.domain.bean.score.ExperimentFeature;
import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import com.westlake.air.pecs.utils.MathUtil;
import com.westlake.air.pecs.utils.ScoreUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-15 16:06
 *
 * scores.xcorr_coelution_score
 * scores.weighted_coelution_score
 * scores.xcorr_shape_score
 * scores.weighted_xcorr_shape
 * scores.log_sn_score
 *
 * scores.var_intensity_score
 */
@Component("chromatographicScorer")
public class ChromatographicScorer {

    /**
     * @param chromatograms chromatogram list of transition group
     * @param experimentFeatures list of features in selected mrmfeature
     * @param signalToNoiseList signal to noise list of chromatogram list
     */
    public void calculateChromatographicScores(List<RtIntensityPairsDouble> chromatograms, List<ExperimentFeature> experimentFeatures, List<Float> libraryIntensity, List<double[]> signalToNoiseList, FeatureScores scores){
        Table<Integer, Integer, Double[]> xcorrMatrix = initializeXCorrMatrix(experimentFeatures);

        //xcorrCoelutionScore
        //xcorrCoelutionScoreWeighted
        //xcorrShapeScore
        //xcorrShapeScoreWeighted
        double[] normalizedLibraryIntensity = ScoreUtil.normalizeSum(libraryIntensity);
        List<Integer> deltas = new ArrayList<>();
        List<Double> deltasWeighted = new ArrayList<>();
        List<Double> intensities = new ArrayList<>();
        List<Double> intensitiesWeighted = new ArrayList<>();
        Double[] value;
        int max;
        for(int i = 0; i<experimentFeatures.size(); i++){
            value = xcorrMatrix.get(i, i);
            max = MathUtil.findMaxIndex(value);
            deltasWeighted.add(Math.abs(max - (value.length - 1)/2) * normalizedLibraryIntensity[i] * normalizedLibraryIntensity[i]);
            intensitiesWeighted.add(value[max] * normalizedLibraryIntensity[i] * normalizedLibraryIntensity[i]);
            for (int j = i; j<experimentFeatures.size(); j++){
                value = xcorrMatrix.get(i, j);
                max = MathUtil.findMaxIndex(value);
                deltas.add(Math.abs(max - (value.length - 1)/2)); //first: maxdelay //delta: 偏移量
                intensities.add(value[max]);//value[max] 吻合系数
                if(j !=i){
                    deltasWeighted.add(Math.abs(max - (value.length + 1)/2) * normalizedLibraryIntensity[i] * normalizedLibraryIntensity[j] * 2f);
                    intensitiesWeighted.add(value[max] * normalizedLibraryIntensity[i] * normalizedLibraryIntensity[j] * 2f);
                }
            }
        }
        double sumDelta = 0.0d, sumDeltaWeighted = 0.0d, sumIntensity = 0.0d, sumIntensityWeighted = 0.0d;
        for(int i=0; i<deltas.size(); i++){
            sumDelta += deltas.get(i);
            sumDeltaWeighted += deltasWeighted.get(i);
            sumIntensity += intensities.get(i);
            sumIntensityWeighted += intensitiesWeighted.get(i);
        }
        double meanDelta = sumDelta / deltas.size();
        double meanIntensity = sumIntensity / intensities.size();
        sumDelta = 0;
        for(int delta: deltas){
            sumDelta += (delta - meanDelta) * (delta - meanDelta);
        }
        double stdDelta = Math.sqrt(sumDelta / (deltas.size()-1));
        scores.setVarXcorrCoelution(meanDelta + stdDelta); //时间偏差
        scores.setVarXcorrCoelutionWeighted(sumDeltaWeighted);
        scores.setVarXcorrShape(meanIntensity); // 平均的吻合程度--> 新的吻合系数
        scores.setVarXcorrShapeWeighted(sumIntensityWeighted);

        //logSnScore
        // log(mean of Apex sn s)
        double rt;
        int leftIndex, rightIndex;
        double snScore = 0.0d;
        if(signalToNoiseList.size() == 0){
            snScore = 0.0d;
        }
        for(int k = 0; k<signalToNoiseList.size();k++){
            rt = experimentFeatures.get(0).getRt(); //max peak rt
            BisectionLowHigh bisectionLowHigh = MathUtil.bisection(chromatograms.get(k), rt);
            leftIndex = bisectionLowHigh.getLow();
            rightIndex = bisectionLowHigh.getHigh();
            if(Math.abs(chromatograms.get(k).getRtArray()[leftIndex] - rt) < Math.abs(chromatograms.get(k).getRtArray()[rightIndex] - rt)){
                snScore += signalToNoiseList.get(k)[leftIndex];
            }else {
                snScore += signalToNoiseList.get(k)[rightIndex];
            }
        }
        snScore /= signalToNoiseList.size();
        if(snScore < 1){
            scores.setVarLogSnScore(0);
        }else {
            scores.setVarLogSnScore(Math.log(snScore));
        }
    }


    /**
     *
     * @param experimentFeatures
     * @param scores
     */
    public void calculateIntensityScore(List<ExperimentFeature> experimentFeatures, FeatureScores scores){
        double intensitySum = 0.0d;
        for(ExperimentFeature feature: experimentFeatures){
            intensitySum += feature.getIntensity();
        }
        double totalXic = experimentFeatures.get(0).getTotalXic();
        scores.setVarIntensityScore((intensitySum / totalXic));
    }

    /**
     * Get the XCorrMatrix with experiment Features
     * 对于一个 mrmFeature，算其中 chromatogramFeature 的 xcorrMatrix
     * @param experimentFeatures features in mrmFeature
     * HullInt: redistributed chromatogram in range of (peptideRef constant) leftRt and rightRt
     * @return Table<Integer, Integer, Float[]> xcorrMatrix
     */
    private Table<Integer, Integer, Double[]> initializeXCorrMatrix(List<ExperimentFeature> experimentFeatures){
        int listLength = experimentFeatures.size();
        Table<Integer, Integer, Double[]> xcorrMatrix = HashBasedTable.create();
        double[] intensityi, intensityj;
        for(int i=0; i<listLength;i++){
            for(int j=i; j<listLength;j++){
                intensityi = MathUtil.standardizeData(experimentFeatures.get(i).getHullInt());
                intensityj = MathUtil.standardizeData(experimentFeatures.get(j).getHullInt());
                xcorrMatrix.put(i,j,calculateCrossCorrelation(intensityi, intensityj));
            }
        }
        return xcorrMatrix;
    }

    /**
     * xcorrMatrix的意义：sum(反斜向的元素)/data.length(3)
     *       0   1   2
     *  0   |0  |1  |2
     *  1   |-1 |0  |1
     *  2   |-2 |-1 |0
     * @param data1 chromatogram feature
     * @param data2 the same length as data1
     * @return value of xcorrMatrix element
     */
    private Double[] calculateCrossCorrelation(double[] data1, double[] data2){
        int maxDelay = data1.length;
        Double[] output = new Double[maxDelay * 2 + 1];
        double sxy;
        int j;
        for(int delay = - maxDelay; delay <= maxDelay; delay ++){
            sxy = 0;
            for(int i = 0; i < maxDelay; i++){
                j = i + delay;
                if(j < 0 || j >= maxDelay){
                    continue;
                }
                sxy += (data1[i] * data2[j]);
            }
            output[delay + maxDelay] = sxy / maxDelay;
    }
        return output;
    }

}
