package com.westlake.air.pecs.domain.bean.score;

import lombok.Data;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-22 22:40
 */
@Data
public class IntensityRtLeftRtRightPairs {

    Double[] rtLeftArray;

    Double[] rtRightArray;

    Double[] intensityArray;

    public IntensityRtLeftRtRightPairs(){}

    public IntensityRtLeftRtRightPairs(Double[] intensityArray, Double[] rtLeftArray, Double[] rtRightArray){
        this.rtLeftArray = rtLeftArray;
        this.rtRightArray = rtRightArray;
        this.intensityArray = intensityArray;
    }
}
