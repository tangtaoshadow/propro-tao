package com.westlake.air.pecs.domain.bean.score;

import lombok.Data;

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
     *
     *
     *
     * scores.xcorr_coelution_score 互相关偏移的mean + std
     * scores.weighted_coelution_score 带权重的相关偏移sum
     * scores.xcorr_shape_score 互相关序列最大值的平均值
     * scores.weighted_xcorr_shape 带权重的互相关序列最大值的平均值
     * scores.log_sn_score log(距离ApexRt最近点的stn值之和)
     *
     * scores.var_intensity_score 同一个peptideRef下, 所有HullPoints的intensity之和 除以 所有intensity之和
     *
     * scores.library_corr //对experiment和library intensity算Pearson相关系数
     * scores.library_norm_manhattan // 对experiment intensity 算平均占比差距
     *
     * scores.massdev_score 按spectrum intensity加权的mz与product mz的偏差ppm百分比之和
     * scores.weighted_massdev_score 按spectrum intensity加权的mz与product mz的偏差ppm百分比按libraryIntensity加权之和
     */

    //对experiment和library intensity算Pearson相关系数
    double varLibraryCorr;

    //对experiment intensity 算占比差距平均值
    double varLibraryRsmd;

    //互相关偏移的mean + std
    double varXcorrCoelution;

    //带权重的相关偏移sum
    double varXcorrCoelutionWeighted;

    //互相关序列最大值的平均值
    double varXcorrShape;

    //带权重的互相关序列最大值的平均值
    double varXcorrShapeWeighted;

    //normalizedExperimentalRt与groupRt之差
    double varNormRtScore;

    //同一个peptideRef下, 所有HullPoints的intensity之和 除以 所有intensity之和
    double varIntensityScore;

    //log(距离ApexRt最近点的stn值之和)
    double varLogSnScore;

    //
    double varElutionModelFitScore;

    //
    double varIsotopeCorrelationScore;

    //feature intensity加权的可能（带电量1-4）无法区分同位素峰值的平均发生次数之和
    double varIsotopeOverlapScore;

    //按spectrum intensity加权的mz与product mz的偏差ppm百分比之和
    double varMassdevScore;
    //按spectrum intensity加权的mz与product mz的偏差ppm百分比按libraryIntensity加权之和
    double varMassdevScoreWeighted;

    //peptideRt对应的spectrumArray中，检测到的b离子的数量
    double varBseriesScore;
    //peptideRt对应的spectrumArray中，检测到的y离子的数量
    double varYseriesScore;

    //Swath主打分
    double mainVarXxSwathPrelimScore;
}
