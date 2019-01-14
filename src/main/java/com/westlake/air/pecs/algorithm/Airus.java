package com.westlake.air.pecs.algorithm;

import com.westlake.air.pecs.algorithm.learner.LDALearner;
import com.westlake.air.pecs.algorithm.learner.XGBoostLearner;
import com.westlake.air.pecs.constants.Classifier;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.constants.ScoreType;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.airus.*;
import com.westlake.air.pecs.domain.bean.score.SimpleFeatureScores;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.AnalyseOverviewDO;
import com.westlake.air.pecs.domain.db.simple.SimpleScores;
import com.westlake.air.pecs.service.AnalyseDataService;
import com.westlake.air.pecs.service.AnalyseOverviewService;
import com.westlake.air.pecs.service.ScoreService;
import com.westlake.air.pecs.utils.AirusUtil;
import com.westlake.air.pecs.utils.ArrayUtil;
import com.westlake.air.pecs.utils.MathUtil;
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
public class Airus {

    public final Logger logger = LoggerFactory.getLogger(Airus.class);

    @Autowired
    SemiSupervised semiSupervised;
    @Autowired
    LDALearner ldaLearner;
    @Autowired
    Stats stats;
    @Autowired
    ScoreService scoreService;
    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    XGBoostLearner xgBoostLearner;

    public FinalResult doAirus(String overviewId, AirusParams airusParams) {
        FinalResult finalResult = new FinalResult();
        logger.info("开始清理已识别的肽段数目");
        ResultDO<AnalyseOverviewDO> overviewDOResultDO = analyseOverviewService.getById(overviewId);
        if (overviewDOResultDO.isSuccess()) {
            AnalyseOverviewDO overviewDO = overviewDOResultDO.getModel();
            overviewDO.setClassifier(airusParams.getClassifier().name());
            if (overviewDO.getMatchedPeptideCount() != null) {
                overviewDO.setMatchedPeptideCount(null);
            }
            analyseOverviewService.update(overviewDO);
        }
        logger.info("开始获取打分数据");

        List<SimpleScores> scores = analyseDataService.getSimpleScoresByOverviewId(overviewId);
        ResultDO resultDO = checkData(scores);
        if (resultDO.isFailed()) {
            finalResult.setErrorInfo(resultDO.getMsgInfo());
            return finalResult;
        }
        HashMap<String, Double> weightsMap = new HashMap<>();
        HashMap<String, Integer> peptideHitMap = new HashMap<>();
        if (airusParams.getClassifier().equals(Classifier.lda)) {
            logger.info("开始训练学习数据权重");
            weightsMap = LDALearn(scores, peptideHitMap, airusParams);
            logger.info("开始计算合并打分");
            ldaLearner.score(scores, weightsMap);
            finalResult.setWeightsMap(weightsMap);
        } else if (airusParams.getClassifier().equals(Classifier.xgboost)) {
//            logger.info("开始训练XGBooster");
            XGBLearn(scores, airusParams);
        }
        List<SimpleFeatureScores> featureScoresList = AirusUtil.findTopFeatureScores(scores, ScoreType.WeightedTotalScore.getTypeName());

        ErrorStat errorStat = stats.errorStatistics(featureScoresList, airusParams);

        finalResult.setAllInfo(errorStat);

        //对于最终的打分结果和选峰结果保存到数据库中
        int hit = 0;
        logger.info("将合并打分及定量结果反馈更新到数据库中,总计:"+featureScoresList+"条数据");
        for (SimpleFeatureScores simpleFeatureScores : featureScoresList) {
            AnalyseDataDO dataDO = analyseDataService.getByOverviewIdAndPeptideRefAndIsDecoy(overviewId, simpleFeatureScores.getPeptideRef(), simpleFeatureScores.getIsDecoy());
            dataDO.setBestRt(simpleFeatureScores.getRt());
            dataDO.setFdr(simpleFeatureScores.getFdr());
            dataDO.setIntensitySum(simpleFeatureScores.getIntensitySum());
            if (!simpleFeatureScores.getIsDecoy()) {
                if (simpleFeatureScores.getFdr() <= 0.01) {
                    Integer count = peptideHitMap.get(simpleFeatureScores.getPeptideRef());
                    if (count != null && count > 4) {
                        hit++;
                    }
                }
                dataDO.setIdentifiedStatus(simpleFeatureScores.getFdr() <= 0.01 ? AnalyseDataDO.IDENTIFIED_STATUS_SUCCESS : AnalyseDataDO.IDENTIFIED_STATUS_UNKNOWN);
            }
            ResultDO r = analyseDataService.update(dataDO);
            if (r.isFailed()) {
                logger.error(r.getMsgInfo());
            }
        }
        logger.info("采用加权法获得的肽段数目为:" + hit);
        logger.info("打分反馈更新完毕");
        int count = AirusUtil.checkFdr(finalResult);
        finalResult.setMatchedPeptideCount(count);
        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);
        if (overviewResult.isSuccess()) {
            AnalyseOverviewDO overviewDO = overviewResult.getModel();
            overviewDO.setWeights(weightsMap);
            overviewDO.setMatchedPeptideCount(count);
            analyseOverviewService.update(overviewDO);
        } else {
            logger.error(overviewResult.getMsgInfo());
        }
        logger.info("合并打分完成,共找到新肽段" + count + "个");
        return finalResult;
    }

    public ResultDO checkData(List<SimpleScores> scores) {
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

    public HashMap<String, Double> LDALearn(List<SimpleScores> scores, HashMap<String, Integer> peptideHitMap, AirusParams airusParams) {
        int neval = airusParams.getTrainTimes();
        List<HashMap<String, Double>> weightsMapList = new ArrayList<>();
        for (int i = 0; i < neval; i++) {
            logger.info("开始第" + i + "轮尝试");
            LDALearnData ldaLearnData = semiSupervised.learnRandomized(scores, airusParams);
            if (ldaLearnData == null) {
                logger.info("跳过本轮训练");
                continue;
            }
            ldaLearner.score(scores, ldaLearnData.getWeightsMap());
            List<SimpleFeatureScores> featureScoresList = AirusUtil.findTopFeatureScores(scores, ScoreType.WeightedTotalScore.getTypeName());
            ErrorStat errorStat = stats.errorStatistics(featureScoresList, airusParams);
            for (SimpleFeatureScores simpleFeatureScores : featureScoresList) {
                if (!simpleFeatureScores.getIsDecoy() && simpleFeatureScores.getFdr() <= 0.01) {
                    Integer count = peptideHitMap.get(simpleFeatureScores.getPeptideRef());
                    if (count == null) {
                        count = 0;
                    }
                    count++;
                    peptideHitMap.put(simpleFeatureScores.getPeptideRef(), count);
                }
            }
            int count = AirusUtil.checkFdr(errorStat.getStatMetrics().getFdr());
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

    public void XGBLearn(List<SimpleScores> scores, AirusParams airusParams) {
        logger.info("开始训练Booster");
        Booster booster = semiSupervised.learnRandomizedXGB(scores, airusParams);
        try {
            logger.info("开始最终打分");
            xgBoostLearner.predictAll(booster, scores, ScoreType.MainScore.getTypeName());
        } catch (Exception e) {
            logger.error("XGBooster Predict All Fail.\n");
            e.printStackTrace();
        }
        List<SimpleFeatureScores> featureScoresList = AirusUtil.findTopFeatureScores(scores, ScoreType.WeightedTotalScore.getTypeName());
        ErrorStat errorStat = stats.errorStatistics(featureScoresList, airusParams);
        int count = AirusUtil.checkFdr(errorStat.getStatMetrics().getFdr());
        if (count > 0) {
            logger.info("XGBooster:检测结果:" + count + "个.");
        }
    }


    private ErrorStat finalErrorTable(ErrorStat errorStat, AirusParams airusParams) {

        StatMetrics statMetrics = errorStat.getStatMetrics();
        Double[] cutOffs = errorStat.getCutoff();
        Double[] cutOffsSort = cutOffs.clone();
        Arrays.sort(cutOffsSort);
        double minCutOff = cutOffsSort[0];
        double maxCutOff = cutOffsSort[cutOffs.length - 1];
        double margin = (maxCutOff - minCutOff) * 0.05;
        Double[] sampledCutoffs = MathUtil.linspace(minCutOff - margin, maxCutOff - margin, airusParams.getNumCutOffs());
        Integer[] ix = MathUtil.findNearestMatches(cutOffs, sampledCutoffs, airusParams.getUseSortOrders());

        ErrorStat sampleErrorStat = new ErrorStat();
        sampleErrorStat.setCutoff(sampledCutoffs);
        sampleErrorStat.setQvalue(ArrayUtil.extractRow(errorStat.getQvalue(), ix));
        sampleErrorStat.setPvalue(ArrayUtil.extractRow(errorStat.getPvalue(), ix));

        StatMetrics sampleStatMatric = new StatMetrics();
        sampleStatMatric.setSvalue(ArrayUtil.extractRow(statMetrics.getSvalue(), ix));
        sampleStatMatric.setFn(ArrayUtil.extractRow(statMetrics.getFn(), ix));
        sampleStatMatric.setFnr(ArrayUtil.extractRow(statMetrics.getFnr(), ix));
        sampleStatMatric.setFdr(ArrayUtil.extractRow(statMetrics.getFdr(), ix));
        sampleStatMatric.setFp(ArrayUtil.extractRow(statMetrics.getFp(), ix));
        sampleStatMatric.setFpr(ArrayUtil.extractRow(statMetrics.getFpr(), ix));
        sampleStatMatric.setTn(ArrayUtil.extractRow(statMetrics.getTn(), ix));
        sampleStatMatric.setTp(ArrayUtil.extractRow(statMetrics.getTp(), ix));

        sampleErrorStat.setStatMetrics(sampleStatMatric);

        return sampleErrorStat;
    }

    private ErrorStat summaryErrorTable(ErrorStat errorStat, AirusParams airusParams) {
        StatMetrics statMetrics = errorStat.getStatMetrics();
        Double[] qvalues = airusParams.getQvalues();
        Integer[] ix = MathUtil.findNearestMatches(errorStat.getQvalue(), qvalues, airusParams.getUseSortOrders());

        ErrorStat subErrorStat = new ErrorStat();
        subErrorStat.setCutoff(ArrayUtil.extractRow(errorStat.getCutoff(), ix));
        subErrorStat.setQvalue(qvalues);
        subErrorStat.setPvalue(ArrayUtil.extractRow(errorStat.getPvalue(), ix));

        StatMetrics subStatMatric = new StatMetrics();
        subStatMatric.setSvalue(ArrayUtil.extractRow(statMetrics.getSvalue(), ix));
        subStatMatric.setFn(ArrayUtil.extractRow(statMetrics.getFn(), ix));
        subStatMatric.setFnr(ArrayUtil.extractRow(statMetrics.getFnr(), ix));
        subStatMatric.setFdr(ArrayUtil.extractRow(statMetrics.getFdr(), ix));
        subStatMatric.setFp(ArrayUtil.extractRow(statMetrics.getFp(), ix));
        subStatMatric.setFpr(ArrayUtil.extractRow(statMetrics.getFpr(), ix));
        subStatMatric.setTn(ArrayUtil.extractRow(statMetrics.getTn(), ix));
        subStatMatric.setTp(ArrayUtil.extractRow(statMetrics.getTp(), ix));

        subErrorStat.setStatMetrics(subStatMatric);

        return subErrorStat;
    }

    /**
     * Dscore: Normalize clfScores with clfScores' TopDecoyPeaks.
     */
    private Double[] calculateDscore(Double[] weights, ScoreData scoreData) {
        Double[][] scores = AirusUtil.getFeatureMatrix(scoreData.getScoreData(), true);
        Double[] classifierScore = MathUtil.dot(scores, weights);
        Double[] classifierTopDecoyPeaks = AirusUtil.getTopDecoyPeaks(classifierScore, scoreData.getIsDecoy(), AirusUtil.findTopIndex(classifierScore, AirusUtil.getGroupNumId(scoreData.getGroupId())));
        return MathUtil.normalize(classifierScore, classifierTopDecoyPeaks);
    }

    private void fixMainScore(Double[][] scores) {
        for (int i = 0; i < scores.length; i++) {
            logger.info("原始分数:" + scores[i][0]);
            scores[i][0] =
                    scores[i][1] * -0.19011762 +
                            scores[i][2] * 2.47298914 +
                            scores[i][7] * 5.63906731 +
                            scores[i][11] * -0.62640133 +
                            scores[i][12] * 0.36006925 +
                            scores[i][13] * 0.08814003 +
                            scores[i][3] * 0.13978311 +
                            scores[i][5] * -1.16475032 +
                            scores[i][16] * -0.19267813 +
                            scores[i][9] * -0.61712054;
            logger.info("事后分数:" + scores[i][0]);
        }
    }

}
