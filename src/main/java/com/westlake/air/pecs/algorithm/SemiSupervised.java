package com.westlake.air.pecs.algorithm;

import com.westlake.air.pecs.domain.bean.airus.LDALearn;
import com.westlake.air.pecs.domain.bean.airus.Params;
import com.westlake.air.pecs.domain.bean.airus.TrainAndTest;
import com.westlake.air.pecs.domain.bean.airus.TrainPeaks;
import com.westlake.air.pecs.utils.AirusUtils;
import com.westlake.air.pecs.utils.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    private TrainPeaks selectTrainPeaks(Double[][] trainData, Double[] scores, Boolean[] trainIsDecoy, Boolean[] isTopPeak, double cutOffFdr) {

        Double[][] topTargetPeaks = AirusUtils.getTopTargetPeaks(trainData, trainIsDecoy, isTopPeak);
        Double[] topTargetScores = AirusUtils.getTopTargetPeaks(scores, trainIsDecoy, isTopPeak);
        Double[][] topDecoyPeaks = AirusUtils.getTopDecoyPeaks(trainData, trainIsDecoy, isTopPeak);
        Double[] topDecoyScores = AirusUtils.getTopDecoyPeaks(scores, trainIsDecoy, isTopPeak);

        // find cutoff fdr from scores and only use best target peaks:
        Double cutoff = stats.findCutoff(topTargetScores, topDecoyScores, cutOffFdr);
        Double[][] bestTargetPeaks = AirusUtils.peaksFilter(topTargetPeaks, topTargetScores, cutoff);

        TrainPeaks trainPeaks = new TrainPeaks();
        trainPeaks.setBestTargetPeaks(bestTargetPeaks);
        trainPeaks.setTopDecoyPeaks(topDecoyPeaks);
        return trainPeaks;
    }

//    private void iterSemiSupervisedLearning(Double[][] trainData, Double[] classifierScore, Boolean[] trainIsDecoy, Boolean[] isTopPeak) {
//        Params params = new Params();
//        TrainPeaks trainPeaks = selectTrainPeaks(trainData, classifierScore, trainIsDecoy, isTopPeak, params.getSsIterationFdr());
//
//        Double[] w = ldaLearner.learn(trainPeaks.getTopDecoyPeaks(), trainPeaks.getBestTargetPeaks(), true);
//        Double[] clfScores = ldaLearner.score(trainData, w, true).getModel();
//    }

    /**
     * set w as average
     * @param params w[]: the result of nevals
     */
    public Double[] averagedLearner(Double[][] params){
        return ldaLearner.averagedWeight(params);
    }

    private Double[] score(Double[][] data, Double[] params){
        return ldaLearner.score(data,params,true);
    }

    public LDALearn learnRandomized(Double[][] scores, Integer[] groupNumId, Boolean[] isDecoy){
        LDALearn ldaLearn = new LDALearn();
        Params params = new Params();
        try {
            //Get part of scores as train input.
            TrainAndTest trainAndTest = AirusUtils.splitForXval(scores, groupNumId, isDecoy, params.getXevalFraction(), params.isTest());
            Double[][] trainScores = trainAndTest.getTrainData();
            Integer[] trainId = trainAndTest.getTrainId();
            Boolean[] trainIsDecoy = trainAndTest.getTrainIsDecoy();
//            Double[][] testScores = trainAndTest.getTestData();
//            Integer[] testId = trainAndTest.getTestId();
//            Boolean[] testIsDecoy = trainAndTest.getTestIsDecoy();
            Double[] mainScore = ArrayUtils.extractColumn(trainScores, 0);
            Boolean[] isTopPeak = AirusUtils.findTopIndex(mainScore, trainId);


            //Start the first time of training with mainScore as main score.
            TrainPeaks trainPeaks = selectTrainPeaks(trainScores, mainScore, trainIsDecoy, isTopPeak, params.getSsInitialFdr());
            Double[] w = ldaLearner.learn(trainPeaks.getTopDecoyPeaks(), trainPeaks.getBestTargetPeaks(), false);
            Double[] clfScores = ldaLearner.score(trainScores, w, false);
            clfScores = ArrayUtils.normalize(clfScores);
            isTopPeak = AirusUtils.findTopIndex(clfScores, trainId);

            //Begin "semi supervised learning" iteration.
            for (int times = 0; times < params.getXevalNumIter(); times++) {
                logger.info("开始第"+times+"轮机器学习");
//                iterSemiSupervisedLearning(trainScores, clfScores, trainIsDecoy, isTopPeak);
                TrainPeaks trainPeaksTemp = selectTrainPeaks(trainScores, clfScores, trainIsDecoy, isTopPeak, params.getSsIterationFdr());
                w = ldaLearner.learn(trainPeaksTemp.getTopDecoyPeaks(), trainPeaksTemp.getBestTargetPeaks(), true);
                clfScores = ldaLearner.score(trainScores, w, true);
                isTopPeak = AirusUtils.findTopIndex(clfScores, trainId);
            }
            //After semi supervised iteration: calculate normalized clfScores of FULL data set.
//            clfScores = score(scores, w);
//            isTopPeak = AirusUtils.findTopIndex(clfScores, groupNumId);
//            Double[] topDecoyScores = AirusUtils.getTopDecoyPeaks(clfScores, isDecoy, isTopPeak);
//            ArrayUtils.normalize(clfScores, topDecoyScores);
//            clfScores = score(testScores, w);
//            Double[] topTestPeaks = AirusUtils.getDecoyPeaks(clfScores, AirusUtils.findTopIndex(clfScores, testId));
//            Double[] topTestTargetScores = AirusUtils.getTargetPeaks(topTestPeaks, testIsDecoy);
//            Double[] topTestDecoyScores = AirusUtils.getDecoyPeaks(topTestPeaks, testIsDecoy);
//            ldaLearn.setTopTestDecoyScores(topTestDecoyScores);
//            ldaLearn.setTopTestTargetScores(topTestTargetScores);
            ldaLearn.setParams(w);
            return ldaLearn;
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("learnRandomized Fail.\n");
            return null;
        }

    }
}
