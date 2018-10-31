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

    double rt;

    HashMap<String, Double> scoresMap;

    public static String[] getScoresColumnNames() {
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

    public void put(String typeName, Double score) {
        if (scoresMap == null) {
            scoresMap = new HashMap<>();
        }
        scoresMap.put(typeName, score);
    }

    public void put(ScoreType type, Double score) {
        if (scoresMap == null) {
            scoresMap = new HashMap<>();
        }
        scoresMap.put(type.getTypeName(), score);
    }

    public Double get(ScoreType type) {
        if (scoresMap == null) {
            return null;
        } else {
            Double d = scoresMap.get(type.getTypeName());
            return d == null ? 0d : d;
        }
    }

    public Double get(String typeName) {
        if (scoresMap == null) {
            return null;
        } else {
            Double d = scoresMap.get(typeName);
            return d == null ? 0d : d;
        }
    }

    public enum ScoreType {

        MainScore("MainScore", "main_VarXxSwathPrelimScore",
                "Swath主打分",
                true, true),
        BseriesScore("BseriesScore", "var_BseriesScore",
                "peptideRt对应的spectrumArray中，检测到的b离子的数量",
                null, false),
        ElutionModelFitScore("ElutionModelFitScore", "var_ElutionModelFitScore",
                "",
                null, false),
        IntensityScore("IntensityScore", "var_IntensityScore",
                "同一个peptideRef下, 所有HullPoints的intensity之和 除以 所有intensity之和",
                true, true),
        IsotopeCorrelationScore("IsotopeCorrelationScore", "var_IsotopeCorrelationScore",
                "",
                null, false),
        IsotopeOverlapScore("IsotopeOverlapScore", "var_IsotopeOverlapScore",
                "feature intensity加权的可能（带电量1-4）无法区分同位素峰值的平均发生次数之和",
                null, false),
        LibraryCorr("LibraryCorr", "var_LibraryCorr",
                "对experiment和library intensity算Pearson相关系",
                true, true),
        LibraryRsmd("LibraryRsmd", "var_LibraryRsmd",
                "对experiment intensity 算占比差距平均值",
                false, true),
        LogSnScore("LogSnScore", "var_LogSnScore",
                "log(距离ApexRt最近点的stn值之和)",
                true, true),
        MassdevScore("MassdevScore", "var_MassdevScore",
                "按spectrum intensity加权的mz与product mz的偏差ppm百分比之和",
                null, false),
        MassdevScoreWeighted("MassdevScoreWeighted", "var_MassdevScoreWeighted",
                "按spectrum intensity加权的mz与product mz的偏差ppm百分比按libraryIntensity加权之和",
                null, false),
        NormRtScore("NormRtScore", "var_NormRtScore",
                "normalizedExperimentalRt与groupRt之差",
                false, true),
        XcorrCoelution("XcorrCoelution", "var_XcorrCoelution",
                "互相关偏移的mean + std",
                false, true),
        XcorrCoelutionWeighted("XcorrCoelutionWeighted", "var_XcorrCoelutionWeighted",
                "带权重的相关偏移sum",
                false, true),
        XcorrShape("XcorrShape", "var_XcorrShape",
                "互相关序列最大值的平均值",
                true, true),
        XcorrShapeWeighted("XcorrShapeWeighted", "var_XcorrShapeWeighted",
                "带权重的互相关序列最大值的平均值",
                true, true),
        LibraryDotprod("LibraryDotprod", "var_LibraryDotprod",
                "",
                true, true),
        LibraryManhattan("LibraryManhattan", "var_LibraryManhattan",
                "",
                false, true),
        LibrarySangle("LibrarySangle", "var_LibrarySangle",
                "",
                false, true),
        LibraryRootmeansquare("LibraryRootmeansquare", "var_LibraryRootmeansquare",
                "",
                false, true),
        ManhattScore("ManhattScore", "var_ManhattScore",
                "",
                null, false),
        YseriesScore("YseriesScore", "var_YseriesScore",
                "peptideRt对应的spectrumArray中，检测到的y离子的数量",
                null, false),
        ;

        String typeName;

        String pyProphetName;

        String description;

        Boolean biggerIsBetter;

        boolean isUsed;

        ScoreType(String typeName, String pyProphetName, String description, Boolean biggerIsBetter, Boolean isUsed) {
            this.typeName = typeName;
            this.pyProphetName = pyProphetName;
            this.isUsed = isUsed;
            this.biggerIsBetter = biggerIsBetter;
            this.description = description;
        }

        public static Boolean getBiggerIsBetter(String typeName){
            for(ScoreType type : values()){
                if (type.getTypeName().equals(typeName)){
                    return type.getBiggerIsBetter();
                }
            }
            return null;
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

        public Boolean getBiggerIsBetter() {
            return biggerIsBetter;
        }

        public String getDescription() {
            return description;
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
