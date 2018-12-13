package com.westlake.air.pecs.rtnormalizer;

import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.pecs.domain.bean.score.*;
import com.westlake.air.pecs.scorer.ChromatographicScorer;
import com.westlake.air.pecs.scorer.LibraryScorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-05 17:18
 */
@Component("rtNormalizerScorer")
public class RtNormalizerScorer {

    @Autowired
    ChromatographicScorer chromatographicScorer;
    @Autowired
    LibraryScorer libraryScorer;


//    private float windowLength = 1000;
//    private int binCount = 30;

//    private float rtNormalizationFactor = 1.0f;
//    private int addUpSpectra = 1;
//    private float spacingForSpectraResampling = 0.005f;
//    //score use params


    /**
     * return scores.library_corr                     * -0.34664267 +
     * scores.library_norm_manhattan           *  2.98700722 +
     * scores.norm_rt_score                    *  7.05496384 +
     * scores.xcorr_coelution_score            *  0.09445371 +
     * scores.xcorr_shape_score                * -5.71823862 +
     * scores.log_sn_score                     * -0.72989582 +
     * scores.elution_model_fit_score          *  1.88443209;
     *
     * @param chromatograms      chromatogramList in transitionGroup
     * @param experimentFeatures features extracted from chromatogramList in transitionGroup
     * @param libraryIntensity   intensity in transitionList in transitionGroup
     * @return List of overallQuality
     */
    public List<ScoreRtPair> score(List<RtIntensityPairsDouble> chromatograms, List<List<ExperimentFeature>> experimentFeatures, List<Double> libraryIntensity, List<double[]> noise1000List, SlopeIntercept slopeIntercept, double groupRt) {


        List<ScoreRtPair> finalScores = new ArrayList<>();
        for (List<ExperimentFeature> features : experimentFeatures) {
            FeatureScores scores = new FeatureScores();
            chromatographicScorer.calculateChromatographicScores(features, libraryIntensity, scores, null);
            chromatographicScorer.calculateLogSnScore(chromatograms, features, noise1000List, scores);
            libraryScorer.calculateLibraryScores(features, libraryIntensity, scores, null);

            double ldaScore = -1d * calculateLdaPrescore(scores);
            ScoreRtPair scoreRtPair = new ScoreRtPair();
            scoreRtPair.setGroupRt(groupRt);
            scoreRtPair.setRt(features.get(0).getRt());
            scoreRtPair.setScore(ldaScore);
            scoreRtPair.setScores(scores);
            finalScores.add(scoreRtPair);
        }

        return finalScores;
    }


    /**
     * The score that is really matter to final pairs selection.
     *
     * @param scores pre-calculated
     * @return final score
     */
    private double calculateLdaPrescore(FeatureScores scores) {
        return scores.get(FeatureScores.ScoreType.LibraryCorr) * -0.34664267d +
                scores.get(FeatureScores.ScoreType.LibraryRsmd) * 2.98700722d +
                scores.get(FeatureScores.ScoreType.XcorrCoelution) * 0.09445371d +
                scores.get(FeatureScores.ScoreType.XcorrShape) * -5.71823862d +
                scores.get(FeatureScores.ScoreType.LogSnScore) * -0.72989582d;
    }


}
