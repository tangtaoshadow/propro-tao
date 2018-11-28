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

    Double rt;

    HashMap<String, Double> scoresMap;

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
