package com.westlake.air.pecs.domain.bean.analyse;

import lombok.Data;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-29 19:35
 */
@Data
public class RtIntensityPairsDouble {
    Float[] rtArray;

    Double[] intensityArray;

    public RtIntensityPairsDouble(){}

    public RtIntensityPairsDouble(Float[] rtArray, Double[] intensityArray){
        this.rtArray = rtArray;
        this.intensityArray = intensityArray;
    }

    public RtIntensityPairsDouble(RtIntensityPairs rtIntensityPairs){
        this.rtArray = rtIntensityPairs.getRtArray().clone();
        Double[] intensity = new Double[rtIntensityPairs.getIntensityArray().length];
        for(int i=0; i<intensity.length; i++){
            intensity[i] = (double) rtIntensityPairs.getIntensityArray()[i];
        }
        this.intensityArray = intensity;
    }
}
