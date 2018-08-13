package com.westlake.air.pecs.domain.bean.score;

import lombok.Data;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-07-30 21-46
 */
@Data
public class ExperimentFeature {

    float rt;

    float intensity;

    float peakApexInt;

    float bestLeft;

    float bestRight;

    float totalXic;

    List<Float> hullRt;
    List<Float> hullInt;
}
