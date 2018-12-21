package com.westlake.air.pecs.algorithm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.westlake.air.pecs.algorithm.learner.LDALearner;
import com.westlake.air.pecs.algorithm.learner.XGBoostLearner;
import com.westlake.air.pecs.constants.ScoreType;
import com.westlake.air.pecs.domain.bean.airus.*;
import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import com.westlake.air.pecs.domain.bean.score.SimpleFeatureScores;
import com.westlake.air.pecs.domain.db.simple.SimpleScores;
import com.westlake.air.pecs.utils.AirusUtil;
import ml.dmlc.xgboost4j.java.Booster;
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
    @Autowired
    XGBoostLearner xgBoostLearner;

    public LDALearnData learnRandomized(List<SimpleScores> scores, AirusParams airusParams) {
        LDALearnData ldaLearnData = new LDALearnData();
        try {
            //Get part of scores as train input.
            TrainData trainData = AirusUtil.split(scores, airusParams.getTrainTestRatio(), airusParams.isDebug());

            //第一次训练数据集使用指定的主分数(默认为MainScore)进行训练
            TrainPeaks trainPeaks = selectTrainPeaks(trainData, airusParams.getMainScore(), airusParams, airusParams.getSsInitialFdr());

            HashMap<String, Double> weightsMap = ldaLearner.learn(trainPeaks, airusParams.getMainScore());
            logger.info("Train Weight:" + JSONArray.toJSONString(weightsMap));

            //根据weightsMap计算子分数的加权总分
            ldaLearner.score(trainData, weightsMap);
            for (int times = 0; times < airusParams.getXevalNumIter(); times++) {
                TrainPeaks trainPeaksTemp = selectTrainPeaks(trainData, ScoreType.WeightedTotalScore.getTypeName(), airusParams, airusParams.getSsIterationFdr());
                weightsMap = ldaLearner.learn(trainPeaksTemp, ScoreType.WeightedTotalScore.getTypeName());
                for(Double value: weightsMap.values()){
                    if(value == null || Double.isNaN(value)){
                        logger.info("本轮训练一坨屎:"+ JSON.toJSONString(weightsMap));
                        continue;
                    }
                }
                ldaLearner.score(trainData, weightsMap);
            }
            //每一轮结束后要将这一轮打出的加权总分删除掉,以免影响下一轮打分
//            trainData.removeWeightedTotalScore();
            ldaLearnData.setWeightsMap(weightsMap);
            return ldaLearnData;
        } catch (Exception e) {
            logger.error("learnRandomized Fail.\n");
            e.printStackTrace();
            return null;
        }

    }

    public Booster learnRandomizedXGB(List<SimpleScores> scores, AirusParams airusParams){
        try{
            //Get part of scores as train input.
            TrainData trainData = AirusUtil.split(scores, airusParams.getTrainTestRatio(), airusParams.isDebug());
            //第一次训练数据集使用MainScore进行训练
            long startTime = System.currentTimeMillis();
            TrainPeaks trainPeaks = selectTrainPeaks(trainData, airusParams.getMainScore(), airusParams, airusParams.getSsInitialFdr());
            logger.info("高可信Target个数："+ trainPeaks.getBestTargets().size());
            Booster booster = xgBoostLearner.train(trainPeaks, airusParams.getMainScore());
            xgBoostLearner.predict(booster, trainData, airusParams.getMainScore());
            for(int times = 0; times < airusParams.getXevalNumIter(); times++){
                logger.info("开始第"+ times +"轮训练");
                TrainPeaks trainPeaksTemp = selectTrainPeaks(trainData, ScoreType.WeightedTotalScore.getTypeName(), airusParams, airusParams.getXgbIterationFdr());
                logger.info("高可信Target个数："+ trainPeaksTemp.getBestTargets().size());
                booster = xgBoostLearner.train(trainPeaksTemp, ScoreType.WeightedTotalScore.getTypeName());
                xgBoostLearner.predict(booster, trainData, ScoreType.WeightedTotalScore.getTypeName());
            }
            logger.info("总时间：" +(System.currentTimeMillis()-startTime));
            List<SimpleFeatureScores> featureScoresList = AirusUtil.findTopFeatureScores(scores, ScoreType.WeightedTotalScore.getTypeName());
            ErrorStat errorStat = stats.errorStatistics(featureScoresList, airusParams);
            int count = AirusUtil.checkFdr(errorStat.getStatMetrics().getFdr());
            logger.info("Train count:" + count);
            return booster;
        } catch (Exception e) {
            logger.error("learnRandomizedXGB Fail.\n");
            e.printStackTrace();
            return null;
        }
    }

    private TrainPeaks selectTrainPeaks(TrainData trainData, String usedScoreType, AirusParams airusParams, Double cutoff) {

        List<SimpleFeatureScores> topTargetPeaks = AirusUtil.findTopFeatureScores(trainData.getTargets(), usedScoreType);
        List<SimpleFeatureScores> topDecoyPeaks = AirusUtil.findTopFeatureScores(trainData.getDecoys(), usedScoreType);

        // find cutoff fdr from scores and only use best target peaks:
        Double cutoffNew = stats.findCutoff(topTargetPeaks, topDecoyPeaks, airusParams, cutoff);
        List<SimpleFeatureScores> bestTargetPeaks = AirusUtil.peaksFilter(topTargetPeaks, cutoffNew);

        TrainPeaks trainPeaks = new TrainPeaks();
        trainPeaks.setBestTargets(bestTargetPeaks);
        trainPeaks.setTopDecoys(topDecoyPeaks);
        return trainPeaks;
    }
}
