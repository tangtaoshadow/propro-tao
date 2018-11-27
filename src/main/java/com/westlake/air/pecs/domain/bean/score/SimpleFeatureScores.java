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

    double rt;

    HashMap<String, Double> scoresMap;

    Double mainScore;

    public SimpleFeatureScores(String peptideRef, Boolean isDecoy){
        this.peptideRef = peptideRef;
        this.isDecoy = isDecoy;
    }
}
