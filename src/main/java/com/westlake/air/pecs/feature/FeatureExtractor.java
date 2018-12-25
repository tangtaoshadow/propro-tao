package com.westlake.air.pecs.feature;

import com.google.common.collect.Lists;
import com.westlake.air.pecs.domain.bean.analyse.*;
import com.westlake.air.pecs.domain.bean.score.ExperimentFeature;
import com.westlake.air.pecs.domain.bean.score.FeatureByPep;
import com.westlake.air.pecs.domain.bean.score.IntensityRtLeftRtRightPairs;
import com.westlake.air.pecs.domain.bean.score.PeakGroup;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.simple.IntensityGroup;
import com.westlake.air.pecs.rtnormalizer.ChromatogramFilter;
import com.westlake.air.pecs.rtnormalizer.RtNormalizerScorer;
import com.westlake.air.pecs.service.AnalyseDataService;
import com.westlake.air.pecs.service.AnalyseOverviewService;
import com.westlake.air.pecs.service.TaskService;
import com.westlake.air.pecs.service.PeptideService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-20 15:47
 */
@Component("featureExtractor")
public class FeatureExtractor {

    public final Logger logger = LoggerFactory.getLogger(FeatureExtractor.class);

    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    PeptideService peptideService;
    @Autowired
    GaussFilter gaussFilter;
    @Autowired
    PeakPicker peakPicker;
    @Autowired
    SignalToNoiseEstimator signalToNoiseEstimator;
    @Autowired
    ChromatogramPicker chromatogramPicker;
    @Autowired
    FeatureFinder featureFinder;
    @Autowired
    RtNormalizerScorer RTNormalizerScorer;
    @Autowired
    TaskService taskService;
    @Autowired
    ChromatogramFilter chromatogramFilter;

    /**
     *
     * @param dataDO 卷积后的数据对象
     * @param intensityMap 得到标准库中peptideRef对应的碎片和强度的键值对
     * @param sigmaSpacing
     * @return
     */
    public FeatureByPep getExperimentFeature(AnalyseDataDO dataDO, HashMap<String, Float> intensityMap, SigmaSpacing sigmaSpacing) {
        boolean featureFound = true;
        if (dataDO.getIntensityMap() == null || dataDO.getIntensityMap().size() == 0) {
            featureFound = false;
        }


        HashMap<String, RtIntensityPairsDouble> ionPeaks = new HashMap<>();
        HashMap<String, IntensityRtLeftRtRightPairs> ionPeakParams = new HashMap<>();
        List<Double> libraryIntensityList = new ArrayList<>();

        //对每一个chromatogram进行运算,dataDO中不含有ms1
        HashMap<String, double[]> noise1000Map = new HashMap<>();
        HashMap<String, Double[]> intensitiesMap = new HashMap<>();

        //将没有卷积到信号的CutInfo过滤掉,同时将Float类型的参数调整为Double类型进行计算
        for (String cutInfo : intensityMap.keySet()) {
            //获取对应的卷积数据
            Float[] intensityArray = dataDO.getIntensityMap().get(cutInfo);
            //如果没有卷积到信号,dataDO为null
            if (intensityArray == null) {
                continue;
            }
            Double[] intensityDoubleArray = new Double[intensityArray.length];
            for (int k = 0; k < intensityArray.length; k++) {
                intensityDoubleArray[k] = Double.parseDouble(intensityArray[k].toString());
            }
            intensitiesMap.put(cutInfo, intensityDoubleArray);
        }

        if (intensitiesMap.size() == 0) {
            return new FeatureByPep(false);
        }

        //计算GaussFilter
        Double[] rtDoubleArray = new Double[dataDO.getRtArray().length];
        for (int k = 0; k < rtDoubleArray.length; k++) {
            rtDoubleArray[k] = Double.parseDouble(dataDO.getRtArray()[k].toString());
        }
        PeptideSpectrum peptideSpectrum = new PeptideSpectrum(rtDoubleArray, intensitiesMap);


        HashMap<String, Double[]> smoothIntensitiesMap = gaussFilter.filter(rtDoubleArray, intensitiesMap, sigmaSpacing);

        //对每一个片段离子选峰
        for (String cutInfo : intensitiesMap.keySet()) {
            //计算两个信噪比
            double[] noises200 = signalToNoiseEstimator.computeSTN(rtDoubleArray, smoothIntensitiesMap.get(cutInfo), 200, 30);
            double[] noisesOri1000 = signalToNoiseEstimator.computeSTN(rtDoubleArray, intensitiesMap.get(cutInfo), 1000, 30);
            //根据信噪比和峰值形状选择最高峰,用降噪200及平滑过后的图去挑选Peak峰
            RtIntensityPairsDouble maxPeakPairs = peakPicker.pickMaxPeak(rtDoubleArray, smoothIntensitiesMap.get(cutInfo), noises200);

            //根据信噪比和最高峰选择谱图
            if (maxPeakPairs == null) {
                logger.info("Error: MaxPeakPairs were null!");
                continue;
            }
            IntensityRtLeftRtRightPairs intensityRtLeftRtRightPairs = chromatogramPicker.pickChromatogram(rtDoubleArray, intensitiesMap.get(cutInfo), smoothIntensitiesMap.get(cutInfo), noisesOri1000, maxPeakPairs);

            ionPeaks.put(cutInfo, maxPeakPairs);
            ionPeakParams.put(cutInfo, intensityRtLeftRtRightPairs);
            libraryIntensityList.add(Double.parseDouble(Float.toString(intensityMap.get(cutInfo))));
            noise1000Map.put(cutInfo, noisesOri1000);
        }

        if (intensitiesMap.size() == 0) {
            return new FeatureByPep(false);
        }
        List<PeakGroup> peakGroupFeatureList = featureFinder.findFeatures(peptideSpectrum, ionPeaks, ionPeakParams);

        FeatureByPep featureResult = new FeatureByPep(featureFound);
        featureResult.setPeakGroupFeatureList(peakGroupFeatureList);
        featureResult.setLibraryIntensityList(libraryIntensityList);
        featureResult.setPeptideSpectrum(peptideSpectrum);
        featureResult.setNoise1000Map(noise1000Map);

        return featureResult;
    }

    /**
     * get intensityGroup corresponding to peptideRef
     *
     * @param intensityGroupList intensity group of all peptides
     * @param peptideRef         chosen peptide
     * @return intensity group of peptideRef
     */
    private IntensityGroup getIntensityGroupByPep(List<IntensityGroup> intensityGroupList, String peptideRef) {
        for (IntensityGroup intensityGroup : intensityGroupList) {
            if (intensityGroup.getPeptideRef().equals(peptideRef)) {
                return intensityGroup;
            }
        }
        System.out.println("GetIntensityGroupByPep Error.");
        return null;
    }
}
