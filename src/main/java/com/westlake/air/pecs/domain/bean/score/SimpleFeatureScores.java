package com.westlake.air.pecs.domain.bean.score;

import lombok.Data;

import java.util.HashMap;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-05 22:42
 */
@Data
public class SimpleFeatureScores {

    String peptideRef;

    Boolean isDecoy;

    //本峰值对应的最佳RT时间
    Double rt;

    Double intensitySum;

    //本峰对应的打分列表
    HashMap<String, Double> scoresMap;

    //本峰对应的最终综合打分
    Double mainScore;

    Double pValue;

    Double qValue;

    Double fdr;

    public SimpleFeatureScores(){}

    public SimpleFeatureScores(String peptideRef, Boolean isDecoy){
        this.peptideRef = peptideRef;
        this.isDecoy = isDecoy;
    }
}
