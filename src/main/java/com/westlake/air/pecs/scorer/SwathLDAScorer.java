package com.westlake.air.pecs.scorer;

import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import org.springframework.stereotype.Component;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-19 21:05
 */
@Component("swathLDAScorer")
public class SwathLDAScorer {

    /**
     * -scores.calculate_swath_lda_prescore
     *
     * @param scores
     * @return
     */
    public void calculateSwathLdaPrescore(FeatureScores scores) {
        scores.put(FeatureScores.ScoreType.MainScore,
                scores.get(FeatureScores.ScoreType.LibraryCorr) * 0.19011762 +
                        scores.get(FeatureScores.ScoreType.LibraryRsmd) * -2.47298914 +
                        scores.get(FeatureScores.ScoreType.NormRtScore) * -5.63906731 +
                        scores.get(FeatureScores.ScoreType.IsotopeCorrelationScore) * 0.62640133 +
                        scores.get(FeatureScores.ScoreType.IsotopeOverlapScore) * -0.36006925 +
                        scores.get(FeatureScores.ScoreType.MassdevScore) * -0.08814003 +
                        scores.get(FeatureScores.ScoreType.XcorrCoelution) * -0.13978311 +
                        scores.get(FeatureScores.ScoreType.XcorrShape) * 1.16475032 +
                        scores.get(FeatureScores.ScoreType.YseriesScore) * 0.19267813 +
                        scores.get(FeatureScores.ScoreType.LogSnScore) * 0.61712054);
    }
}
