package com.westlake.air.propro.algorithm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.westlake.air.propro.algorithm.learner.LDALearner;
import com.westlake.air.propro.algorithm.learner.XGBoostLearner;
import com.westlake.air.propro.constants.ScoreType;
import com.westlake.air.propro.domain.bean.airus.*;
import com.westlake.air.propro.domain.bean.score.FeatureScores;
import com.westlake.air.propro.domain.bean.score.SimpleFeatureScores;
import com.westlake.air.propro.domain.db.simple.SimpleScores;
import com.westlake.air.propro.utils.AirusUtil;
import ml.dmlc.xgboost4j.java.Booster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
//            TrainPeaks trainPeaks = selectTrainPeaks(trainData, airusParams.getMainScore(), airusParams, airusParams.getSsInitialFdr());
            TrainPeaks trainPeaks = selectFirstTrainPeaks(trainData);

            HashMap<String, Double> weightsMap = ldaLearner.learn(trainPeaks, airusParams.getMainScore());
            logger.info("Train Weight:" + JSONArray.toJSONString(weightsMap));

            //根据weightsMap计算子分数的加权总分
            ldaLearner.score(trainData, weightsMap);
            weightsMap = new HashMap<>();
            HashMap<String, Double> lastWeightsMap = new HashMap<>();
            for (int times = 0; times < airusParams.getXevalNumIter(); times++) {
                TrainPeaks trainPeaksTemp = selectTrainPeaks(trainData, ScoreType.WeightedTotalScore.getTypeName(), airusParams, airusParams.getSsIterationFdr());
//                if(trainPeaksTemp.getBestTargets().size() == 0){
//                    System.out.println("emmm");
//                }
                lastWeightsMap = weightsMap;
                weightsMap = ldaLearner.learn(trainPeaksTemp, ScoreType.WeightedTotalScore.getTypeName());
//                cleanWeightsMap(weightsMap);
                logger.info("Train Weight:" + JSONArray.toJSONString(weightsMap));
                for(Double value: weightsMap.values()){
                    if(value == null || Double.isNaN(value)){
                        logger.info("本轮训练一坨屎:"+ JSON.toJSONString(weightsMap));
                        continue;
                    }
                }
                if (lastWeightsMap.size() !=0) {
                    for (String key : weightsMap.keySet()) {
                        weightsMap.put(key, (weightsMap.get(key) + lastWeightsMap.get(key)) / 2);
                    }
                }
                ldaLearner.score(trainData, weightsMap);
            }
//            cleanWeightsMap(weightsMap);
//            System.out.println(JSONArray.toJSONString(weightsMap));
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
            List<SimpleFeatureScores> featureScoresList = AirusUtil.findTopFeatureScores(scores, ScoreType.WeightedTotalScore.getTypeName(), false);
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

        List<SimpleFeatureScores> topTargetPeaks = AirusUtil.findTopFeatureScores(trainData.getTargets(), usedScoreType, true);
        List<SimpleFeatureScores> topDecoyPeaks = AirusUtil.findTopFeatureScores(trainData.getDecoys(), usedScoreType, false);

        Double cutoffNew;
        if (topTargetPeaks.size() < 100){
            Double decoyMax = Double.MIN_VALUE, targetMax = Double.MIN_VALUE;
            for (SimpleFeatureScores scores: topDecoyPeaks){
                if (scores.getMainScore() > decoyMax){
                    decoyMax = scores.getMainScore();
                }
            }
            for (SimpleFeatureScores scores: topTargetPeaks){
                if (scores.getMainScore() > targetMax){
                    targetMax = scores.getMainScore();
                }
            }
            cutoffNew = (decoyMax + targetMax)/2;
        }else {
            // find cutoff fdr from scores and only use best target peaks:
            cutoffNew = stats.findCutoff(topTargetPeaks, topDecoyPeaks, airusParams, cutoff);
        }
        List<SimpleFeatureScores> bestTargetPeaks = AirusUtil.peaksFilter(topTargetPeaks, cutoffNew);

        TrainPeaks trainPeaks = new TrainPeaks();
        trainPeaks.setBestTargets(bestTargetPeaks);
        trainPeaks.setTopDecoys(topDecoyPeaks);
        System.out.println(topTargetPeaks.size() + " " + topDecoyPeaks.size() + " " + cutoffNew + " " + bestTargetPeaks.size());
        return trainPeaks;
    }

    private TrainPeaks selectFirstTrainPeaks(TrainData trainData){
        List<SimpleFeatureScores> decoyPeaks = new ArrayList<>();
        for (SimpleScores simpleScores: trainData.getDecoys()){
            for (FeatureScores featureScores : simpleScores.getFeatureScoresList()){
                SimpleFeatureScores simpleFeatureScores = new SimpleFeatureScores();
                simpleFeatureScores.setScoresMap(featureScores.getScoresMap());
                decoyPeaks.add(simpleFeatureScores);
            }
        }
        TrainPeaks trainPeaks = new TrainPeaks();
        trainPeaks.setTopDecoys(decoyPeaks);
        HashMap<String, Double> bestScoreMap = new HashMap<>();
        bestScoreMap.put(ScoreType.XcorrShape.getTypeName(), 1d);
        bestScoreMap.put(ScoreType.XcorrShapeWeighted.getTypeName(), 1d);
        bestScoreMap.put(ScoreType.XcorrCoelution.getTypeName(), 0d);
        bestScoreMap.put(ScoreType.XcorrCoelutionWeighted.getTypeName(), 0d);
        bestScoreMap.put(ScoreType.LibraryCorr.getTypeName(), 1d);
        bestScoreMap.put(ScoreType.LibraryRsmd.getTypeName(), 0d);
        bestScoreMap.put(ScoreType.LibraryManhattan.getTypeName(), 0d);
        bestScoreMap.put(ScoreType.LibraryDotprod.getTypeName(), 1d);
        bestScoreMap.put(ScoreType.LibrarySangle.getTypeName(), 0d);
        bestScoreMap.put(ScoreType.LibraryRootmeansquare.getTypeName(), 0d);
        bestScoreMap.put(ScoreType.LogSnScore.getTypeName(), 5d);
        bestScoreMap.put(ScoreType.NormRtScore.getTypeName(), 0d);
        bestScoreMap.put(ScoreType.IntensityScore.getTypeName(), 1d);
        bestScoreMap.put(ScoreType.IsotopeCorrelationScore.getTypeName(), 1d);
        bestScoreMap.put(ScoreType.IsotopeOverlapScore.getTypeName(), 0d);
        bestScoreMap.put(ScoreType.MassdevScore.getTypeName(), 0d);
        bestScoreMap.put(ScoreType.MassdevScoreWeighted.getTypeName(), 0d);
        bestScoreMap.put(ScoreType.BseriesScore.getTypeName(), 4d);
        bestScoreMap.put(ScoreType.YseriesScore.getTypeName(), 10d);
        bestScoreMap.put(ScoreType.NewScore.getTypeName(), 0d);
        SimpleFeatureScores bestTargetScore = new SimpleFeatureScores();
        bestTargetScore.setScoresMap(bestScoreMap);
        List<SimpleFeatureScores> bestTargets = new ArrayList<>();
        bestTargets.add(bestTargetScore);
        trainPeaks.setBestTargets(bestTargets);
        return trainPeaks;
    }

    private void cleanWeightsMap(HashMap<String, Double> weightsMap){
        String[] positive = new String[]{
                ScoreType.XcorrShape.getTypeName(),
                ScoreType.XcorrShapeWeighted.getTypeName(),
                ScoreType.LibraryCorr.getTypeName(),
                ScoreType.LibraryDotprod.getTypeName(),
                ScoreType.BseriesScore.getTypeName(),
                ScoreType.YseriesScore.getTypeName(),
                ScoreType.IntensityScore.getTypeName(),
                ScoreType.IsotopeCorrelationScore.getTypeName(),
                ScoreType.LogSnScore.getTypeName()
        };
        String[] negative = new String[]{
                ScoreType.IsotopeOverlapScore.getTypeName(),
                ScoreType.MassdevScore.getTypeName(),
                ScoreType.MassdevScoreWeighted.getTypeName(),
                ScoreType.LibraryRsmd.getTypeName(),
                ScoreType.NormRtScore.getTypeName(),
                ScoreType.XcorrCoelution.getTypeName(),
                ScoreType.XcorrCoelutionWeighted.getTypeName(),
                ScoreType.LibraryManhattan.getTypeName(),
                ScoreType.LibrarySangle.getTypeName(),
                ScoreType.LibraryRootmeansquare.getTypeName()
        };
        for (String key: positive){
            if (weightsMap.get(key) < 0){
                weightsMap.put(key, -weightsMap.get(key));
            }
        }
        for (String key: negative){
            if (weightsMap.get(key) > 0){
                weightsMap.put(key, -weightsMap.get(key));
            }
        }
    }
}
