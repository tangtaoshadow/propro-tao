package com.westlake.air.pecs.rtnormalizer;

import com.westlake.air.pecs.domain.bean.score.*;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairs;
import com.westlake.air.pecs.feature.SignalToNoiseEstimator;
import com.westlake.air.pecs.scorer.ChromatograpicScorer;
import com.westlake.air.pecs.scorer.LibraryScorer;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-05 17:18
 */
@Component("peakScorer")
public class RTNormalizerScorer {

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
    public List<ScoreRtPair> score(List<RtIntensityPairs> chromatograms, List<List<ExperimentFeature>> experimentFeatures, List<Float> libraryIntensity, SlopeIntercept slopeIntercept, float groupRt, float windowLength, int binCount){

        //get signal to noise list
        List<float[]> signalToNoiseList = new ArrayList<>();
        for(RtIntensityPairs chromatogram: chromatograms) {
            float[] signalToNoise = new SignalToNoiseEstimator().computeSTN(chromatogram, windowLength, binCount);
            signalToNoiseList.add(signalToNoise);
        }

        List<ScoreRtPair> finalScores = new ArrayList<>();
        for(List<ExperimentFeature> features: experimentFeatures){
            PecsScores scores = new PecsScores();
            new ChromatograpicScorer().calculateChromatographicScores(chromatograms, features, libraryIntensity, signalToNoiseList, scores);
            new LibraryScorer().calculateLibraryScores(features,libraryIntensity, scores, slopeIntercept, groupRt);
            float ldaScore = calculateLdaPrescore(scores);
            ScoreRtPair scoreRtPair = new ScoreRtPair();
            scoreRtPair.setRt(features.get(0).getRt());
            scoreRtPair.setScore(ldaScore);
            finalScores.add(scoreRtPair);
        }

        return finalScores;
    }


    /**
     * The score that is really matter to final pairs selection.
     * @param scores pre-calculated
     * @return final score
     */
    private float calculateLdaPrescore(PecsScores scores){
        return  scores.getVarLibraryCorr()              * -0.34664267f +
                scores.getVarLibraryRsmd()              *  2.98700722f +
                scores.getVarXcorrCoelution()           *  0.09445371f +
                scores.getVarXcorrShape()               * -5.71823862f +
                scores.getVarLogSnScore()               * -0.72989582f;
    }


}
