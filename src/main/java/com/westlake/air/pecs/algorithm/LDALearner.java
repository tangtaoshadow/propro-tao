package com.westlake.air.pecs.algorithm;

import com.westlake.air.pecs.domain.bean.airus.TrainData;
import com.westlake.air.pecs.domain.bean.airus.TrainPeaks;
import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import com.westlake.air.pecs.domain.bean.score.SimpleFeatureScores;
import com.westlake.air.pecs.domain.db.simple.SimpleScores;
import com.westlake.air.pecs.utils.AirusUtil;
import com.westlake.air.pecs.utils.MathUtil;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class LDALearner {

    public final Logger logger = LoggerFactory.getLogger(LDALearner.class);

    /**
     * Get clfScore with given confidence(params).
     * 根据weightsMap计算子分数的(加权总分-平均加权总分)
     */
    public void score(TrainData data, HashMap<String, Double> weightsMap) {
        score(data.getTargets(), weightsMap);
        score(data.getDecoys(), weightsMap);
    }

    public void score(List<SimpleScores> scores, HashMap<String, Double> weightsMap) {
        Set<Map.Entry<String, Double>> entries = weightsMap.entrySet();
        for (SimpleScores score : scores) {
            for (FeatureScores featureScores : score.getFeatureScoresList()) {
                double addedScore = 0;
                for (Map.Entry<String, Double> entry : entries) {
                    addedScore += featureScores.getScoresMap().get(entry.getKey()) * entry.getValue();
                }
                featureScores.put(FeatureScores.ScoreType.WeightedTotalScore.getTypeName(), addedScore);
            }
        }
    }

    /**
     * Get clfScore with given confidence(params).
     */
    public Double[] score(Double[][] peaks, Double[] weights, boolean useMainScore) {
        Double[][] featureMatrix = AirusUtil.getFeatureMatrix(peaks, useMainScore);
        if (featureMatrix != null) {
            return MathUtil.dot(featureMatrix, weights);
        } else {
            logger.error("Score Error");
            return null;
        }
    }

    /**
     * Calculate average confidence(weight) of nevals(trainTimes).
     */
    public Double[] averagedWeight(Double[][] weights) {
        Double[] averagedW = new Double[weights[0].length];
        double sum = 0.0;
        for (int i = 0; i < weights[0].length; i++) {
            for (Double[] j : weights) {
                sum += j[i];
            }
            averagedW[i] = sum / weights.length;
            sum = 0;
        }
        return averagedW;
    }

    /**
     * 使用apache的svd库进行计算
     *
     * @param trainPeaks
     * @param skipScoreType 需要在结果中剔除的主分数,如果为空则不删除
     * @return key为子分数的名称, value是该子分数的权重值
     */
    public HashMap<String, Double> learn(TrainPeaks trainPeaks, String skipScoreType) throws Exception {

        int totalLength = trainPeaks.getBestTargets().size() + trainPeaks.getTopDecoys().size();
        HashMap<String, Double> scoreMapSample = trainPeaks.getBestTargets().get(0).getScoresMap();
        int scoreTypes = 0;
        Set<String> keySet = scoreMapSample.keySet();
        if (keySet.contains(skipScoreType)) {
            scoreTypes = keySet.size() - 1;
        } else {
            scoreTypes = keySet.size();
        }

        //先将需要进入学习的打分转化为二位矩阵
        RealMatrix scoresMatrix = new Array2DRowRealMatrix(totalLength, scoreTypes);
        RealVector labelVector = new ArrayRealVector(totalLength);
        int k = 0;
        for (SimpleFeatureScores sfs : trainPeaks.getBestTargets()) {
            int i = 0;
            for (String scoreType : keySet) {
                if(scoreType.equals(skipScoreType)){
                    continue;
                }
                scoresMatrix.setEntry(k, i, sfs.getScoresMap().get(scoreType));
                i++;
            }
            labelVector.setEntry(k, 1);
            k++;
        }
        for (SimpleFeatureScores sfs : trainPeaks.getTopDecoys()) {
            int i = 0;
            for (String scoreType : keySet) {
                if(scoreType.equals(skipScoreType)){
                    continue;
                }
                scoresMatrix.setEntry(k, i, sfs.getScoresMap().get(scoreType));
                i++;
            }
            labelVector.setEntry(k, 0);
            k++;
        }

        org.apache.commons.math3.linear.SingularValueDecomposition solver = new org.apache.commons.math3.linear.SingularValueDecomposition(scoresMatrix);
        RealVector realVector = solver.getSolver().solve(labelVector);

        HashMap<String, Double> weightsMap = new HashMap<>();
        int tempJ = 0;
        for (String key : scoreMapSample.keySet()) {
            if(key.equals(skipScoreType)){
                continue;
            }
            weightsMap.put(key, realVector.getEntry(tempJ));
            tempJ++;
        }
        return weightsMap;
    }

}
