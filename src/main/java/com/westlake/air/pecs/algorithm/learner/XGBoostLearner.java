package com.westlake.air.pecs.algorithm.learner;

import com.westlake.air.pecs.constants.ScoreType;
import com.westlake.air.pecs.domain.bean.airus.TrainData;
import com.westlake.air.pecs.domain.bean.airus.TrainPeaks;
import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import com.westlake.air.pecs.domain.bean.score.SimpleFeatureScores;
import com.westlake.air.pecs.domain.db.simple.SimpleScores;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoostError;
import org.junit.Test;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by Nico Wang
 * Time: 2018-12-07 10:21
 */
@Component
public class XGBoostLearner extends Learner {

    Map<String, Object> params = new HashMap<String, Object>() {
        {
            put("booster", "gbtree");
//            put("gamma", 0);
            put("min_child_weight", 10);
            put("eta", 0.6);
            put("max_depth", 4);
//            put("silent", 0);
//            put("alpha", 1);
//            put("lambda", 0.5);// 用于逻辑回归的时候L2正则选项
            put("objective", "binary:logistic");
            put("eval_metric", "auc");
            put("seed", "23");
        }
    };
    int nRounds = 100;

    public Booster train(TrainPeaks trainPeaks, String skipScoreType) throws XGBoostError {

        DMatrix trainMat = trainPeaksToDMatrix(trainPeaks, skipScoreType);
        Map<String, DMatrix> watches = new HashMap<>();
        watches.put("train", trainMat);

        Booster booster = XGBoost.train(trainMat, this.params,100, watches, null, null);
//        booster.saveModel("D:\\model.bin");
        return booster;
    }

    public void predict(Booster booster, TrainData trainData, String skipScoreType) throws XGBoostError {
        List<SimpleScores> totalGroupScore = new ArrayList<>(trainData.getDecoys());
        totalGroupScore.addAll(trainData.getTargets());
        predictAll(booster, totalGroupScore, skipScoreType);
    }

    public void predictAll(Booster booster, List<SimpleScores> scores, String skipScoreType) throws XGBoostError{
        Set<String> keySet = scores.get(0).getFeatureScoresList().get(0).getScoresMap().keySet();
        int scoreTypes = keySet.size();
        if(skipScoreType.equals(ScoreType.MainScore.getTypeName())){
            scoreTypes -= 1;
        }else{
            scoreTypes -= 2;
        }
//        List<Float> testData = new ArrayList<>();
        for(SimpleScores simpleScores : scores){
            for(FeatureScores featureScores: simpleScores.getFeatureScoresList()){
                if(!simpleScores.getIsDecoy() && !checkRationality(featureScores)){
                    featureScores.put(ScoreType.WeightedTotalScore.getTypeName(), 0d);
                    continue;
                }
                HashMap<String,Double> scoreMap = featureScores.getScoresMap();
                float[] testData = new float[scoreTypes];
                int tempIndex = 0;
                for (String scoreName : keySet) {
                    if (scoreName.equals(ScoreType.WeightedTotalScore.getTypeName()) || scoreName.equals(ScoreType.MainScore.getTypeName())) {
                        continue;
                    }
                    testData[tempIndex] = scoreMap.get(scoreName).floatValue();
                    tempIndex ++;
                }
                DMatrix dMatrix = new DMatrix(testData, 1,scoreTypes);
                float[][] predicts = booster.predict(dMatrix);
                double score = predicts[0][0];
                featureScores.put(ScoreType.WeightedTotalScore.getTypeName(), score);
            }
        }
    }

    public DMatrix trainPeaksToDMatrix(TrainPeaks trainPeaks, String skipScoreType) throws XGBoostError {


        int totalLength = trainPeaks.getBestTargets().size() + trainPeaks.getTopDecoys().size();
        HashMap<String, Double> scoreMapSample = trainPeaks.getBestTargets().get(0).getScoresMap();
        Set<String> keySet = scoreMapSample.keySet();
        int scoreTypes = keySet.size();
        if(skipScoreType.equals(ScoreType.MainScore.getTypeName())){
            scoreTypes -= 1;
        }else{
            scoreTypes -= 2;
        }

        float[] trainData = new float[totalLength * scoreTypes];
        float[] trainLabel = new float[totalLength];
        int dataIndex = 0, labelIndex = 0;
        for(SimpleFeatureScores sample: trainPeaks.getBestTargets()){
            for (String scoreName : keySet) {
                if(scoreName.equals(skipScoreType) || scoreName.equals(ScoreType.MainScore.getTypeName())){
                    continue;
                }
                trainData[dataIndex] = sample.getScoresMap().get(scoreName).floatValue();
                trainLabel[labelIndex] = 1;
                dataIndex ++;
            }
            labelIndex ++;
        }
        for(SimpleFeatureScores sample: trainPeaks.getTopDecoys()){
            for (String scoreName : keySet) {
                if(scoreName.equals(skipScoreType) || scoreName.equals(ScoreType.MainScore.getTypeName())){
                    continue;
                }
                trainData[dataIndex] = sample.getScoresMap().get(scoreName).floatValue();
                trainLabel[labelIndex] = 0;
                dataIndex ++;
            }
            labelIndex ++;
        }

        DMatrix trainMat = new DMatrix(trainData, totalLength, scoreTypes);
        trainMat.setLabel(trainLabel);
        return trainMat;
    }

    private boolean checkRationality(FeatureScores featureScores){
        HashMap<String, Double> scoresMap = featureScores.getScoresMap();
        if(scoresMap.get(ScoreType.XcorrShape.getTypeName()) < 0.5){
            return false;
        }else {
            return true;
        }
    }


}
