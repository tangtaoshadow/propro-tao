package com.westlake.air.pecs.domain.bean.score;

import lombok.Data;

import java.util.HashMap;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-05 22:42
 */
@Data
public class FeatureScores {

    static String[] columns;
    /**
     * return scores.library_corr                     * -0.34664267 + 2
     * scores.library_norm_manhattan           *  2.98700722 + 2
     * scores.norm_rt_score                    *  7.05496384 + //0
     * scores.xcorr_coelution_score            *  0.09445371 + 1
     * scores.xcorr_shape_score                * -5.71823862 + 1
     * scores.log_sn_score                     * -0.72989582 + 1
     * scores.elution_model_fit_score          *  1.88443209; //0
     * <p>
     * <p>
     * <p>
     * scores.xcorr_coelution_score 互相关偏移的mean + std
     * scores.weighted_coelution_score 带权重的相关偏移sum
     * scores.xcorr_shape_score 互相关序列最大值的平均值
     * scores.weighted_xcorr_shape 带权重的互相关序列最大值的平均值
     * scores.log_sn_score log(距离ApexRt最近点的stn值之和)
     * <p>
     * scores.var_intensity_score 同一个peptideRef下, 所有HullPoints的intensity之和 除以 所有intensity之和
     * <p>
     * scores.library_corr //对experiment和library intensity算Pearson相关系数
     * scores.library_norm_manhattan // 对experiment intensity 算平均占比差距
     * <p>
     * scores.massdev_score 按spectrum intensity加权的mz与product mz的偏差ppm百分比之和
     * scores.weighted_massdev_score 按spectrum intensity加权的mz与product mz的偏差ppm百分比按libraryIntensity加权之和
     */
    public static final int SCORES_COUNT = 17;

    //对experiment和library intensity算Pearson相关系数
    double varLibraryCorr;

    //对experiment intensity 算占比差距平均值
    double varLibraryRsmd;

    //互相关偏移的mean + std
    double varXcorrCoelution;

    //互相关序列最大值的平均值
    double varXcorrShape;

    //log(距离ApexRt最近点的stn值之和)
    double varLogSnScore;

    //同一个peptideRef下, 所有HullPoints的intensity之和 除以 所有intensity之和
    double varIntensityScore;

    //带权重的相关偏移sum
    double varXcorrCoelutionWeighted;

    //带权重的互相关序列最大值的平均值
    double varXcorrShapeWeighted;

    //normalizedExperimentalRt与groupRt之差
    double varNormRtScore;

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

    public static final String MainVarXxSwathPrelimScore = "mainVarXxSwathPrelimScore";
    public static final String VarLibraryCorr = "varLibraryCorr";
    public static final String VarLibraryRsmd = "varLibraryRsmd";
    public static final String VarXcorrCoelution = "varXcorrCoelution";
    public static final String VarXcorrCoelutionWeighted = "varXcorrCoelutionWeighted";
    public static final String VarXcorrShape = "varXcorrShape";
    public static final String VarXcorrShapeWeighted = "varXcorrShapeWeighted";
    public static final String VarNormRtScore = "varNormRtScore";
    public static final String VarIntensityScore = "varIntensityScore";
    public static final String VarLogSnScore = "varLogSnScore";
    public static final String VarElutionModelFitScore = "varElutionModelFitScore";
    public static final String VarIsotopeCorrelationScore = "varIsotopeCorrelationScore";
    public static final String VarIsotopeOverlapScore = "varIsotopeOverlapScore";
    public static final String VarMassdevScore = "varMassdevScore";
    public static final String VarMassdevScoreWeighted = "varMassdevScoreWeighted";
    public static final String VarBseriesScore = "varBseriesScore";
    public static final String VarYseriesScore = "varYseriesScore";

    public HashMap<String, Double> buildScoreMap() {
        HashMap<String, Double> map = new HashMap<>();
        map.put(MainVarXxSwathPrelimScore, mainVarXxSwathPrelimScore);
        map.put(VarLibraryCorr, varLibraryCorr);
        map.put(VarLibraryRsmd, varLibraryRsmd);
        map.put(VarXcorrCoelution, varXcorrCoelution);
        map.put(VarXcorrCoelutionWeighted, varXcorrCoelutionWeighted);
        map.put(VarXcorrShape, varXcorrShape);
        map.put(VarXcorrShapeWeighted, varXcorrShapeWeighted);
        map.put(VarNormRtScore, varNormRtScore);
        map.put(VarIntensityScore, varIntensityScore);
        map.put(VarLogSnScore, varLogSnScore);
        map.put(VarElutionModelFitScore, varElutionModelFitScore);
        map.put(VarIsotopeCorrelationScore, varIsotopeCorrelationScore);
        map.put(VarIsotopeOverlapScore, varIsotopeOverlapScore);
        map.put(VarMassdevScore, varMassdevScore);
        map.put(VarMassdevScoreWeighted, varMassdevScoreWeighted);
        map.put(VarBseriesScore, varBseriesScore);
        map.put(VarYseriesScore, varYseriesScore);

        return map;
    }

    public static String[] getScoresColumns() {

        if (columns == null) {
            columns = new String[SCORES_COUNT];
            columns[0] = MainVarXxSwathPrelimScore;
            columns[1] = VarLibraryCorr;
            columns[2] = VarLibraryRsmd;
            columns[3] = VarXcorrCoelution;
            columns[4] = VarXcorrCoelutionWeighted;
            columns[5] = VarXcorrShape;
            columns[6] = VarXcorrShapeWeighted;
            columns[7] = VarNormRtScore;
            columns[8] = VarIntensityScore;
            columns[9] = VarLogSnScore;
            columns[10] = VarElutionModelFitScore;
            columns[11] = VarIsotopeCorrelationScore;
            columns[12] = VarIsotopeOverlapScore;
            columns[13] = VarMassdevScore;
            columns[14] = VarMassdevScoreWeighted;
            columns[15] = VarBseriesScore;
            columns[16] = VarYseriesScore;
        }

        return columns;
    }

    public static void fillScores(HashMap<String, Double> scoreMap, Double[] scoreArray) {
        scoreArray[0] = scoreMap.get(MainVarXxSwathPrelimScore);
        scoreArray[1] = scoreMap.get(VarLibraryCorr);
        scoreArray[2] = scoreMap.get(VarLibraryRsmd);
        scoreArray[3] = scoreMap.get(VarXcorrCoelution);
        scoreArray[4] = scoreMap.get(VarXcorrCoelutionWeighted);
        scoreArray[5] = scoreMap.get(VarXcorrShape);
        scoreArray[6] = scoreMap.get(VarXcorrShapeWeighted);
        scoreArray[7] = scoreMap.get(VarNormRtScore);
        scoreArray[8] = scoreMap.get(VarIntensityScore);
        scoreArray[9] = scoreMap.get(VarLogSnScore);
        scoreArray[10] = scoreMap.get(VarElutionModelFitScore);
        scoreArray[11] = scoreMap.get(VarIsotopeCorrelationScore);
        scoreArray[12] = scoreMap.get(VarIsotopeOverlapScore);
        scoreArray[13] = scoreMap.get(VarMassdevScore);
        scoreArray[14] = scoreMap.get(VarMassdevScoreWeighted);
        scoreArray[15] = scoreMap.get(VarBseriesScore);
        scoreArray[16] = scoreMap.get(VarYseriesScore);
    }
}
