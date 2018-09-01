package com.westlake.air.pecs.domain.bean.analyse;

import lombok.Data;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-29 19:35
 */
@Data
public class RtIntensityPairsDouble {
    Double[] rtArray;

    Double[] intensityArray;

    public RtIntensityPairsDouble(){}

    public RtIntensityPairsDouble(Float[] rtArray, Float[] intensityArray){
        Double[] rt = new Double[rtArray.length];
        Double[] intens = new Double[intensityArray.length];
        for(int i=0; i<rt.length; i++){
            rt[i] = Double.parseDouble(rtArray[i].toString());
            intens[i] = Double.parseDouble(intensityArray[i].toString());
        }
        this.rtArray = rt;
        this.intensityArray = intens;
    }

    public RtIntensityPairsDouble(Double[] rtArray, Double[] intensityArray){

        this.rtArray = rtArray;
        this.intensityArray = intensityArray;
    }

    public RtIntensityPairsDouble(RtIntensityPairs rtIntensityPairs){
        Double[] rt = new Double[rtIntensityPairs.getRtArray().length];
        for(int i=0; i<rt.length; i++){
            rt[i] = (double) rtIntensityPairs.getRtArray()[i];
        }
        this.rtArray = rt;
        Double[] intensity = new Double[rtIntensityPairs.getIntensityArray().length];
        for(int i=0; i<intensity.length; i++){
            intensity[i] = (double) rtIntensityPairs.getIntensityArray()[i];
        }
        this.intensityArray = intensity;
    }

    public RtIntensityPairsDouble(RtIntensityPairsDouble rtIntensityPairsDouble){
        this.rtArray = rtIntensityPairsDouble.getRtArray().clone();
        this.intensityArray = rtIntensityPairsDouble.getIntensityArray().clone();
    }
}
