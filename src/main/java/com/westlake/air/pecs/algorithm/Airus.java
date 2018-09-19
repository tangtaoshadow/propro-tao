package com.westlake.air.pecs.algorithm;

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
        logger.info("打分数据获取完毕");
        ScoreData scoreData = trans(scores);
        Double[] weights = learn(scoreData.getScoreData(), scoreData.getGroupNumId(), scoreData.getIsDecoy());
        Double[] dscore = calculateDscore(weights, scoreData);

        Double[] topTargetDscores = AirusUtils.getTopTargetPeaks(dscore, scoreData.getIsDecoy(), AirusUtils.findTopIndex(dscore, scoreData.getGroupNumId()).getModel()).getModel();
        Double[] topDecoyDscores = AirusUtils.getTopDecoyPeaks(dscore, scoreData.getIsDecoy(), AirusUtils.findTopIndex(dscore, scoreData.getGroupNumId()).getModel()).getModel();
        String[] scoreColumns = FeatureScores.getScoresColumns();

        HashMap<String, Double> classifierTable = new HashMap<String, Double>();
        for (int i = 0; i < weights.length; i++) {
            classifierTable.put(scoreColumns[i], weights[i]);
        }
        for(Double d : topTargetDscores){
            if(d == null || d.equals(Double.NaN)){
                logger.info("T出现啦!"+d);
            }
        }
        for(Double d : topDecoyDscores){
            if(d == null || d.equals(Double.NaN)){
                logger.info("D出现啦!"+d);
            }
        }
        FinalResult finalResult = new FinalResult();
        finalResult.setClassifierTable(classifierTable);
        finalResult.setFinalErrorTable(finalErrorTable(topTargetDscores, topDecoyDscores));
        finalResult.setSummaryErrorTable(summaryErrorTable(topTargetDscores, topDecoyDscores));
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
                scoreList.add(fs.buildScoreMap());
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

    private ErrorStat finalErrorTable(Double[] targetScores, Double[] decoyScores) {

        Params params = new Params();
        ErrorStat errorStat = stats.errorStatistics(targetScores, decoyScores);
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
        sampleErrorStat.setQvalue(ArrayUtils.extractRow(errorStat.getQvalue(), ix).getModel());
        sampleErrorStat.setPvalue(ArrayUtils.extractRow(errorStat.getPvalue(), ix).getModel());

        StatMetrics sampleStatMatric = new StatMetrics();
        sampleStatMatric.setSvalue(ArrayUtils.extractRow(statMetrics.getSvalue(), ix).getModel());
        sampleStatMatric.setFn(ArrayUtils.extractRow(statMetrics.getFn(), ix).getModel());
        sampleStatMatric.setFnr(ArrayUtils.extractRow(statMetrics.getFnr(), ix).getModel());
        sampleStatMatric.setFdr(ArrayUtils.extractRow(statMetrics.getFdr(), ix).getModel());
        sampleStatMatric.setFp(ArrayUtils.extractRow(statMetrics.getFp(), ix).getModel());
        sampleStatMatric.setFpr(ArrayUtils.extractRow(statMetrics.getFpr(), ix).getModel());
        sampleStatMatric.setTn(ArrayUtils.extractRow(statMetrics.getTn(), ix).getModel());
        sampleStatMatric.setTp(ArrayUtils.extractRow(statMetrics.getTp(), ix).getModel());

        sampleErrorStat.setStatMetrics(sampleStatMatric);

        return sampleErrorStat;
    }

    private ErrorStat summaryErrorTable(Double[] targetScores, Double[] decoyScores) {
        Params params = new Params();
        ErrorStat errorStat = stats.errorStatistics(targetScores, decoyScores);
        StatMetrics statMetrics = errorStat.getStatMetrics();
        Double[] qvalues = params.getQvalues();
        Integer[] ix = ArrayUtils.findNearestMatches(errorStat.getQvalue(), qvalues, params.getUseSortOrders());

        ErrorStat subErrorStat = new ErrorStat();
        subErrorStat.setCutoff(ArrayUtils.extractRow(errorStat.getCutoff(), ix).getModel());
        subErrorStat.setQvalue(qvalues);
        subErrorStat.setPvalue(ArrayUtils.extractRow(errorStat.getPvalue(), ix).getModel());

        StatMetrics subStatMatric = new StatMetrics();
        subStatMatric.setSvalue(ArrayUtils.extractRow(statMetrics.getSvalue(), ix).getModel());
        subStatMatric.setFn(ArrayUtils.extractRow(statMetrics.getFn(), ix).getModel());
        subStatMatric.setFnr(ArrayUtils.extractRow(statMetrics.getFnr(), ix).getModel());
        subStatMatric.setFdr(ArrayUtils.extractRow(statMetrics.getFdr(), ix).getModel());
        subStatMatric.setFp(ArrayUtils.extractRow(statMetrics.getFp(), ix).getModel());
        subStatMatric.setFpr(ArrayUtils.extractRow(statMetrics.getFpr(), ix).getModel());
        subStatMatric.setTn(ArrayUtils.extractRow(statMetrics.getTn(), ix).getModel());
        subStatMatric.setTp(ArrayUtils.extractRow(statMetrics.getTp(), ix).getModel());

        subErrorStat.setStatMetrics(subStatMatric);

        return subErrorStat;
    }

    /**
     * Dscore: Normalize clfScores with clfScores' TopDecoyPeaks.
     */
    private Double[] calculateDscore(Double[] weights, ScoreData scoreData) {
        Double[][] scores = AirusUtils.getFeatureMatrix(scoreData.getScoreData(), true).getModel();
        Double[] classifierScore = ArrayUtils.dot(scores, weights).getModel();
        Double[] classifierTopDecoyPeaks = AirusUtils.getTopDecoyPeaks(classifierScore, scoreData.getIsDecoy(), AirusUtils.findTopIndex(classifierScore, AirusUtils.getGroupNumId(scoreData.getGroupId())).getModel()).getModel();
        return ArrayUtils.normalize(classifierScore, classifierTopDecoyPeaks);
    }

    public FinalResult buildResult(ScoreData scoreData) {
        Params params = new Params();
        if (params.isTest()) {
            scoreData = AirusUtils.fakeSortTgId(scoreData);
        }
//        String[] uniqueGroupId = ArrayUtils.extractRow(scoreData.getGroupId(), AirusUtils.sortedUniqueIndex(scoreData.getGroupNumId())).getModel();
        Double[] weights = learn(scoreData.getScoreData(), scoreData.getGroupNumId(), scoreData.getIsDecoy());
        Double[] dscore = calculateDscore(weights, scoreData);
        Double[] topTargetDscores = AirusUtils.getTopTargetPeaks(dscore, scoreData.getIsDecoy(), AirusUtils.findTopIndex(dscore, scoreData.getGroupNumId()).getModel()).getModel();
        Double[] topDecoyDscores = AirusUtils.getTopDecoyPeaks(dscore, scoreData.getIsDecoy(), AirusUtils.findTopIndex(dscore, scoreData.getGroupNumId()).getModel()).getModel();
        String[] scoreColumns = scoreData.getScoreColumns();

        HashMap<String, Double> classifierTable = new HashMap<String, Double>();
        for (int i = 0; i < weights.length; i++) {
            classifierTable.put(scoreColumns[i], weights[i]);
        }
        //Double[] score = LDALearner.score(scoreData.getScoreData(),weights,true).getFeedBack();
        FinalResult finalResult = new FinalResult();
        finalResult.setClassifierTable(classifierTable);
        finalResult.setFinalErrorTable(finalErrorTable(topTargetDscores, topDecoyDscores));
        finalResult.setSummaryErrorTable(summaryErrorTable(topTargetDscores, topDecoyDscores));
        return finalResult;
    }

}
