package com.westlake.air.propro.algorithm.learner;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.westlake.air.propro.algorithm.learner.classifier.LDAClassifier;
import com.westlake.air.propro.algorithm.learner.classifier.XGBoostClassifier;
import com.westlake.air.propro.constants.Classifier;
import com.westlake.air.propro.constants.Constants;
import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.constants.ScoreType;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.airus.*;
import com.westlake.air.propro.domain.bean.score.FeatureScores;
import com.westlake.air.propro.domain.bean.score.SimpleFeatureScores;
import com.westlake.air.propro.domain.db.AnalyseOverviewDO;
import com.westlake.air.propro.domain.db.simple.SimpleScores;
import com.westlake.air.propro.service.AnalyseDataService;
import com.westlake.air.propro.service.AnalyseOverviewService;
import com.westlake.air.propro.service.ScoreService;
import com.westlake.air.propro.utils.AirusUtil;
import com.westlake.air.propro.utils.ArrayUtil;
import com.westlake.air.propro.utils.MathUtil;
import com.westlake.air.propro.utils.SortUtil;
import ml.dmlc.xgboost4j.java.Booster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-19 09:25
 */
@Component
public class SemiSupervise {

    public final Logger logger = LoggerFactory.getLogger(SemiSupervise.class);

    @Autowired
    LDAClassifier ldaClassifier;
    @Autowired
    Stats stats;
    @Autowired
    ScoreService scoreService;
    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    XGBoostClassifier xgboostClassifier;

    public FinalResult doSemiSupervise(String overviewId, AirusParams airusParams) {
        FinalResult finalResult = new FinalResult();
        logger.info("开始处理已识别的肽段数目");
        ResultDO<AnalyseOverviewDO> overviewDOResultDO = analyseOverviewService.getById(overviewId);
        if (overviewDOResultDO.isFailed()) {
            finalResult.setErrorInfo(ResultCode.ANALYSE_OVERVIEW_NOT_EXISTED.getMessage());
            return finalResult;
        }
        AnalyseOverviewDO overviewDO = overviewDOResultDO.getModel();
        overviewDO.setClassifier(airusParams.getClassifier().name());
        if (overviewDO.getMatchedPeptideCount() != null) {
            overviewDO.setMatchedPeptideCount(null);
        }
        analyseOverviewService.update(overviewDO);
        String type = overviewDO.getType();
        logger.info("开始获取打分数据");

        if (airusParams.getScoreTypes() == null) {
            airusParams.setScoreTypes(overviewDO.getScoreTypes());
        }
        List<SimpleScores> scores = analyseDataService.getSimpleScoresByOverviewId(overviewId);
        ResultDO resultDO = checkData(scores);
        if (resultDO.isFailed()) {
            finalResult.setErrorInfo(resultDO.getMsgInfo());
            return finalResult;
        }
        if (type.equals(Constants.EXP_TYPE_PRM)) {
            cleanScore(scores, overviewDO.getScoreTypes());
        }
        HashMap<String, Double> weightsMap = new HashMap<>();
        HashMap<String, Integer> peptideHitMap = new HashMap<>();
        switch (airusParams.getClassifier()) {
            case lda:
                weightsMap = LDALearn(scores, peptideHitMap, airusParams, overviewDO.getScoreTypes(), type);
                ldaClassifier.score(scores, weightsMap, airusParams.getScoreTypes());
                finalResult.setWeightsMap(weightsMap);
                break;

            case xgboost:
                XGBLearn(scores, overviewDO.getScoreTypes(), airusParams);
                break;

            default:
                break;
        }

        List<SimpleFeatureScores> featureScoresList = AirusUtil.findTopFeatureScores(scores, ScoreType.WeightedTotalScore.getTypeName(), overviewDO.getScoreTypes(), false);
        int hit = 0, count = 0;
        if (type.equals(Constants.EXP_TYPE_PRM)) {
            double maxDecoy = Double.MIN_VALUE;
            for (SimpleFeatureScores simpleFeatureScores : featureScoresList) {
                if (simpleFeatureScores.getIsDecoy() && simpleFeatureScores.getMainScore() > maxDecoy) {
                    maxDecoy = simpleFeatureScores.getMainScore();
                }
            }
            for (SimpleFeatureScores simpleFeatureScores : featureScoresList) {
                if (!simpleFeatureScores.getIsDecoy() && simpleFeatureScores.getMainScore() > maxDecoy && simpleFeatureScores.getThresholdPassed()) {
                    simpleFeatureScores.setFdr(0d);
                    count++;
                } else {
                    simpleFeatureScores.setFdr(1d);
                }
            }

        } else {
            ErrorStat errorStat = stats.errorStatistics(featureScoresList, airusParams);
            finalResult.setAllInfo(errorStat);
            count = AirusUtil.checkFdr(finalResult);
        }
        //对于最终的打分结果和选峰结果保存到数据库中
        logger.info("将合并打分及定量结果反馈更新到数据库中,总计:" + featureScoresList.size() + "条数据");
        giveDecoyFdr(featureScoresList);
        long start = System.currentTimeMillis();
        analyseDataService.removeMultiDecoy(overviewId, featureScoresList, airusParams.getFdr());
        logger.info("删除无用数据一共用时：" + (System.currentTimeMillis() - start) + "毫秒");
        start = System.currentTimeMillis();
        analyseDataService.updateMulti(overviewDO.getId(), featureScoresList);
        logger.info("更新数据" + featureScoresList.size() + "条一共用时：" + (System.currentTimeMillis() - start));

        for (SimpleFeatureScores simpleFeatureScores : featureScoresList) {
            if (!simpleFeatureScores.getIsDecoy()) {
                //投票策略
                if (simpleFeatureScores.getFdr() <= airusParams.getFdr()) {
                    Integer hitCount = peptideHitMap.get(simpleFeatureScores.getPeptideRef());
                    if (hitCount != null && hitCount >= airusParams.getTrainTimes() / 2) {
                        hit++;
                    }
                }
            }
        }

        logger.info("采用加权法获得的肽段数目为:" + hit + ",打分反馈更新完毕");
        finalResult.setMatchedPeptideCount(count);
        overviewDO.setWeights(weightsMap);
        overviewDO.setMatchedPeptideCount(count);
        analyseOverviewService.update(overviewDO);

        logger.info("合并打分完成,共找到新肽段" + count + "个");
        return finalResult;
    }

    private ResultDO checkData(List<SimpleScores> scores) {
        boolean isAllDecoy = true;
        boolean isAllReal = true;
        for (SimpleScores score : scores) {
            if (score.getIsDecoy()) {
                isAllReal = false;
            } else {
                isAllDecoy = false;
            }
        }
        if (isAllDecoy) {
            return ResultDO.buildError(ResultCode.ALL_SCORE_DATA_ARE_DECOY);
        }
        if (isAllReal) {
            return ResultDO.buildError(ResultCode.ALL_SCORE_DATA_ARE_REAL);
        }
        return new ResultDO(true);
    }

    /**
     * @param scores
     * @param peptideHitMap
     * @param airusParams
     * @param type          实验类型,PRM还是其他
     * @return
     */
    public HashMap<String, Double> LDALearn(List<SimpleScores> scores, HashMap<String, Integer> peptideHitMap, AirusParams airusParams, List<String> scoreTypes, String type) {
        logger.info("开始训练学习数据权重");
        if (scores.size() < 500) {
            airusParams.setXevalNumIter(10);
            airusParams.setSsIterationFdr(0.02);
            airusParams.setProgressiveRate(0.8);
        }
        int neval = airusParams.getTrainTimes();
        List<HashMap<String, Double>> weightsMapList = new ArrayList<>();
        for (int i = 0; i < neval; i++) {
            logger.info("开始第" + i + "轮尝试,总计" + neval + "轮");
            LDALearnData ldaLearnData = learnRandomized(scores, airusParams);
            if (ldaLearnData == null) {
                logger.info("跳过本轮训练");
                continue;
            }
            ldaClassifier.score(scores, ldaLearnData.getWeightsMap(), scoreTypes);
            List<SimpleFeatureScores> featureScoresList = AirusUtil.findTopFeatureScores(scores, ScoreType.WeightedTotalScore.getTypeName(), scoreTypes, false);
            int count = 0;
            if (type.equals(Constants.EXP_TYPE_PRM)) {
                double maxDecoy = Double.MIN_VALUE;
                for (SimpleFeatureScores simpleFeatureScores : featureScoresList) {
                    if (simpleFeatureScores.getIsDecoy() && simpleFeatureScores.getMainScore() > maxDecoy) {
                        maxDecoy = simpleFeatureScores.getMainScore();
                    }
                }
                for (SimpleFeatureScores simpleFeatureScores : featureScoresList) {
                    if (!simpleFeatureScores.getIsDecoy() && simpleFeatureScores.getMainScore() > maxDecoy && simpleFeatureScores.getThresholdPassed()) {
                        count++;
                        simpleFeatureScores.setFdr(0d);
                    } else {
                        simpleFeatureScores.setFdr(1d);
                    }
                }
            } else {
                ErrorStat errorStat = stats.errorStatistics(featureScoresList, airusParams);
                count = AirusUtil.checkFdr(errorStat.getStatMetrics().getFdr());
            }
            for (SimpleFeatureScores simpleFeatureScores : featureScoresList) {
                if (!simpleFeatureScores.getIsDecoy() && simpleFeatureScores.getFdr() <= 0.01) {
                    Integer hitCount = peptideHitMap.get(simpleFeatureScores.getPeptideRef());
                    if (hitCount == null) {
                        hitCount = 0;
                    }
                    hitCount++;
                    peptideHitMap.put(simpleFeatureScores.getPeptideRef(), hitCount);
                }
            }

            if (count > 0) {
                logger.info("本轮尝试有效果:检测结果:" + count + "个");
            }
            weightsMapList.add(ldaLearnData.getWeightsMap());
            if (airusParams.isDebug()) {
                break;
            }
        }

        return AirusUtil.averagedWeights(weightsMapList);
    }

    public void XGBLearn(List<SimpleScores> scores, List<String> scoreTypes, AirusParams airusParams) {
        logger.info("开始训练Booster");
        Booster booster = learnRandomizedXGB(scores, airusParams);
        try {
            logger.info("开始最终打分");
            xgboostClassifier.predictAll(booster, scores, ScoreType.MainScore.getTypeName(), airusParams.getScoreTypes());
        } catch (Exception e) {
            logger.error("XGBooster Predict All Fail.\n");
            e.printStackTrace();
        }
        List<SimpleFeatureScores> featureScoresList = AirusUtil.findTopFeatureScores(scores, ScoreType.WeightedTotalScore.getTypeName(), scoreTypes, false);
        ErrorStat errorStat = stats.errorStatistics(featureScoresList, airusParams);
        int count = AirusUtil.checkFdr(errorStat.getStatMetrics().getFdr());
        if (count > 0) {
            logger.info("XGBooster:检测结果:" + count + "个.");
        }
    }

    public LDALearnData learnRandomized(List<SimpleScores> scores, AirusParams airusParams) {
        LDALearnData ldaLearnData = new LDALearnData();
        try {
            //Get part of scores as train input.
            TrainData trainData = AirusUtil.split(scores, airusParams.getTrainTestRatio(), airusParams.isDebug(), airusParams.getScoreTypes());

            //第一次训练数据集使用指定的主分数(默认为MainScore)进行训练
            //TrainPeaks trainPeaks = selectTrainPeaks(trainData, airusParams.getMainScore(), airusParams, airusParams.getSsInitialFdr());
            TrainPeaks trainPeaks = selectFirstTrainPeaks(trainData, airusParams);

            HashMap<String, Double> weightsMap = ldaClassifier.learn(trainPeaks, airusParams.getMainScore(), airusParams.getScoreTypes());
            logger.info("Train Weight:" + JSONArray.toJSONString(weightsMap));

            //根据weightsMap计算子分数的加权总分
            ldaClassifier.score(trainData, weightsMap, airusParams.getScoreTypes());
            weightsMap = new HashMap<>();
            HashMap<String, Double> lastWeightsMap = new HashMap<>();
            for (int times = 0; times < airusParams.getXevalNumIter(); times++) {
                TrainPeaks trainPeaksTemp = selectTrainPeaks(trainData, ScoreType.WeightedTotalScore.getTypeName(), airusParams, airusParams.getSsIterationFdr());
                lastWeightsMap = weightsMap;
                weightsMap = ldaClassifier.learn(trainPeaksTemp, ScoreType.WeightedTotalScore.getTypeName(), airusParams.getScoreTypes());
                logger.info("Train Weight:" + JSONArray.toJSONString(weightsMap));
                for (Double value : weightsMap.values()) {
                    if (value == null || Double.isNaN(value)) {
                        logger.info("本轮训练一坨屎:" + JSON.toJSONString(weightsMap));
                        continue;
                    }
                }
                if (lastWeightsMap.size() != 0) {
                    for (String key : weightsMap.keySet()) {
                        weightsMap.put(key, weightsMap.get(key) * airusParams.getProgressiveRate() + lastWeightsMap.get(key) * (1d - airusParams.getProgressiveRate()));
                    }
                }
                ldaClassifier.score(trainData, weightsMap, airusParams.getScoreTypes());
            }
            ldaLearnData.setWeightsMap(weightsMap);
            return ldaLearnData;
        } catch (Exception e) {
            logger.error("learnRandomized Fail.\n");
            e.printStackTrace();
            return null;
        }

    }

    public Booster learnRandomizedXGB(List<SimpleScores> scores, AirusParams airusParams) {
        try {
            //Get part of scores as train input.
            TrainData trainData = AirusUtil.split(scores, airusParams.getTrainTestRatio(), airusParams.isDebug(), airusParams.getScoreTypes());
            //第一次训练数据集使用MainScore进行训练
            long startTime = System.currentTimeMillis();
            TrainPeaks trainPeaks = selectTrainPeaks(trainData, airusParams.getMainScore(), airusParams, airusParams.getSsInitialFdr());
            logger.info("高可信Target个数：" + trainPeaks.getBestTargets().size());
            Booster booster = xgboostClassifier.train(trainPeaks, airusParams.getMainScore(), airusParams.getScoreTypes());
            xgboostClassifier.predict(booster, trainData, airusParams.getMainScore(), airusParams.getScoreTypes());
            for (int times = 0; times < airusParams.getXevalNumIter(); times++) {
                logger.info("开始第" + times + "轮训练");
                TrainPeaks trainPeaksTemp = selectTrainPeaks(trainData, ScoreType.WeightedTotalScore.getTypeName(), airusParams, airusParams.getXgbIterationFdr());
                logger.info("高可信Target个数：" + trainPeaksTemp.getBestTargets().size());
                booster = xgboostClassifier.train(trainPeaksTemp, ScoreType.WeightedTotalScore.getTypeName(), airusParams.getScoreTypes());
                xgboostClassifier.predict(booster, trainData, ScoreType.WeightedTotalScore.getTypeName(), airusParams.getScoreTypes());
            }
            logger.info("总时间：" + (System.currentTimeMillis() - startTime));
            List<SimpleFeatureScores> featureScoresList = AirusUtil.findTopFeatureScores(scores, ScoreType.WeightedTotalScore.getTypeName(), airusParams.getScoreTypes(), false);
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

    private void giveDecoyFdr(List<SimpleFeatureScores> featureScoresList) {

        List<SimpleFeatureScores> sortedAll = SortUtil.sortByMainScore(featureScoresList, false);
        SimpleFeatureScores leftFeatureScore = null;
        SimpleFeatureScores rightFeatureScore;
        List<SimpleFeatureScores> decoyPartList = new ArrayList<>();
        for (SimpleFeatureScores simpleFeatureScores : sortedAll) {
            if (simpleFeatureScores.getIsDecoy()) {
                decoyPartList.add(simpleFeatureScores);
            } else {
                rightFeatureScore = simpleFeatureScores;
                if (leftFeatureScore != null && !decoyPartList.isEmpty()) {
                    for (SimpleFeatureScores decoy : decoyPartList) {
                        if (decoy.getMainScore() - leftFeatureScore.getMainScore() < rightFeatureScore.getMainScore() - decoy.getMainScore()) {
                            decoy.setFdr(leftFeatureScore.getFdr());
                            decoy.setQValue(leftFeatureScore.getQValue());
                        } else {
                            decoy.setFdr(rightFeatureScore.getFdr());
                            decoy.setQValue(rightFeatureScore.getQValue());
                        }
                    }
                }
                leftFeatureScore = rightFeatureScore;
                decoyPartList.clear();
            }
        }
        if (leftFeatureScore != null && !decoyPartList.isEmpty()) {
            for (SimpleFeatureScores decoy : decoyPartList) {
                decoy.setFdr(leftFeatureScore.getFdr());
                decoy.setQValue(leftFeatureScore.getQValue());
            }
        }
    }

    private void cleanScore(List<SimpleScores> scoresList, List<String> scoreTypes) {
        for (SimpleScores simpleScores : scoresList) {
            if (simpleScores.getIsDecoy()) {
                continue;
            }
            for (FeatureScores featureScores : simpleScores.getFeatureScoresList()) {
                int count = 0;
                if (featureScores.get(ScoreType.NormRtScore.getTypeName(), scoreTypes) != null && featureScores.get(ScoreType.NormRtScore.getTypeName(), scoreTypes) > 8) {
                    count++;
                }
                if (featureScores.get(ScoreType.LogSnScore.getTypeName(), scoreTypes) != null && featureScores.get(ScoreType.LogSnScore.getTypeName(), scoreTypes) < 3) {
                    count++;
                }
                if (featureScores.get(ScoreType.IsotopeCorrelationScore.getTypeName(), scoreTypes) != null && featureScores.get(ScoreType.IsotopeCorrelationScore.getTypeName(), scoreTypes) < 0.8) {
                    count++;
                }
                if (featureScores.get(ScoreType.IsotopeOverlapScore.getTypeName(), scoreTypes) != null && featureScores.get(ScoreType.IsotopeOverlapScore.getTypeName(), scoreTypes) > 0.2) {
                    count++;
                }
                if (featureScores.get(ScoreType.MassdevScoreWeighted.getTypeName(), scoreTypes) != null && featureScores.get(ScoreType.MassdevScoreWeighted.getTypeName(), scoreTypes) > 15) {
                    count++;
                }
                if (featureScores.get(ScoreType.BseriesScore.getTypeName(), scoreTypes) != null && featureScores.get(ScoreType.BseriesScore.getTypeName(), scoreTypes) < 1) {
                    count++;
                }
                if (featureScores.get(ScoreType.YseriesScore.getTypeName(), scoreTypes) != null && featureScores.get(ScoreType.YseriesScore.getTypeName(), scoreTypes) < 5) {
                    count++;
                }
                if (featureScores.get(ScoreType.XcorrShapeWeighted.getTypeName(), scoreTypes) != null && featureScores.get(ScoreType.XcorrShapeWeighted.getTypeName(), scoreTypes) < 0.6) {
                    count++;
                }
                if (featureScores.get(ScoreType.XcorrShape.getTypeName(), scoreTypes) != null && featureScores.get(ScoreType.XcorrShape.getTypeName(), scoreTypes) < 0.5) {
                    count++;
                }

                if (count > 3) {
                    featureScores.setThresholdPassed(false);
                }
            }
        }
    }

    private TrainPeaks selectTrainPeaks(TrainData trainData, String usedScoreType, AirusParams airusParams, Double cutoff) {

        List<SimpleFeatureScores> topTargetPeaks = AirusUtil.findTopFeatureScores(trainData.getTargets(), usedScoreType, airusParams.getScoreTypes(), true);
        List<SimpleFeatureScores> topDecoyPeaks = AirusUtil.findTopFeatureScores(trainData.getDecoys(), usedScoreType, airusParams.getScoreTypes(), false);

        Double cutoffNew;
        if (topTargetPeaks.size() < 100) {
            Double decoyMax = Double.MIN_VALUE, targetMax = Double.MIN_VALUE;
            for (SimpleFeatureScores scores : topDecoyPeaks) {
                if (scores.getMainScore() > decoyMax) {
                    decoyMax = scores.getMainScore();
                }
            }
            for (SimpleFeatureScores scores : topTargetPeaks) {
                if (scores.getMainScore() > targetMax) {
                    targetMax = scores.getMainScore();
                }
            }
            cutoffNew = (decoyMax + targetMax) / 2;
        } else {
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

    private TrainPeaks selectFirstTrainPeaks(TrainData trainData, AirusParams airusParams) {
        List<SimpleFeatureScores> decoyPeaks = new ArrayList<>();
        List<String> scoreTypes = airusParams.getScoreTypes();
        for (SimpleScores simpleScores : trainData.getDecoys()) {
            FeatureScores topDecoy = null;
            double maxMainScore = -Double.MAX_VALUE;
            for (FeatureScores featureScores : simpleScores.getFeatureScoresList()) {
                double mainScore = featureScores.get(ScoreType.MainScore.getTypeName(), scoreTypes);
                if (mainScore > maxMainScore) {
                    maxMainScore = mainScore;
                    topDecoy = featureScores;
                }
            }
            SimpleFeatureScores simpleFeatureScores = new SimpleFeatureScores();
            simpleFeatureScores.setScores(topDecoy.getScores());
            decoyPeaks.add(simpleFeatureScores);
        }
        TrainPeaks trainPeaks = new TrainPeaks();
        trainPeaks.setTopDecoys(decoyPeaks);

        SimpleFeatureScores bestTargetScore = new SimpleFeatureScores(airusParams.getScoreTypes().size());
        bestTargetScore.put(ScoreType.XcorrShape.getTypeName(), 1d, scoreTypes);
        bestTargetScore.put(ScoreType.XcorrShapeWeighted.getTypeName(), 1d, scoreTypes);
        bestTargetScore.put(ScoreType.XcorrCoelution.getTypeName(), 0d, scoreTypes);
        bestTargetScore.put(ScoreType.XcorrCoelutionWeighted.getTypeName(), 0d, scoreTypes);
        bestTargetScore.put(ScoreType.LibraryCorr.getTypeName(), 1d, scoreTypes);
        bestTargetScore.put(ScoreType.LibraryRsmd.getTypeName(), 0d, scoreTypes);
        bestTargetScore.put(ScoreType.LibraryManhattan.getTypeName(), 0d, scoreTypes);
        bestTargetScore.put(ScoreType.LibraryDotprod.getTypeName(), 1d, scoreTypes);
        bestTargetScore.put(ScoreType.LibrarySangle.getTypeName(), 0d, scoreTypes);
        bestTargetScore.put(ScoreType.LibraryRootmeansquare.getTypeName(), 0d, scoreTypes);
        bestTargetScore.put(ScoreType.LogSnScore.getTypeName(), 5d, scoreTypes);
        bestTargetScore.put(ScoreType.NormRtScore.getTypeName(), 0d, scoreTypes);
        bestTargetScore.put(ScoreType.IntensityScore.getTypeName(), 1d, scoreTypes);
        bestTargetScore.put(ScoreType.IsotopeCorrelationScore.getTypeName(), 1d, scoreTypes);
        bestTargetScore.put(ScoreType.IsotopeOverlapScore.getTypeName(), 0d, scoreTypes);
        bestTargetScore.put(ScoreType.MassdevScore.getTypeName(), 0d, scoreTypes);
        bestTargetScore.put(ScoreType.MassdevScoreWeighted.getTypeName(), 0d, scoreTypes);
        bestTargetScore.put(ScoreType.BseriesScore.getTypeName(), 4d, scoreTypes);
        bestTargetScore.put(ScoreType.YseriesScore.getTypeName(), 10d, scoreTypes);


        List<SimpleFeatureScores> bestTargets = new ArrayList<>();
        bestTargets.add(bestTargetScore);
        trainPeaks.setBestTargets(bestTargets);
        return trainPeaks;
    }
}
