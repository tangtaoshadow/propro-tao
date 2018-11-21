package com.westlake.air.pecs.algorithm;

import com.westlake.air.pecs.domain.bean.airus.LDALearn;
import com.westlake.air.pecs.domain.bean.airus.Params;
import com.westlake.air.pecs.domain.bean.airus.TrainAndTest;
import com.westlake.air.pecs.domain.bean.airus.TrainPeaks;
import com.westlake.air.pecs.domain.db.ScoresDO;
import com.westlake.air.pecs.utils.AirusUtil;
import com.westlake.air.pecs.utils.ArrayUtil;
import com.westlake.air.pecs.utils.MathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    /**
     * set w as average
     * @param weights w[]: the result of nevals
     */
    public Double[] averagedLearner(Double[][] weights){
        return ldaLearner.averagedWeight(weights);
    }

    private Double[] score(Double[][] data, Double[] params){
        return ldaLearner.score(data,params,true);
    }

    public LDALearn learnRandomized(Double[][] scores, Integer[] groupNumId, Boolean[] isDecoy){
        LDALearn ldaLearn = new LDALearn();
        Params params = new Params();
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
                logger.info("开始第"+times+"轮机器学习");
                TrainPeaks trainPeaksTemp = selectTrainPeaks(trainScores, clfScores, trainIsDecoy, isTopPeak, params.getSsIterationFdr());
                w = ldaLearner.learn(trainPeaksTemp.getTopDecoyPeaks(), trainPeaksTemp.getBestTargetPeaks(), true);
                clfScores = ldaLearner.score(trainScores, w, true);
                isTopPeak = AirusUtil.findTopIndex(clfScores, trainId);
            }

            ldaLearn.setWeights(w);
            return ldaLearn;
        }catch (Exception e){
            logger.error("learnRandomized Fail.\n");
            e.printStackTrace();
            return null;
        }
    }

    public LDALearn learnRandomized(List<ScoresDO> scores){
        LDALearn ldaLearn = new LDALearn();
        Params params = new Params();
        try {
            //Get part of scores as train input.
            TrainAndTest trainAndTest = AirusUtil.split(scores, params.getTrainTestRatio(), params.isDebug());
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
                logger.info("开始第"+times+"轮机器学习");
                TrainPeaks trainPeaksTemp = selectTrainPeaks(trainScores, clfScores, trainIsDecoy, isTopPeak, params.getSsIterationFdr());
                w = ldaLearner.learn(trainPeaksTemp.getTopDecoyPeaks(), trainPeaksTemp.getBestTargetPeaks(), true);
                clfScores = ldaLearner.score(trainScores, w, true);
                isTopPeak = AirusUtil.findTopIndex(clfScores, trainId);
            }

            ldaLearn.setWeights(w);
            return ldaLearn;
        }catch (Exception e){
            logger.error("learnRandomized Fail.\n");
            e.printStackTrace();
            return null;
        }

    }
}
