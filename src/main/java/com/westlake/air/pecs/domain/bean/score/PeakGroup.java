package com.westlake.air.pecs.domain.bean.score;

import lombok.Data;

import java.util.List;

@Data
public class PeakGroup {

    //算法得出的最高峰的Rt(不一定在谱图里面存在这个值)
    double rt;

    //group中每一个碎片的强度
    List<Double> intensities;

    //最终的强度总和
    double intensitySum;

    //单个离子在bestLeftRt和bestRightRt中间最大峰的强度
    List<Double> peakApexInts;

    //算法选定的峰形范围左侧最合适的RT
    double bestLeftRt;

    //算法选定的峰形范围右侧最合适的RT
    double bestRightRt;

    //所有离子在所有RT上的Intensity总和
    double totalXic;

    //在算法选定的峰形范围内的Rt和Intensity对
    List<Double> hullRt;

    List<List<Double>> hullInts;
}
