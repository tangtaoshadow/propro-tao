package com.westlake.air.propro.algorithm.learner;

import com.westlake.air.propro.constants.ScoreType;
import com.westlake.air.propro.domain.bean.airus.TrainData;
import com.westlake.air.propro.domain.bean.airus.TrainPeaks;
import com.westlake.air.propro.domain.bean.score.FeatureScores;
import com.westlake.air.propro.domain.bean.score.SimpleFeatureScores;
import com.westlake.air.propro.domain.db.simple.SimpleScores;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoostError;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by Nico Wang
 * Time: 2018-12-07 10:21
 */
@Component("xgboostLearner")
public class XGBoostLearner extends Learner {

    Map<String, Object> params = new HashMap<String, Object>() {
        {
            //original params
//            put("booster", "gbtree");
//            put("min_child_weight", 10);
//            put("eta", 0.6);
//            put("max_depth", 4);
//            put("objective", "binary:logistic");
//            put("eval_metric", "auc");
//            put("seed", "23");
            put("booster", "gbtree");
            put("min_child_weight", 10);//cv
            put("eta", 0.1);//0.01-0.2
            put("max_depth", 4);//3-10,与max_leaf_nodes互斥
//            put("silent", 0);
//            put("alpha", 1);
//            put("lambda", 0.5);// 用于逻辑回归的时候L2正则选项
            put("objective", "binary:logitraw");
            put("eval_metric", "error");
            put("seed", "23");
            put("subsample", 0.5);
        }
    };
    int nRounds = 100;

    public Booster train(TrainPeaks trainPeaks, String skipScoreType, List<String> scoreTypes) throws XGBoostError {
        DMatrix trainMat = trainPeaksToDMatrix(trainPeaks, skipScoreType, scoreTypes);
        Map<String, DMatrix> watches = new HashMap<>();
        watches.put("train", trainMat);

        Booster booster = XGBoost.train(trainMat, this.params, 500, watches, null, null);
        return booster;
    }

    public void predict(Booster booster, TrainData trainData, String skipScoreType, List<String> scoreTypes) throws XGBoostError {
        List<SimpleScores> totalGroupScore = new ArrayList<>(trainData.getDecoys());
        totalGroupScore.addAll(trainData.getTargets());
        predictAll(booster, totalGroupScore, skipScoreType, scoreTypes);
    }

    public void predictAll(Booster booster, List<SimpleScores> scores, String skipScoreType, List<String> scoreTypes) throws XGBoostError {
        int scoreTypesCount = scoreTypes.size();
        if (skipScoreType.equals(ScoreType.MainScore.getTypeName())) {
            scoreTypesCount -= 1;
        } else {
            scoreTypesCount -= 2;
        }
//        List<Float> testData = new ArrayList<>();
        for (SimpleScores simpleScores : scores) {
            for (FeatureScores featureScores : simpleScores.getFeatureScoresList()) {
                if (!simpleScores.getIsDecoy() && !checkRationality(featureScores, scoreTypes)) {
                    featureScores.put(ScoreType.WeightedTotalScore.getTypeName(), 0d, scoreTypes);
                    continue;
                }
                float[] testData = new float[scoreTypesCount];
                int tempIndex = 0;
                for (String scoreName : scoreTypes) {
                    if (scoreName.equals(ScoreType.WeightedTotalScore.getTypeName()) || scoreName.equals(ScoreType.MainScore.getTypeName())) {
                        continue;
                    }
                    testData[tempIndex] = featureScores.get(scoreName, scoreTypes).floatValue();
                    tempIndex++;
                }
                DMatrix dMatrix = new DMatrix(testData, 1, scoreTypesCount);
                float[][] predicts = booster.predict(dMatrix);
                double score = predicts[0][0];
                featureScores.put(ScoreType.WeightedTotalScore.getTypeName(), score, scoreTypes);
            }
        }
    }

    public DMatrix trainPeaksToDMatrix(TrainPeaks trainPeaks, String skipScoreType, List<String> scoreTypes) throws XGBoostError {
        int totalLength = trainPeaks.getBestTargets().size() + trainPeaks.getTopDecoys().size();
        int scoreTypesCount = scoreTypes.size();
        if (skipScoreType.equals(ScoreType.MainScore.getTypeName())) {
            scoreTypesCount -= 1;
        } else {
            scoreTypesCount -= 2;
        }

        float[] trainData = new float[totalLength * scoreTypesCount];
        float[] trainLabel = new float[totalLength];
        int dataIndex = 0, labelIndex = 0;
        for (SimpleFeatureScores sample : trainPeaks.getBestTargets()) {
            for (String scoreName : scoreTypes) {
                if (scoreName.equals(skipScoreType) || scoreName.equals(ScoreType.MainScore.getTypeName())) {
                    continue;
                }
                trainData[dataIndex] = sample.get(scoreName, scoreTypes).floatValue();
                trainLabel[labelIndex] = 1;
                dataIndex++;
            }
            labelIndex++;
        }
        for (SimpleFeatureScores sample : trainPeaks.getTopDecoys()) {
            for (String scoreName : scoreTypes) {
                if (scoreName.equals(skipScoreType) || scoreName.equals(ScoreType.MainScore.getTypeName())) {
                    continue;
                }
                trainData[dataIndex] = sample.get(scoreName, scoreTypes).floatValue();
                trainLabel[labelIndex] = 0;
                dataIndex++;
            }
            labelIndex++;
        }

        DMatrix trainMat = new DMatrix(trainData, totalLength, scoreTypesCount);
        trainMat.setLabel(trainLabel);
        return trainMat;
    }

    private boolean checkRationality(FeatureScores featureScores, List<String> scoreTypes) {
        if (featureScores.get(ScoreType.XcorrShape.getTypeName(), scoreTypes) < 0.5) {
            return false;
        } else {
            return true;
        }
    }


}
