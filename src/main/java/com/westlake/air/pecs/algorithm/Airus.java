package com.westlake.air.pecs.algorithm;

import com.alibaba.fastjson.JSON;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.airus.*;
import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import com.westlake.air.pecs.domain.bean.score.SimpleFeatureScores;
import com.westlake.air.pecs.domain.db.ScoresDO;
import com.westlake.air.pecs.domain.db.simple.SimpleScores;
import com.westlake.air.pecs.service.ScoresService;
import com.westlake.air.pecs.utils.AirusUtil;
import com.westlake.air.pecs.utils.ArrayUtil;
import com.westlake.air.pecs.utils.MathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.westlake.air.pecs.domain.bean.score.FeatureScores.SCORES_COUNT;

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
    ScoresService scoresService;

    public FinalResult doAirus(String overviewId, Params params) {
        logger.info("开始获取打分数据");
//        List<ScoresDO> scores = scoresService.getAllByOverviewId(overviewId);
//        return doAirus(scores, params);
        List<SimpleScores> scores = scoresService.getSimpleAllByOverviewId(overviewId);
        return doAirusWithDB(overviewId, scores, params);
    }

    public FinalResult doAirus(List<ScoresDO> scores, Params params) {
        logger.info("开始检查数据是否健康");
        ResultDO resultDO = checkData(scores);
        if (resultDO.isFailed()) {
            logger.info("数据异常:" + resultDO.getMsgInfo());
            FinalResult result = new FinalResult();
            result.setErrorInfo(resultDO.getMsgInfo());
            return result;
        }
        logger.info("开始转换打分数据格式");
        ScoreData scoreData = trans(scores);
        return doAirus(scoreData, params);
    }

    public FinalResult doAirusWithDB(String overviewId, List<SimpleScores> scores, Params params) {
        logger.info("开始检查数据是否健康");
        for (SimpleScores score : scores) {
            score.setGroupId(score.getIsDecoy() ? ("DECOY_" + score.getPeptideRef()) : score.getPeptideRef());
        }

        logger.info("开始训练学习数据权重");
        HashMap<String, Double> weightsMap = learn(scores, params);
        logger.info("开始计算合并打分");
        ldaLearner.score(scores, weightsMap);
        List<SimpleFeatureScores> featureScoresList = AirusUtil.findTopFeatureScores(scores, FeatureScores.ScoreType.WeightedTotalScore.getTypeName());

        ErrorStat errorStat = stats.errorStatistics(featureScoresList, params);
        FinalResult finalResult = new FinalResult();
        finalResult.setAllInfo(errorStat);
        finalResult.setWeightsMap(weightsMap);

        //对于最终的打分结果和选峰结果保存到数据库中
        for (SimpleFeatureScores simpleFeatureScores : featureScoresList) {
            ScoresDO scoresDO = scoresService.getByPeptideRefAndIsDecoy(overviewId, simpleFeatureScores.getPeptideRef(), simpleFeatureScores.getIsDecoy());
            scoresDO.setBestRt(simpleFeatureScores.getRt());
            if(!simpleFeatureScores.getIsDecoy()){
                scoresDO.setIsIdentified(simpleFeatureScores.getFdr() <= 0.01);
            }
            scoresService.update(scoresDO);
        }
        return finalResult;
    }

    public FinalResult doAirus(ScoreData scoreData, Params params) {
        if (params.isDebug()) {
            scoreData = AirusUtil.fakeSortTgId(scoreData);
        }
        logger.info("开始训练学习数据权重");
        Double[] weights = learn(scoreData.getScoreData(), scoreData.getGroupNumId(), scoreData.getIsDecoy(), params);
        logger.info("开始计算合并打分");
        Double[] dscore = calculateDscore(weights, scoreData);
        Double[] topTargetDscores = AirusUtil.getTopTargetPeaks(dscore, scoreData.getIsDecoy(), AirusUtil.findTopIndex(dscore, scoreData.getGroupNumId()));
        Double[] topDecoyDscores = AirusUtil.getTopDecoyPeaks(dscore, scoreData.getIsDecoy(), AirusUtil.findTopIndex(dscore, scoreData.getGroupNumId()));
        String[] scoreColumns = FeatureScores.getScoresColumnNames();

        HashMap<String, Double> classifierTable = new HashMap<String, Double>();
        for (int i = 0; i < weights.length; i++) {
            classifierTable.put(scoreColumns[i], weights[i]);
        }
        FinalResult finalResult = new FinalResult();
        finalResult.setWeightsMap(classifierTable);
        ErrorStat errorStat = stats.errorStatistics(topTargetDscores, topDecoyDscores, params);
        finalResult.setFinalErrorTable(finalErrorTable(errorStat, params));
        finalResult.setSummaryErrorTable(summaryErrorTable(errorStat, params));
        finalResult.setAllInfo(errorStat);
        logger.info("合并打分完毕");
        return finalResult;
    }


    public ResultDO checkData(List<ScoresDO> scores) {
        boolean isAllDecoy = true;
        boolean isAllReal = true;
        for (ScoresDO score : scores) {
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

//    public ResultDO checkData(List<SimpleScores> scores) {
//        boolean isAllDecoy = true;
//        boolean isAllReal = true;
//        for (SimpleScores score : scores) {
//            if (score.getIsDecoy()) {
//                isAllReal = false;
//            } else {
//                isAllDecoy = false;
//            }
//        }
//        if (isAllDecoy) {
//            return ResultDO.buildError(ResultCode.ALL_SCORE_DATA_ARE_DECOY);
//        }
//        if (isAllReal) {
//            return ResultDO.buildError(ResultCode.ALL_SCORE_DATA_ARE_REAL);
//        }
//        return new ResultDO(true);
//    }

    public ScoreData trans(List<ScoresDO> scores) {
        ScoreData scoreData = new ScoreData();
        if (scores == null || scores.size() == 0) {
            return null;
        }

        //之后会转化为Decoy
        List<Boolean> isDecoyList = new ArrayList<>();
        //之后会转化为GroupId
        List<String> peptideRefList = new ArrayList<>();
        //之后会转化为FeatureScores
        List<HashMap<String, Double>> scoreList = new ArrayList<>();
        for (ScoresDO score : scores) {
            for (FeatureScores fs : score.getFeatureScoresList()) {
                isDecoyList.add(score.getIsDecoy());
                String peptideRef = score.getPeptideRef();
                if (score.getIsDecoy()) {
                    peptideRef = "DECOY_" + peptideRef;
                }
                peptideRefList.add(peptideRef);
                HashMap<String, Double> scoreMap = fs.getScoresMap();
                scoreList.add(scoreMap);
                for (String key : scoreMap.keySet()) {
                    if (scoreMap.get(key).equals(Double.NaN)) {
                        logger.info("包含空打分肽段名:" + JSON.toJSONString(score.getPeptideRef()));
                        logger.info("Key:" + key);
                    }
                }
            }
        }

        Boolean[] isDecoyArray = new Boolean[isDecoyList.size()];
        isDecoyList.toArray(isDecoyArray);

        String[] groupIds = new String[peptideRefList.size()];
        peptideRefList.toArray(groupIds);
        Integer[] groupNumIds = AirusUtil.getGroupNumId(groupIds);
        Double[][] scoresArray = new Double[peptideRefList.size()][SCORES_COUNT];
        for (int i = 0; i < scoreList.size(); i++) {
            scoresArray[i] = FeatureScores.toArray(scoreList.get(i));
        }

        logger.info("打分数据构造完毕");
        scoreData.setGroupId(groupIds);
        scoreData.setGroupNumId(groupNumIds);
        scoreData.setIsDecoy(isDecoyArray);
        scoreData.setScoreData(scoresArray);
        return scoreData;
    }

    /**
     * Airus入口函数
     *
     * @param scores
     * @param groupNumId
     * @param isDecoy
     * @return
     */
    public Double[] learn(Double[][] scores, Integer[] groupNumId, Boolean[] isDecoy, Params params) {
        int neval = params.getTrainTimes();
        Double[][] weights = new Double[neval][scores[0].length];
        for (int i = 0; i < neval; i++) {
            LDALearnData ldaLearnData = semiSupervised.learnRandomized(scores, groupNumId, isDecoy, params);

            if (params.isDebug()) {
                weights = new Double[1][scores[0].length];
                weights[0] = ldaLearnData.getWeights();
                break;
            } else {
                weights[i] = ldaLearnData.getWeights();
            }
        }
        logger.info("学习完毕");

        return semiSupervised.averagedLearner(weights);
    }

    public HashMap<String, Double> learn(List<SimpleScores> scores, Params params) {
        int neval = params.getTrainTimes();
        List<HashMap<String, Double>> weightsMapList = new ArrayList<>();
        int maxCount = 0;
        for (int i = 0; i < neval; i++) {
            logger.info("开始第" + i + "轮尝试");
            LDALearnData ldaLearnData = semiSupervised.learnRandomized(scores, params);

            ldaLearner.score(scores, ldaLearnData.getWeightsMap());
            List<SimpleFeatureScores> featureScoresList = AirusUtil.findTopFeatureScores(scores, FeatureScores.ScoreType.WeightedTotalScore.getTypeName());
            ErrorStat errorStat = stats.errorStatistics(featureScoresList, params);
            int count = AirusUtil.checkFdr(errorStat.getStatMetrics().getFdr());
            logger.info("pi0:" + errorStat.getPi0Est().getPi0());
            if (count > 0) {
                logger.info("本轮尝试有效果:检测结果:" + count + "个");
            }
            if (count > maxCount) {
                maxCount = count;
                weightsMapList.add(ldaLearnData.getWeightsMap());
            }

            if (params.isDebug()) {
                break;
            }
        }

        return AirusUtil.averagedWeights(weightsMapList);
    }


    private ErrorStat finalErrorTable(ErrorStat errorStat, Params params) {

        StatMetrics statMetrics = errorStat.getStatMetrics();
        Double[] cutOffs = errorStat.getCutoff();
        Double[] cutOffsSort = cutOffs.clone();
        Arrays.sort(cutOffsSort);
        double minCutOff = cutOffsSort[0];
        double maxCutOff = cutOffsSort[cutOffs.length - 1];
        double margin = (maxCutOff - minCutOff) * 0.05;
        Double[] sampledCutoffs = MathUtil.linspace(minCutOff - margin, maxCutOff - margin, params.getNumCutOffs());
        Integer[] ix = MathUtil.findNearestMatches(cutOffs, sampledCutoffs, params.getUseSortOrders());

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

    private ErrorStat summaryErrorTable(ErrorStat errorStat, Params params) {
        StatMetrics statMetrics = errorStat.getStatMetrics();
        Double[] qvalues = params.getQvalues();
        Integer[] ix = MathUtil.findNearestMatches(errorStat.getQvalue(), qvalues, params.getUseSortOrders());

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
