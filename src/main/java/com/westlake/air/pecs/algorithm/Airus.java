package com.westlake.air.pecs.algorithm;

import com.westlake.air.pecs.domain.bean.airus.*;
import com.westlake.air.pecs.utils.AirusUtils;
import com.westlake.air.pecs.utils.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-19 09:25
 */
@Component
public class Airus {

    @Autowired
    SemiSupervised semiSupervised;
    @Autowired
    Stats stats;

    public Double[] learn(Double[][] scores, Integer[] groupNumId, Boolean[] isDecoy){
        Params params = new Params();

        int neval = params.getSsNumIter();
        Double[][] ws = new Double[neval][scores[0].length];
        for(int i=0;i<neval;i++){
            LDALearn ldaLearn = semiSupervised.learnRandomized(scores, groupNumId, isDecoy);
            ws[i] = ldaLearn.getParams();
        }

        return semiSupervised.averagedLearner(ws);
    }

    private ErrorStat finalErrorTable(Double[] targetScores, Double[] decoyScores){

        Params params = new Params();
        ErrorStat errorStat = stats.errorStatistics(targetScores, decoyScores);
        StatMetrics statMetrics = errorStat.getStatMetrics();
        Double[] cutOffs = errorStat.getCutoff();
        Double[] cutOffsSort = cutOffs.clone();
        AirusUtils.sort(cutOffsSort);
        double minCutOff = cutOffsSort[0];
        double maxCutOff = cutOffsSort[cutOffs.length-1];
        double margin = (maxCutOff - minCutOff) * 0.05;
        Double[] sampledCutoffs = AirusUtils.linspace(minCutOff-margin,maxCutOff-margin, params.getNumCutOffs());
        Integer[] ix = AirusUtils.findNearestMatches(cutOffs, sampledCutoffs, params.getUseSortOrders());

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

    private ErrorStat summaryErrorTable(Double[] targetScores, Double[] decoyScores){
        Params params = new Params();
        ErrorStat errorStat = stats.errorStatistics(targetScores, decoyScores);
        StatMetrics statMetrics = errorStat.getStatMetrics();
        Double[] qvalues = params.getQvalues();
        Integer[] ix = AirusUtils.findNearestMatches(errorStat.getQvalue(), qvalues, params.getUseSortOrders());

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
    private Double[] calculateDscore(Double[] weights, ScoreData scoreData){
        Double[][] scores = ArrayUtils.getFeatureMatrix(scoreData.getScoreData(), true).getModel();
        Double[] classifierScore = ArrayUtils.dot(scores, weights).getModel();
        Double[] classifierTopDecoyPeaks = ArrayUtils.getTopDecoyPeaks(classifierScore, scoreData.getIsDecoy(),ArrayUtils.findTopIndex(classifierScore,ArrayUtils.getGroupNumId(scoreData.getGroupId()).getModel()).getModel()).getModel();
        return AirusUtils.normalize(classifierScore, classifierTopDecoyPeaks);
    }

    public FinalResult buildResult(ScoreData scoreData){
        Params params = new Params();
        if(params.isTest()) {
            scoreData = ArrayUtils.fakeSortTgId(scoreData);
        }
        String[] uniqueGroupId = ArrayUtils.extractRow(scoreData.getGroupId(),AirusUtils.sortedUniqueIndex(scoreData.getGroupNumId())).getModel();
        Double[] weights = learn(scoreData.getScoreData(),scoreData.getGroupNumId(),scoreData.getIsDecoy());
        Double[] dscore = calculateDscore(weights, scoreData);
        Double[] topTargetDscores = ArrayUtils.getTopTargetPeaks(dscore, scoreData.getIsDecoy(), ArrayUtils.findTopIndex(dscore,scoreData.getGroupNumId()).getModel()).getModel();
        Double[] topDecoyDscores = ArrayUtils.getTopDecoyPeaks(dscore, scoreData.getIsDecoy(), ArrayUtils.findTopIndex(dscore, scoreData.getGroupNumId()).getModel()).getModel();
        String[] scoreColumns = scoreData.getScoreColumns();

        HashMap<String, Double> classifierTable = new HashMap<String, Double>();
        for(int i=0;i<weights.length;i++){
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
