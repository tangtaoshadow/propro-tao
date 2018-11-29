package com.westlake.air.pecs.algorithm.learner;

import com.westlake.air.pecs.domain.bean.airus.TrainPeaks;
import com.westlake.air.pecs.domain.bean.score.SimpleFeatureScores;
import org.apache.commons.math3.linear.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Set;

@Component
public class LDALearner extends Learner{

    public final Logger logger = LoggerFactory.getLogger(LDALearner.class);

    /**
     * 使用apache的svd库进行计算
     *
     * @param trainPeaks
     * @param skipScoreType 需要在结果中剔除的主分数,如果为空则不删除
     * @return key为子分数的名称, value是该子分数的权重值
     */
    public HashMap<String, Double> learn(TrainPeaks trainPeaks, String skipScoreType) {

        int totalLength = trainPeaks.getBestTargets().size() + trainPeaks.getTopDecoys().size();
        HashMap<String, Double> scoreMapSample = trainPeaks.getBestTargets().get(0).getScoresMap();
        int scoreTypes = 0;
        Set<String> keySet = scoreMapSample.keySet();
        if (keySet.contains(skipScoreType)) {
            scoreTypes = keySet.size() - 1;
        } else {
            scoreTypes = keySet.size();
        }

        //先将需要进入学习的打分转化为二维矩阵
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

        //计算SVD的解
        SingularValueDecomposition solver = new SingularValueDecomposition(scoresMatrix);
        RealVector realVector = solver.getSolver().solve(labelVector);

        //输出最终的权重值
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
