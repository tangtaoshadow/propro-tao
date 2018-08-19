package com.westlake.air.pecs.domain.bean.score;

import lombok.Data;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-19 19:13
 */
@Data
public class EmgModelParams {
    float boundingBoxMin;

    float boundingBoxMax;

    float mean = 1.0f;

    float variance = 1.0f;

    double height;

    double width;

    double symmetry;

    double retention;

    float toleranceStdevBox = 3.0f;

    float interpolationStep = 0.2f;
}
