package com.westlake.air.pecs.algorithm;

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

    @Deprecated
    public LDALearnData learnRandomized(Double[][] scores, Integer[] groupNumId, Boolean[] isDecoy, Params params) {
        LDALearnData ldaLearnData = new LDALearnData();
        try {

            //Get part of scores as train input.
            TrainAndTest trainAndTest = AirusUtil.split(scores, groupNumId, isDecoy, params.getTrainTestRatio(), params.isDebug());

            Double[][] trainScores = trainAndTest.getTrainData();
            Integer[] trainId = trainAndTest.getTrainId();
            Boolean[] trainIsDecoy = trainAndTest.getTrainIsDecoy();
            Double[] mainScore = ArrayUtil.extractColumn(trainScores, 0);
            Boolean[] isTopPeak = AirusUtil.findTopIndex(mainScore, trainId);

            //Start the first time of training with mainScore as main score.
            TrainPeaks trainPeaks = selectTrainPeaks(trainScores, mainScore, trainIsDecoy, isTopPeak, params.getSsInitialFdr());

            Double[] w = ldaLearner.learn(trainPeaks.getTopDecoyPeaks(), trainPeaks.getBestTargetPeaks(), false);

            Double[] clfScores = ldaLearner.score(trainScores, w, false);

            clfScores = MathUtil.normalize(clfScores);
            isTopPeak = AirusUtil.findTopIndex(clfScores, trainId);

            //Begin "semi supervised learning" iteration.
            for (int times = 0; times < params.getXevalNumIter(); times++) {
                logger.info("开始第" + times + "轮机器学习");

                TrainPeaks trainPeaksTemp = selectTrainPeaks(trainScores, clfScores, trainIsDecoy, isTopPeak, params.getSsIterationFdr());

                w = ldaLearner.learn(trainPeaksTemp.getTopDecoyPeaks(), trainPeaksTemp.getBestTargetPeaks(), true);

                clfScores = ldaLearner.score(trainScores, w, true);

                isTopPeak = AirusUtil.findTopIndex(clfScores, trainId);
            }

            ldaLearnData.setWeights(w);
            return ldaLearnData;
        } catch (Exception e) {
            logger.error("learnRandomized Fail.\n");
            e.printStackTrace();
            return null;
        }
    }

    public LDALearnData learnRandomized(List<SimpleScores> scores, Params params) {
        LDALearnData ldaLearnData = new LDALearnData();
        try {
            //Get part of scores as train input.
            TrainData trainData = AirusUtil.split(scores, params.getTrainTestRatio(), params.isDebug());
            //第一次训练数据集使用MainScore进行训练
            TrainPeaks trainPeaks = selectTrainPeaks(trainData, params.getMainScore(), params);
            HashMap<String, Double> weightsMap = ldaLearner.learn(trainPeaks, params.getMainScore());
            //根据weightsMap计算子分数的加权总分
            ldaLearner.score(trainData, weightsMap);

            for (int times = 0; times < params.getXevalNumIter(); times++) {
                TrainPeaks trainPeaksTemp = selectTrainPeaks(trainData, FeatureScores.ScoreType.WeightedTotalScore.getTypeName(), params);
                weightsMap = ldaLearner.learn(trainPeaksTemp, FeatureScores.ScoreType.WeightedTotalScore.getTypeName());
                ldaLearner.score(trainData, weightsMap);
            }
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

    private TrainPeaks selectTrainPeaks(Double[][] trainData, Double[] scores, Boolean[] trainIsDecoy, Boolean[] isTopPeak, double cutOffFdr) {

        Double[][] topTargetPeaks = AirusUtil.getTopTargetPeaks(trainData, trainIsDecoy, isTopPeak);
        Double[] topTargetScores = AirusUtil.getTopTargetPeaks(scores, trainIsDecoy, isTopPeak);
        Double[][] topDecoyPeaks = AirusUtil.getTopDecoyPeaks(trainData, trainIsDecoy, isTopPeak);
        Double[] topDecoyScores = AirusUtil.getTopDecoyPeaks(scores, trainIsDecoy, isTopPeak);

        // find cutoff fdr from scores and only use best target peaks:
        Double cutoff = stats.findCutoff(topTargetScores, topDecoyScores, cutOffFdr);
        Double[][] bestTargetPeaks = AirusUtil.peaksFilter(topTargetPeaks, topTargetScores, cutoff);

        TrainPeaks trainPeaks = new TrainPeaks();
        trainPeaks.setBestTargetPeaks(bestTargetPeaks);
        trainPeaks.setTopDecoyPeaks(topDecoyPeaks);
        return trainPeaks;
    }

    private Double[] score(Double[][] data, Double[] params) {
        return ldaLearner.score(data, params, true);
    }
}
