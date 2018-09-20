package com.westlake.air.pecs.algorithm;

import com.alibaba.fastjson.JSON;
import com.westlake.air.pecs.domain.bean.airus.*;
import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import com.westlake.air.pecs.domain.db.ScoresDO;
import com.westlake.air.pecs.service.ScoresService;
import com.westlake.air.pecs.utils.AirusUtils;
import com.westlake.air.pecs.utils.ArrayUtils;
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
    Stats stats;
    @Autowired
    ScoresService scoresService;

    public FinalResult doAirus(String overviewId) {
        logger.info("开始获取打分数据");
        List<ScoresDO> scores = scoresService.getAllByOverviewId(overviewId);
        logger.info("开始转换打分数据格式");
        ScoreData scoreData = trans(scores);
        logger.info("开始训练学习数据权重");
        Double[] weights = learn(scoreData.getScoreData(), scoreData.getGroupNumId(), scoreData.getIsDecoy());
        logger.info("开始计算打分");
        Double[] dscore = calculateDscore(weights, scoreData);

        Double[] topTargetDscores = AirusUtils.getTopTargetPeaks(dscore, scoreData.getIsDecoy(), AirusUtils.findTopIndex(dscore, scoreData.getGroupNumId()));
        Double[] topDecoyDscores = AirusUtils.getTopDecoyPeaks(dscore, scoreData.getIsDecoy(), AirusUtils.findTopIndex(dscore, scoreData.getGroupNumId()));
        String[] scoreColumns = FeatureScores.getScoresColumns();

        HashMap<String, Double> classifierTable = new HashMap<String, Double>();
        for (int i = 0; i < weights.length; i++) {
            classifierTable.put(scoreColumns[i], weights[i]);
        }
        FinalResult finalResult = new FinalResult();
        finalResult.setClassifierTable(classifierTable);
        ErrorStat errorStat = stats.errorStatistics(topTargetDscores, topDecoyDscores);
        finalResult.setFinalErrorTable(finalErrorTable(errorStat));
        finalResult.setSummaryErrorTable(summaryErrorTable(errorStat));
        finalResult.setAllInfo(errorStat);
        return finalResult;
    }

    public ScoreData trans(List<ScoresDO> scores) {
        ScoreData scoreData = new ScoreData();
        if (scores == null || scores.size() == 0) {
            return null;
        }

        logger.info("开始构造打分数据");
        List<Boolean> isDecoyList = new ArrayList<>();
        List<String> peptideRefList = new ArrayList<>();
        List<HashMap<String, Double>> scoreList = new ArrayList<>();
        for (ScoresDO score : scores) {
            for (FeatureScores fs : score.getFeatureScoresList()) {
                isDecoyList.add(score.getIsDecoy());
                peptideRefList.add(score.getPeptideRef());
                HashMap<String, Double> scoreMap = fs.buildScoreMap();
                scoreList.add(scoreMap);
                for (String key : scoreMap.keySet()) {
                    if (scoreMap.get(key).equals(Double.NaN)) {
                        logger.info("包含空打分肽段名:" + JSON.toJSONString(score));
                        logger.info("包含空打分全部数据:" + JSON.toJSONString(score));
                    }
                }
            }
        }

        Boolean[] isDecoyArray = new Boolean[isDecoyList.size()];
        isDecoyList.toArray(isDecoyArray);

        String[] groupIds = new String[peptideRefList.size()];
        Integer[] groupNumIds = AirusUtils.getGroupNumId(peptideRefList.toArray(groupIds));
        Double[][] scoresArray = new Double[peptideRefList.size()][SCORES_COUNT];
        for (int i = 0; i < scoreList.size(); i++) {
            Double[] jSeries = new Double[SCORES_COUNT];
            scoreList.get(i).values().toArray(jSeries);
            scoresArray[i] = jSeries;
        }
        logger.info("打分数据构造完毕,开始学习");
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
    public Double[] learn(Double[][] scores, Integer[] groupNumId, Boolean[] isDecoy) {
        Params params = new Params();

        int neval = params.getSsNumIter();
        Double[][] ws = new Double[neval][scores[0].length];
        for (int i = 0; i < neval; i++) {
            LDALearn ldaLearn = semiSupervised.learnRandomized(scores, groupNumId, isDecoy);
            ws[i] = ldaLearn.getParams();
        }
        logger.info("学习完毕");

        return semiSupervised.averagedLearner(ws);
    }

    private ErrorStat finalErrorTable(ErrorStat errorStat) {

        Params params = new Params();
        StatMetrics statMetrics = errorStat.getStatMetrics();
        Double[] cutOffs = errorStat.getCutoff();
        Double[] cutOffsSort = cutOffs.clone();
        Arrays.sort(cutOffsSort);
        double minCutOff = cutOffsSort[0];
        double maxCutOff = cutOffsSort[cutOffs.length - 1];
        double margin = (maxCutOff - minCutOff) * 0.05;
        Double[] sampledCutoffs = ArrayUtils.linspace(minCutOff - margin, maxCutOff - margin, params.getNumCutOffs());
        Integer[] ix = ArrayUtils.findNearestMatches(cutOffs, sampledCutoffs, params.getUseSortOrders());

        ErrorStat sampleErrorStat = new ErrorStat();
        sampleErrorStat.setCutoff(sampledCutoffs);
        sampleErrorStat.setQvalue(ArrayUtils.extractRow(errorStat.getQvalue(), ix));
        sampleErrorStat.setPvalue(ArrayUtils.extractRow(errorStat.getPvalue(), ix));

        StatMetrics sampleStatMatric = new StatMetrics();
        sampleStatMatric.setSvalue(ArrayUtils.extractRow(statMetrics.getSvalue(), ix));
        sampleStatMatric.setFn(ArrayUtils.extractRow(statMetrics.getFn(), ix));
        sampleStatMatric.setFnr(ArrayUtils.extractRow(statMetrics.getFnr(), ix));
        sampleStatMatric.setFdr(ArrayUtils.extractRow(statMetrics.getFdr(), ix));
        sampleStatMatric.setFp(ArrayUtils.extractRow(statMetrics.getFp(), ix));
        sampleStatMatric.setFpr(ArrayUtils.extractRow(statMetrics.getFpr(), ix));
        sampleStatMatric.setTn(ArrayUtils.extractRow(statMetrics.getTn(), ix));
        sampleStatMatric.setTp(ArrayUtils.extractRow(statMetrics.getTp(), ix));

        sampleErrorStat.setStatMetrics(sampleStatMatric);

        return sampleErrorStat;
    }

    private ErrorStat summaryErrorTable(ErrorStat errorStat) {
        Params params = new Params();
        StatMetrics statMetrics = errorStat.getStatMetrics();
        Double[] qvalues = params.getQvalues();
        Integer[] ix = ArrayUtils.findNearestMatches(errorStat.getQvalue(), qvalues, params.getUseSortOrders());

        ErrorStat subErrorStat = new ErrorStat();
        subErrorStat.setCutoff(ArrayUtils.extractRow(errorStat.getCutoff(), ix));
        subErrorStat.setQvalue(qvalues);
        subErrorStat.setPvalue(ArrayUtils.extractRow(errorStat.getPvalue(), ix));

        StatMetrics subStatMatric = new StatMetrics();
        subStatMatric.setSvalue(ArrayUtils.extractRow(statMetrics.getSvalue(), ix));
        subStatMatric.setFn(ArrayUtils.extractRow(statMetrics.getFn(), ix));
        subStatMatric.setFnr(ArrayUtils.extractRow(statMetrics.getFnr(), ix));
        subStatMatric.setFdr(ArrayUtils.extractRow(statMetrics.getFdr(), ix));
        subStatMatric.setFp(ArrayUtils.extractRow(statMetrics.getFp(), ix));
        subStatMatric.setFpr(ArrayUtils.extractRow(statMetrics.getFpr(), ix));
        subStatMatric.setTn(ArrayUtils.extractRow(statMetrics.getTn(), ix));
        subStatMatric.setTp(ArrayUtils.extractRow(statMetrics.getTp(), ix));

        subErrorStat.setStatMetrics(subStatMatric);

        return subErrorStat;
    }

    /**
     * Dscore: Normalize clfScores with clfScores' TopDecoyPeaks.
     */
    private Double[] calculateDscore(Double[] weights, ScoreData scoreData) {
        Double[][] scores = AirusUtils.getFeatureMatrix(scoreData.getScoreData(), true);
        Double[] classifierScore = ArrayUtils.dot(scores, weights);
        Double[] classifierTopDecoyPeaks = AirusUtils.getTopDecoyPeaks(classifierScore, scoreData.getIsDecoy(), AirusUtils.findTopIndex(classifierScore, AirusUtils.getGroupNumId(scoreData.getGroupId())));
        return ArrayUtils.normalize(classifierScore, classifierTopDecoyPeaks);
    }

    public FinalResult buildResult(ScoreData scoreData) {
        Params params = new Params();
        if (params.isTest()) {
            scoreData = AirusUtils.fakeSortTgId(scoreData);
        }
//        String[] uniqueGroupId = ArrayUtils.extractRow(scoreData.getGroupId(), AirusUtils.sortedUniqueIndex(scoreData.getGroupNumId()));
        Double[] weights = learn(scoreData.getScoreData(), scoreData.getGroupNumId(), scoreData.getIsDecoy());
        Double[] dscore = calculateDscore(weights, scoreData);
        Double[] topTargetDscores = AirusUtils.getTopTargetPeaks(dscore, scoreData.getIsDecoy(), AirusUtils.findTopIndex(dscore, scoreData.getGroupNumId()));
        Double[] topDecoyDscores = AirusUtils.getTopDecoyPeaks(dscore, scoreData.getIsDecoy(), AirusUtils.findTopIndex(dscore, scoreData.getGroupNumId()));
        String[] scoreColumns = scoreData.getScoreColumns();

        HashMap<String, Double> classifierTable = new HashMap<String, Double>();
        for (int i = 0; i < weights.length; i++) {
            classifierTable.put(scoreColumns[i], weights[i]);
        }
        //Double[] score = LDALearner.score(scoreData.getScoreData(),weights,true).getFeedBack();
        FinalResult finalResult = new FinalResult();
        finalResult.setClassifierTable(classifierTable);
        ErrorStat errorStat = stats.errorStatistics(topTargetDscores, topDecoyDscores);

        finalResult.setFinalErrorTable(finalErrorTable(errorStat));
        finalResult.setSummaryErrorTable(summaryErrorTable(errorStat));
        return finalResult;
    }

}
