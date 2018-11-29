package com.westlake.air.pecs.algorithm;

import com.alibaba.fastjson.JSONArray;
import com.westlake.air.pecs.domain.bean.airus.*;
import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import com.westlake.air.pecs.domain.bean.score.SimpleFeatureScores;
import com.westlake.air.pecs.domain.db.simple.SimpleScores;
import com.westlake.air.pecs.utils.AirusUtil;
import com.westlake.air.pecs.utils.ArrayUtil;
import com.westlake.air.pecs.utils.MathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-13 15:28
 */
@Component
public class SemiSupervised {

    public final Logger logger = LoggerFactory.getLogger(SemiSupervised.class);

    @Autowired
    Stats stats;
    @Autowired
    LDALearner ldaLearner;

    /**
     * set w as average
     *
     * @param weights w[]: the result of nevals
     */
    public Double[] averagedLearner(Double[][] weights) {
        return ldaLearner.averagedWeight(weights);
    }

    public LDALearnData learnRandomized(List<SimpleScores> scores, Params params) {
        LDALearnData ldaLearnData = new LDALearnData();
        try {
            //Get part of scores as train input.
            TrainData trainData = AirusUtil.split(scores, params.getTrainTestRatio(), params.isDebug());
            //第一次训练数据集使用MainScore进行训练
            TrainPeaks trainPeaks = selectTrainPeaks(trainData, params.getMainScore(), params);
            HashMap<String, Double> weightsMap = ldaLearner.learn(trainPeaks, params.getMainScore());
            logger.info("Train1:"+ JSONArray.toJSONString(weightsMap));
            //根据weightsMap计算子分数的加权总分
            ldaLearner.score(trainData, weightsMap);
            for (int times = 0; times < params.getXevalNumIter(); times++) {
                TrainPeaks trainPeaksTemp = selectTrainPeaks(trainData, FeatureScores.ScoreType.WeightedTotalScore.getTypeName(), params);
                weightsMap = ldaLearner.learn(trainPeaksTemp, FeatureScores.ScoreType.WeightedTotalScore.getTypeName());
                ldaLearner.score(trainData, weightsMap);
            }
            //每一轮结束后要将这一轮打出的加权总分删除掉,以免影响下一轮打分
            trainData.removeWeightedTotalScore();
            ldaLearnData.setWeightsMap(weightsMap);
            return ldaLearnData;
        } catch (Exception e) {
            logger.error("learnRandomized Fail.\n");
            e.printStackTrace();
            return null;
        }

    }

    private TrainPeaks selectTrainPeaks(TrainData trainData, String usedScoreType, Params params) {

        List<SimpleFeatureScores> topTargetPeaks = AirusUtil.findTopFeatureScores(trainData.getTargets(), usedScoreType);
        List<SimpleFeatureScores> topDecoyPeaks = AirusUtil.findTopFeatureScores(trainData.getDecoys(), usedScoreType);

        // find cutoff fdr from scores and only use best target peaks:
        Double cutoff = stats.findCutoff(topTargetPeaks, topDecoyPeaks, params);
        List<SimpleFeatureScores> bestTargetPeaks = AirusUtil.peaksFilter(topTargetPeaks, cutoff);

        TrainPeaks trainPeaks = new TrainPeaks();
        trainPeaks.setBestTargets(bestTargetPeaks);
        trainPeaks.setTopDecoys(topDecoyPeaks);
        return trainPeaks;
    }
}
