package com.westlake.air.propro.scorer;

import com.westlake.air.propro.constants.ScoreType;
import com.westlake.air.propro.domain.bean.score.FeatureScores;
import org.springframework.stereotype.Component;

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
//    public void calculateSwathLdaPrescore(FeatureScores scores) {
//        scores.put(ScoreType.MainScore,
//                scores.get(ScoreType.LibraryCorr) * 0.19011762 +
//                        scores.get(ScoreType.LibraryRsmd) * -2.47298914 +
//                        scores.get(ScoreType.NormRtScore) * -5.63906731 +
//                        scores.get(ScoreType.IsotopeCorrelationScore) * 0.62640133 +
//                        scores.get(ScoreType.IsotopeOverlapScore) * -0.36006925 +
//                        scores.get(ScoreType.MassdevScore) * -0.08814003 +
//                        scores.get(ScoreType.XcorrCoelution) * -0.13978311 +
//                        scores.get(ScoreType.XcorrShape) * 1.16475032 +
//                        scores.get(ScoreType.YseriesScore) * 0.19267813 +
//                        scores.get(ScoreType.LogSnScore) * 0.61712054);
//    }

    /**
     * -scores.calculate_swath_lda_prescore
     *
     * @param scores
     * @return
     */
    public void calculateSwathLdaPrescore(FeatureScores scores) {
        scores.put(ScoreType.MainScore,
                scores.get(ScoreType.LibraryCorr) * 0.2 +
                        scores.get(ScoreType.LibraryRsmd) * -2.5 +
                        scores.get(ScoreType.NormRtScore) * -5 +
                        scores.get(ScoreType.IsotopeCorrelationScore) * 0.5 +
                        scores.get(ScoreType.IsotopeOverlapScore) * -0.5 +
                        scores.get(ScoreType.XcorrShape) * 3 +
                        scores.get(ScoreType.XcorrShapeWeighted) * 3 +
                        scores.get(ScoreType.YseriesScore) * 0.2 +
                        scores.get(ScoreType.YseriesScore) * 0.2 +
                        scores.get(ScoreType.LogSnScore) * 0.5);
    }
}
