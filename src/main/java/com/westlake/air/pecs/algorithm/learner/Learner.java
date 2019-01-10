package com.westlake.air.pecs.algorithm.learner;

import com.westlake.air.pecs.constants.ScoreType;
import com.westlake.air.pecs.domain.bean.airus.TrainData;
import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import com.westlake.air.pecs.domain.db.simple.SimpleScores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Learner {

    public final Logger logger = LoggerFactory.getLogger(Learner.class);

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
            if(score.getFeatureScoresList() == null){
                continue;
            }
            for (FeatureScores featureScores : score.getFeatureScoresList()) {
                double addedScore = 0;
                for (Map.Entry<String, Double> entry : entries) {
                    addedScore += featureScores.getScoresMap().get(entry.getKey()) * entry.getValue();
                }
                featureScores.put(ScoreType.WeightedTotalScore.getTypeName(), addedScore);
            }
        }
    }
}
