package com.westlake.air.pecs.domain.bean.score;

import lombok.Data;

import java.util.HashMap;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-05 22:42
 */
@Data
public class FeatureScores {
    /**
     *       return scores.library_corr                     * -0.34664267 + 2
     *              scores.library_norm_manhattan           *  2.98700722 + 2
     *              scores.norm_rt_score                    *  7.05496384 + //0
     *              scores.xcorr_coelution_score            *  0.09445371 + 1
     *              scores.xcorr_shape_score                * -5.71823862 + 1
     *              scores.log_sn_score                     * -0.72989582 + 1
     *              scores.elution_model_fit_score          *  1.88443209; //0
     */
    public static final int SCORES_COUNT = 17;

    double varLibraryCorr;
    double varLibraryRsmd;

    double varXcorrCoelution;
    double varXcorrCoelutionWeighted;

    double varXcorrShape;
    double varXcorrShapeWeighted;

    double varNormRtScore;

    double varIntensityScore;

    double varLogSnScore;

    double varElutionModelFitScore;

    double varIsotopeCorrelationScore;
    double varIsotopeOverlapScore;
    double varMassdevScore;
    double varMassdevScoreWeighted;
    double varBseriesScore;
    double varYseriesScore;

    double mainVarXxSwathPrelimScore;

    public HashMap<String, Double> buildScoreMap(){
        HashMap<String, Double> map = new HashMap<>();
        map.put("varLibraryCorr",varLibraryCorr);
        map.put("varLibraryRsmd",varLibraryRsmd);
        map.put("varXcorrCoelution",varXcorrCoelution);
        map.put("varXcorrCoelutionWeighted",varXcorrCoelutionWeighted);
        map.put("varXcorrShape",varXcorrShape);
        map.put("varXcorrShapeWeighted",varXcorrShapeWeighted);
        map.put("varNormRtScore",varNormRtScore);
        map.put("varIntensityScore",varIntensityScore);
        map.put("varLogSnScore",varLogSnScore);
        map.put("varElutionModelFitScore",varElutionModelFitScore);
        map.put("varIsotopeCorrelationScore",varIsotopeCorrelationScore);
        map.put("varIsotopeOverlapScore",varIsotopeOverlapScore);
        map.put("varMassdevScore",varMassdevScore);
        map.put("varMassdevScoreWeighted",varMassdevScoreWeighted);
        map.put("varBseriesScore",varBseriesScore);
        map.put("varYseriesScore",varYseriesScore);
        map.put("mainVarXxSwathPrelimScore",mainVarXxSwathPrelimScore);

        return map;
    }
}
