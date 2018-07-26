package com.westlake.air.pecs.domain.bean;

import lombok.Data;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-22 22:40
 */
@Data
public class MzIntensityPairs{

    Float[] mzArray;

    Float[] intensityArray;

    public MzIntensityPairs(){}

    public MzIntensityPairs(Float[] mzArray, Float[] intensityArray){
        this.mzArray = mzArray;
        this.intensityArray = intensityArray;
    }
}
