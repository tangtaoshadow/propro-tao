package com.westlake.air.pecs.rtnormalizer.domain;

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

    List<Float> hullRt;
    List<Float> hullInt;
}
