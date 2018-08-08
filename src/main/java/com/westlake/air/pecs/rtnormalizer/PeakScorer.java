package com.westlake.air.pecs.rtnormalizer;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.westlake.air.pecs.domain.bean.RTNormalizationScores;
import com.westlake.air.pecs.domain.bean.RtIntensityPairs;
import com.westlake.air.pecs.domain.bean.ScoreRtPair;
import com.westlake.air.pecs.domain.bean.ExperimentFeature;
import com.westlake.air.pecs.utils.MathUtil;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-05 17:18
 */
@Component("peakScorer")
public class PeakScorer {

//    private float windowLength = 1000;
//    private int binCount = 30;

//    private float rtNormalizationFactor = 1.0f;
//    private int addUpSpectra = 1;
//    private float spacingForSpectraResampling = 0.005f;
//    //score use params


    /**
     *        return scores.library_corr                     * -0.34664267 +
     *               scores.library_norm_manhattan           *  2.98700722 +
     *               scores.norm_rt_score                    *  7.05496384 +
     *               scores.xcorr_coelution_score            *  0.09445371 +
     *               scores.xcorr_shape_score                * -5.71823862 +
     *               scores.log_sn_score                     * -0.72989582 +
     *               scores.elution_model_fit_score          *  1.88443209;
     * @param chromatograms chromatogramList in transitionGroup
     * @param experimentFeatures features extracted from chromatogramList in transitionGroup
     * @param libraryIntensity intensity in transitionList in transitionGroup
     * @return List of overallQuality
     */
    public List<ScoreRtPair> score(List<RtIntensityPairs> chromatograms, List<List<ExperimentFeature>> experimentFeatures, List<Float> libraryIntensity, float windowLength, int binCount){

        //get signal to noise list
        List<float[]> signalToNoiseList = new ArrayList<>();
        for(RtIntensityPairs chromatogram: chromatograms) {
            float[] signalToNoise = new SignalToNoiseEstimator().computeSTN(chromatogram, windowLength, binCount);
            signalToNoiseList.add(signalToNoise);
        }

        List<ScoreRtPair> finalScores = new ArrayList<>();
        for(List<ExperimentFeature> features: experimentFeatures){
            RTNormalizationScores scores = new RTNormalizationScores();
            calculateChromatographicScores(chromatograms, features, signalToNoiseList, scores);
            calculateLibraryScores(features,libraryIntensity, scores);
            float ldaScore = calculateLdaPrescore(scores);
            ScoreRtPair scoreRtPair = new ScoreRtPair();
            scoreRtPair.setRt(features.get(0).getRt());
            scoreRtPair.setScore(ldaScore);
            finalScores.add(scoreRtPair);
        }

        return finalScores;
    }




    /**
     * scores.xcorr_coelution_score
     * scores.xcorr_shape_score
     * scores.log_sn_score
     * @param chromatograms chromatogram list of transition group
     * @param experimentFeatures list of features in selected mrmfeature
     * @param signalToNoiseList signal to noise list of chromatogram list
     */
    private void calculateChromatographicScores(List<RtIntensityPairs> chromatograms, List<ExperimentFeature> experimentFeatures, List<float[]> signalToNoiseList, RTNormalizationScores scores){
        Table<Integer, Integer, Float[]> xcorrMatrix = initializeXCorrMatrix(experimentFeatures);

        //xcorrCoelutionScore
        //xcorr_shape_score
        List<Integer> deltas = new ArrayList<>();
        List<Float> intensities = new ArrayList<>();
        Float[] value;
        int max;
        for(int i = 0; i<experimentFeatures.size(); i++){
            for (int j = i; j<experimentFeatures.size(); j++){
                value = xcorrMatrix.get(i, j);
                max = MathUtil.findMaxIndex(value);
                deltas.add(max - (value.length -1)/2);
                intensities.add(value[max]);
            }
        }
        float sumDelta = 0.0f, sumIntensity = 0.0f;
        for(int i=0; i<deltas.size(); i++){
            sumDelta += deltas.get(i);
            sumIntensity += intensities.get(i);
        }
        float meanDelta = sumDelta / deltas.size();
        float meanIntensity = sumIntensity / intensities.size();
        sumDelta = 0;
        for(int delta: deltas){
            sumDelta += (delta - meanDelta) * (delta - meanDelta);
        }
        float stdDelta = (float) Math.sqrt(sumDelta / (deltas.size()-1));
        scores.setXcorrCoelutionScore(meanDelta + stdDelta);
        scores.setXcorrShapeScore(meanIntensity);

        //logSnScore
        float rt;
        int leftIndex, rightIndex;
        float snScore = 0.0f;
        if(signalToNoiseList.size() == 0){
            snScore = 0.0f;
        }
        for(int k = 0; k<signalToNoiseList.size();k++){
            rt = experimentFeatures.get(0).getRt();
            leftIndex = MathUtil.bisection(chromatograms.get(k), rt);
            rightIndex = leftIndex + 1;
            if(Math.abs(chromatograms.get(k).getRtArray()[leftIndex] - rt) < Math.abs(chromatograms.get(k).getRtArray()[rightIndex] - rt)){
                snScore += signalToNoiseList.get(k)[leftIndex];
            }else {
                snScore += signalToNoiseList.get(k)[rightIndex];
            }
        }
        snScore /= signalToNoiseList.size();
        if(snScore < 1){
            scores.setLogSnScore(0);
        }else {
            scores.setLogSnScore((float)Math.log(snScore));
        }
    }




    /**
     * scores.library_corr
     * scores.library_norm_manhattan
     * @param experimentFeatures get experimentIntensity: from features extracted
     * @param libraryIntensity get libraryIntensity: from transitions
     * @param scores library_corr, library_norm_manhattan
     */
    private void calculateLibraryScores(List<ExperimentFeature> experimentFeatures, List<Float> libraryIntensity, RTNormalizationScores scores){
        List<Float> experimentIntensity = new ArrayList<>();
        for(ExperimentFeature experimentFeature: experimentFeatures){
            experimentIntensity.add(experimentFeature.getIntensity());
        }
        // experimentIntensity, libraryIntensity same size
        //library_norm_manhattan
        float sum = 0.0f;
        float[] x = normalizeSum(libraryIntensity);
        float[] y = normalizeSum(experimentIntensity);
        for(int i=0; i<x.length; i++){
            sum += Math.abs(x[i] - y[i]);
        }
        scores.setLibraryNormManhattan(sum / x.length);

        //library_corr
        float corr = 0.0f, m1 = 0.0f, m2 = 0.0f, s1 = 0.0f, s2 = 0.0f;
        for(int i=0;i<libraryIntensity.size(); i++){
            corr += experimentIntensity.get(i) * libraryIntensity.get(i);
            m1 += experimentIntensity.get(i);
            m2 += libraryIntensity.get(i);
            s1 += experimentIntensity.get(i) * experimentIntensity.get(i);
            s2 += libraryIntensity.get(i) * libraryIntensity.get(i);
        }
        m1 /= libraryIntensity.size();
        m2 /= libraryIntensity.size();
        s1 -= m1 * m1 * libraryIntensity.size();
        s2 -= m2 * m2 * libraryIntensity.size();
        if(s1 < Math.pow(1,-12) || s2 < Math.pow(1,-12)){
            scores.setLibraryCorr(0.0f);
        }else {
            corr -= m1 * m2 * libraryIntensity.size();
            corr /= Math.sqrt(s1 * s2);
            scores.setLibraryCorr(corr);
        }
    }

    /**
     * Get the XCorrMatrix with experiment Features
     * @param experimentFeatures features in mrmFeature
     * @return Table<Integer, Integer, Float[]> xcorrMatrix
     */
    private Table<Integer, Integer, Float[]> initializeXCorrMatrix(List<ExperimentFeature> experimentFeatures){
        int listLength = experimentFeatures.size();
        Table<Integer, Integer, Float[]> xcorrMatrix = HashBasedTable.create();
        float[] intensityi, intensityj;
        for(int i=0; i<listLength;i++){
            for(int j=i; j<listLength;j++){
                intensityi = MathUtil.standardizeData(experimentFeatures.get(i).getHullInt());
                intensityj = MathUtil.standardizeData(experimentFeatures.get(j).getHullInt());
                xcorrMatrix.put(i,j,calculateCrossCorrelation(intensityi, intensityj));
            }
        }
        return xcorrMatrix;
    }

    private Float[] calculateCrossCorrelation(float[] data1, float[] data2){
        int maxDelay = data1.length;
        Float[] output = new Float[maxDelay * 2 + 1];
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
            output[delay + maxDelay] = (float) sxy / maxDelay;
        }
        return output;
    }

    private float[] normalizeSum(List libraryIntensity){
        float[] normalizedLibraryIntensity = new float[libraryIntensity.size()];
        float sum = 0f;
        for(Object intensity: libraryIntensity){
            sum += (float)intensity;
        }

        if(sum == 0f){
            sum += 0.000001;
        }

        for(int i = 0; i<libraryIntensity.size(); i++){
            normalizedLibraryIntensity[i] = (float)libraryIntensity.get(i) / sum;
        }
        return normalizedLibraryIntensity;
    }
    /**
     * The score that is really matter to final pairs selection.
     * @param scores pre-calculated
     * @return final score
     */
    private float calculateLdaPrescore(RTNormalizationScores scores){
        return  scores.getLibraryCorr()                     * -0.34664267f +
                scores.getLibraryNormManhattan()            *  2.98700722f +
                scores.getXcorrCoelutionScore()             *  0.09445371f +
                scores.getXcorrShapeScore()                 * -5.71823862f +
                scores.getLogSnScore()                      * -0.72989582f;
    }

}
