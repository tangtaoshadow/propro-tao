package com.westlake.air.propro.algorithm.learner.classifier;

import com.alibaba.fastjson.JSON;
import com.westlake.air.propro.domain.bean.airus.TrainPeaks;
import com.westlake.air.propro.domain.bean.score.SimpleFeatureScores;
import org.apache.commons.math3.linear.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@Component("ldaClassifier")
public class LDAClassifier extends Classifier{

    public final Logger logger = LoggerFactory.getLogger(LDAClassifier.class);

    /**
     * 使用apache的svd库进行计算
     *
     * @param trainPeaks
     * @param skipScoreType 需要在结果中剔除的主分数,如果为空则不删除
     * @return key为子分数的名称, value是该子分数的权重值
     */
    public HashMap<String, Double> learn(TrainPeaks trainPeaks, String skipScoreType, List<String> scoreTypes) {

        int totalLength = trainPeaks.getBestTargets().size() + trainPeaks.getTopDecoys().size();
        int scoreTypesCount = 0;
        if (scoreTypes.contains(skipScoreType)) {
            scoreTypesCount = scoreTypes.size() - 1;
        } else {
            scoreTypesCount = scoreTypes.size();
        }

        //先将需要进入学习的打分转化为二维矩阵
        RealMatrix scoresMatrix = new Array2DRowRealMatrix(totalLength, scoreTypesCount);
        RealVector labelVector = new ArrayRealVector(totalLength);
        int k = 0;
        for (SimpleFeatureScores sfs : trainPeaks.getBestTargets()) {
            int i = 0;
            for (String scoreType : scoreTypes) {
                if (scoreType.equals(skipScoreType)) {
                    continue;
                }
                scoresMatrix.setEntry(k, i, sfs.get(scoreType, scoreTypes));
                i++;
            }
            labelVector.setEntry(k, 1);
            k++;
        }
        for (SimpleFeatureScores sfs : trainPeaks.getTopDecoys()) {
            int i = 0;
            for (String scoreType : scoreTypes) {
                if (scoreType.equals(skipScoreType)) {
                    continue;
                }
                scoresMatrix.setEntry(k, i, sfs.get(scoreType, scoreTypes));
                i++;
            }
            labelVector.setEntry(k, 0);
            k++;
        }

        //计算SVD的解
        SingularValueDecomposition solver = new SingularValueDecomposition(scoresMatrix);
        RealVector realVector = solver.getSolver().solve(labelVector);

        //输出最终的权重值
        HashMap<String, Double> weightsMap = new HashMap<>();
        int tempJ = 0;
        for (String key : scoreTypes) {
            if (key.equals(skipScoreType)) {
                continue;
            }
            weightsMap.put(key, realVector.getEntry(tempJ));
            tempJ++;
        }

        for (Double value : weightsMap.values()) {
            if (value == null || Double.isNaN(value)) {
                logger.info("本轮训练一坨屎:" + JSON.toJSONString(weightsMap));
                return null;
            }
        }
        return weightsMap;
    }

}