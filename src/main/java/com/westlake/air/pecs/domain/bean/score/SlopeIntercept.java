package com.westlake.air.pecs.domain.bean.score;

import lombok.Data;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-08 00:06
 */
@Data
public class SlopeIntercept {

    float slope;

    float intercept;

    public SlopeIntercept(){}

    public static SlopeIntercept create(){
        SlopeIntercept si = new SlopeIntercept();
        si.setIntercept(0);
        si.setSlope(1);
        return si;
    }

    @Override
    public String toString() {
        return "slope:" + slope + ";intercept:" + intercept;
    }
}
