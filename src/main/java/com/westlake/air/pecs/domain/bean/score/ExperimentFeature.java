package com.westlake.air.pecs.domain.bean.score;

import lombok.Data;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-07-30 21-46
 */
@Data
public class ExperimentFeature {

    //算法得出的最高峰的Rt(不一定在谱图里面存在这个值)
    double rt;

    //单个离子的强度总和
    double intensity;

    //求最终的强度总和
    double intensitySum;

    //单个离子在bestLeftRt和bestRightRt中间最大峰的强度
    double peakApexInt;

    //算法选定的峰形范围左侧最合适的RT
    double bestLeftRt;

    //算法选定的峰形范围右侧最合适的RT
    double bestRightRt;

    //所有离子在所有RT上的Intensity总和
    double totalXic;

    //在算法选定的峰形范围内的Rt和Intensity对
    List<Double> hullRt;
    List<Double> hullInt;
}
