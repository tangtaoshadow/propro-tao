package com.westlake.air.pecs.domain.bean.score;

import lombok.Data;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-07-30 21-46
 */
@Data
public class ExperimentFeature {

    double rt;

    double intensity;

    double intensitySum;

    double peakApexInt;

    double bestLeft;

    double bestRight;

    double totalXic;

    List<Double> hullRt;
    List<Double> hullInt;
}
