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
        scores.put(FeatureScores.ScoreType.MainVarXxSwathPrelimScore,
                scores.get(FeatureScores.ScoreType.VarLibraryCorr) * 0.19011762 +
                        scores.get(FeatureScores.ScoreType.VarLibraryRsmd) * -2.47298914 +
                        scores.get(FeatureScores.ScoreType.VarNormRtScore) * -5.63906731 +
                        scores.get(FeatureScores.ScoreType.VarIsotopeCorrelationScore) * 0.62640133 +
                        scores.get(FeatureScores.ScoreType.VarIsotopeOverlapScore) * -0.36006925 +
                        scores.get(FeatureScores.ScoreType.VarMassdevScore) * -0.08814003 +
                        scores.get(FeatureScores.ScoreType.VarXcorrCoelution) * -0.13978311 +
                        scores.get(FeatureScores.ScoreType.VarXcorrShape) * 1.16475032 +
                        scores.get(FeatureScores.ScoreType.VarYseriesScore) * 0.19267813 +
                        scores.get(FeatureScores.ScoreType.VarLogSnScore) * 0.61712054);
    }
}
