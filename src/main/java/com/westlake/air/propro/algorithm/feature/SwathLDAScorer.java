package com.westlake.air.propro.algorithm.feature;

import com.westlake.air.propro.constants.ScoreType;
import com.westlake.air.propro.domain.bean.score.FeatureScores;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-19 21:05
 */
@Component("swathLDAScorer")
public class SwathLDAScorer {

//    /**
//     * -scores.calculate_swath_lda_prescore
//     *
//     * @param scores
//     * @return
//     */
    public void calculateSwathLdaPrescore(FeatureScores scores, List<String> scoreTypes) {
        scores.put(ScoreType.MainScore.getTypeName(),
                scores.get(ScoreType.LibraryCorr.getTypeName(), scoreTypes) * 0.19011762 +
                        scores.get(ScoreType.LibraryRsmd.getTypeName(), scoreTypes) * -2.47298914 +
                        scores.get(ScoreType.NormRtScore.getTypeName(), scoreTypes) * -5.63906731 +
                        scores.get(ScoreType.IsotopeCorrelationScore.getTypeName(), scoreTypes) * 0.62640133 +
                        scores.get(ScoreType.IsotopeOverlapScore.getTypeName(), scoreTypes) * -0.36006925 +
                        scores.get(ScoreType.MassdevScore.getTypeName(), scoreTypes) * -0.08814003 +
                        scores.get(ScoreType.XcorrCoelution.getTypeName(), scoreTypes) * -0.13978311 +
                        scores.get(ScoreType.XcorrShape.getTypeName(), scoreTypes) * 1.16475032 +
                        scores.get(ScoreType.YseriesScore.getTypeName(), scoreTypes) * 0.19267813 +
                        scores.get(ScoreType.LogSnScore.getTypeName(), scoreTypes) * 0.61712054, scoreTypes);
    }
}
