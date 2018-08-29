package com.westlake.air.pecs.domain.bean.score;

import lombok.Data;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-08 00:06
 */
@Data
public class SlopeIntercept {

    float slope = 1f;

    float intercept = 0;

    @Override
    public String toString() {
        return "slope:" + slope + ";intercept:" + intercept;
    }
}
