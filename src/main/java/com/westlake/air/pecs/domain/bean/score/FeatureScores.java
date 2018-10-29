package com.westlake.air.pecs.domain.bean.score;

import com.westlake.air.pecs.constants.Constants;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
     * scores.library_norm_manhattan //对experiment intensity 算平均占比差距
     * <p>
     * scores.massdev_score 按spectrum intensity加权的mz与product mz的偏差ppm百分比之和
     * scores.weighted_massdev_score 按spectrum intensity加权的mz与product mz的偏差ppm百分比按libraryIntensity加权之和
     */
    public static final int SCORES_COUNT = ScoreType.getUsedTypes().size();
//    public static final int SCORES_COUNT = 17;

    double rt;

    //Swath主打分
    double mainVarXxSwathPrelimScore;

    //对experiment和library intensity算Pearson相关系数
    double varLibraryCorr;

    //对experiment intensity 算占比差距平均值
    double varLibraryRsmd;

    //
    double varLibraryDotprod;

    //
    double varLibraryManhattan;

    //
    double varLibrarySangle;

    //
    double varLibraryRootmeansquare;

    //
    double varManhattScore;

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

    public static final String VarLibraryDotprod = "varLibraryDotprod";
    public static final String VarLibraryManhattan = "varLibraryManhattan";
    public static final String VarLibrarySangle = "varLibrarySangle";
    public static final String VarLibraryRootmeansquare = "varLibraryRootmeansquare";
    public static final String VarManhattScore = "varManhattScore";

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

        map.put(VarLibraryDotprod, varLibraryDotprod);
        map.put(VarLibraryManhattan, varLibraryManhattan);
        map.put(VarLibrarySangle, varLibrarySangle);
        map.put(VarLibraryRootmeansquare, varLibraryRootmeansquare);
        map.put(VarManhattScore, varManhattScore);

        return map;
    }

    public static String[] getScoresColumns() {
        if (columns == null) {
            columns = new String[SCORES_COUNT];
            List<ScoreType> scoreTypes = ScoreType.getUsedTypes();
            for (int i = 0; i < SCORES_COUNT; i++) {
                columns[i] = scoreTypes.get(i).getTypeName();
            }
        }

        return columns;
    }

    public static Double[] toArray(HashMap<String, Double> scoreMap) {
        Double[] scoreArray = new Double[SCORES_COUNT];
        List<ScoreType> scoreTypes = ScoreType.getUsedTypes();
        for (int i = 0; i < SCORES_COUNT; i++) {
            scoreArray[i] = scoreMap.get(scoreTypes.get(i).getTypeName());
        }

        return scoreArray;
    }

    public static FeatureScores toFeaturesScores(Double[] scores) {
        if (scores == null || scores.length != 17) {
            return null;
        }
        FeatureScores featureScores = new FeatureScores();
        featureScores.setMainVarXxSwathPrelimScore(scores[0]);
        featureScores.setVarBseriesScore(scores[1]);
        featureScores.setVarElutionModelFitScore(scores[2]);
        featureScores.setVarIntensityScore(scores[3]);
        featureScores.setVarIsotopeCorrelationScore(scores[4]);
        featureScores.setVarIsotopeOverlapScore(scores[5]);
        featureScores.setVarLibraryCorr(scores[6]);
        featureScores.setVarLibraryRsmd(scores[7]);
        featureScores.setVarLogSnScore(scores[8]);
        featureScores.setVarMassdevScore(scores[9]);
        featureScores.setVarMassdevScoreWeighted(scores[10]);
        featureScores.setVarNormRtScore(scores[11]);
        featureScores.setVarXcorrCoelution(scores[12]);
        featureScores.setVarXcorrCoelutionWeighted(scores[13]);
        featureScores.setVarXcorrShape(scores[14]);
        featureScores.setVarXcorrShapeWeighted(scores[15]);
        featureScores.setVarYseriesScore(scores[16]);
        return featureScores;
    }

    public enum ScoreType {
        MainVarXxSwathPrelimScore("mainVarXxSwathPrelimScore", "main_VarXxSwathPrelimScore", true),
        VarBseriesScore("varBseriesScore", "var_BseriesScore", false),
        VarElutionModelFitScore("varElutionModelFitScore", "var_ElutionModelFitScore", false),
        VarIntensityScore("varIntensityScore", "var_IntensityScore", true),
        VarIsotopeCorrelationScore("varIsotopeCorrelationScore", "var_IsotopeCorrelationScore", false),
        VarIsotopeOverlapScore("varIsotopeOverlapScore", "var_IsotopeOverlapScore", false),
        VarLibraryCorr("varLibraryCorr", "var_LibraryCorr", true),
        VarLibraryRsmd("varLibraryRsmd", "var_LibraryRsmd", true),
        VarLogSnScore("varLogSnScore", "var_LogSnScore", true),
        VarMassdevScore("varMassdevScore", "var_MassdevScore", false),
        VarMassdevScoreWeighted("varMassdevScoreWeighted", "var_MassdevScoreWeighted", false),
        VarNormRtScore("varNormRtScore", "var_NormRtScore", true),
        VarXcorrCoelution("varXcorrCoelution", "var_XcorrCoelution", true),
        VarXcorrCoelutionWeighted("varXcorrCoelutionWeighted", "var_XcorrCoelutionWeighted", true),
        VarXcorrShape("varXcorrShape", "var_XcorrShape", true),
        VarXcorrShapeWeighted("varXcorrShapeWeighted", "var_XcorrShapeWeighted", true),

        VarLibraryDotprod("varLibraryDotprod", "var_LibraryDotprod", false),
        VarLibraryManhattan("varLibraryManhattan", "var_LibraryManhattan", false),
        VarLibrarySangle("varLibrarySangle", "var_LibrarySangle", false),
        VarLibraryRootmeansquare("varLibraryRootmeansquare", "var_LibraryRootmeansquare", false),
        VarManhattScore("varManhattScore", "var_ManhattScore", false),
        VarYseriesScore("varYseriesScore", "var_YseriesScore", false),
        ;

        String typeName;

        String pyProphetName;

        boolean isUsed;

        ScoreType(String typeName, String pyProphetName, Boolean isUsed) {
            this.typeName = typeName;
            this.pyProphetName = pyProphetName;
            this.isUsed = isUsed;
        }

        public String getTypeName() {
            return typeName;
        }

        public String getPyProphetName() {
            return pyProphetName;
        }

        public boolean isUsed() {
            return isUsed;
        }

        public static List<ScoreType> getUsedTypes() {
            List<ScoreType> types = new ArrayList<>();
            for (ScoreType type : values()) {
                if (type.isUsed) {
                    types.add(type);
                }
            }
            return types;
        }

        public static List<ScoreType> getUnusedTypes() {
            List<ScoreType> types = new ArrayList<>();
            for (ScoreType type : values()) {
                if (!type.isUsed) {
                    types.add(type);
                }
            }
            return types;
        }

        public static String getPyProphetScoresColumns() {
            StringBuilder columns = new StringBuilder();
            List<ScoreType> scoreTypes = ScoreType.getUsedTypes();
            for (int i = 0; i < scoreTypes.size(); i++) {
                if (i != scoreTypes.size() - 1) {
                    columns.append(scoreTypes.get(i).getPyProphetName()).append(Constants.TAB);
                } else {
                    columns.append(scoreTypes.get(i).getPyProphetName()).append(Constants.CHANGE_LINE);
                }
            }
            return columns.toString();
        }
    }
}
