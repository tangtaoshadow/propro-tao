package com.westlake.air.pecs.domain.bean;

import lombok.Data;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-05 22:42
 */
@Data
public class RTNormalizationScores {
    /**
     *       return scores.library_corr                     * -0.34664267 + 2
     *              scores.library_norm_manhattan           *  2.98700722 + 2
     *              scores.norm_rt_score                    *  7.05496384 + //0
     *              scores.xcorr_coelution_score            *  0.09445371 + 1
     *              scores.xcorr_shape_score                * -5.71823862 + 1
     *              scores.log_sn_score                     * -0.72989582 + 1
     *              scores.elution_model_fit_score          *  1.88443209; //0
     */
    float libraryCorr;
    float libraryNormManhattan;
    float xcorrCoelutionScore;
    float xcorrShapeScore;
    float logSnScore;

}
