package com.westlake.air.pecs.domain.bean.score;

import lombok.Data;
import scala.reflect.internal.tpe.FindMembers;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-07-30 21-46
 */
@Data
public class ExperimentFeature {

    //算法得出的最高峰的Rt(不一定在谱图里面存在这个值)
    List<Double> apexRtList;

    //单个离子的强度总和
    List<HashMap<String, Double>> ionIntensityList;

    //求最终的强度总和
    List<Double> peakGroupIntList;

    //单个离子在bestLeftRt和bestRightRt中间最大峰的强度
    List<HashMap<String,Double>> ionApexIntList;

    //算法选定的峰形范围左侧最合适的RT
    List<Double> bestLeftRtList;

    //算法选定的峰形范围右侧最合适的RT
    List<Double> bestRightRtList;

    //所有离子在所有RT上的Intensity总和
    double totalXic;

    //在算法选定的峰形范围内的Rt和Intensity对
    List<Double[]> ionHullRtList;
    List<HashMap<String,Double[]>> ionHullIntList;
}
