package com.westlake.air.pecs.domain.bean.airus;

import com.westlake.air.pecs.constants.Classifier;
import com.westlake.air.pecs.constants.ScoreType;
import lombok.Data;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-19 09:05
 */

@Data
public class AirusParams {

//    Learner.classifier classifier = Learner.classifier.XgbLearner;
    Classifier classifier = Classifier.lda;

    double trainTestRatio = 0.5;

    int xevalNumIter = 10;

    double ssInitialFdr = 0.15;

    double ssIterationFdr = 0.05;

    double xgbInitialFdr = 0.01;

    double xgbIterationFdr = 0.008;

    //训练数据集的次数
    int trainTimes = 10;

    String ssMainScore = "var_xcorr_shape";

    boolean parametric = false;

    boolean pFdr = false;

    Double[] pi0Lambda = {0.0, 0.05, 0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45};
//    Double[] pi0Lambda = {0.01, 0.02, 0.03,0.04, 0.05, 0.06, 0.07, 0.08, 0.09, 0.1};

    String pi0Method = "bootstrap";

    boolean pi0SmoothLogPi0 = false;

    boolean computeLfdr = false;

    boolean lfdrTruncate = true;

    boolean lfdrMonotone = true;

    //not used
    String lfdrTransformation = "probit";

    double lfdrAdj = 1.5;

    double lfdrEps = Math.pow(10.0, -8);

    int numCutOffs = 51;

    int useSortOrders = 1;

    Double[] qvalues = {0.0, 0.01, 0.02, 0.05, 0.1, 0.2, 0.3, 0.4, 0.5};

    boolean isDebug = false;

    //首批训练时默认作为主分数的分数类型
    String mainScore = ScoreType.MainScore.getTypeName();
}
