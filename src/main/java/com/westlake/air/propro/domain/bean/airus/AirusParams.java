package com.westlake.air.propro.domain.bean.airus;

import com.westlake.air.propro.constants.Classifier;
import com.westlake.air.propro.constants.ScoreType;
import lombok.Data;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-19 09:05
 */

@Data
public class AirusParams {

//    Learner.classifier classifier = Learner.classifier.XgbLearner;
    Classifier classifier = Classifier.lda;

    double trainTestRatio = 1;

    int xevalNumIter = 30;

    double ssInitialFdr = 0.15;

    double ssIterationFdr = 0.05; //0.1 3310; 0.08 3300; 0.05 3276

    double xgbInitialFdr = 0.01;

    double xgbIterationFdr = 0.008;

    //训练数据集的次数
    int trainTimes = 1;

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

    //用于训练的打分快照
    List<String> scoreTypes;

    //首批训练时默认作为主分数的分数类型
    String mainScore = ScoreType.MainScore.getTypeName();
}
