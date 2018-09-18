package com.westlake.air.pecs.feature;

import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.pecs.domain.bean.score.ExperimentFeature;
import com.westlake.air.pecs.domain.bean.score.FeatureByPep;
import com.westlake.air.pecs.domain.bean.score.IntensityRtLeftRtRightPairs;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.simple.IntensityGroup;
import com.westlake.air.pecs.domain.db.simple.TransitionGroup;
import com.westlake.air.pecs.rtnormalizer.ChromatogramFilter;
import com.westlake.air.pecs.rtnormalizer.RTNormalizerScorer;
import com.westlake.air.pecs.service.AnalyseDataService;
import com.westlake.air.pecs.service.AnalyseOverviewService;
import com.westlake.air.pecs.service.TaskService;
import com.westlake.air.pecs.service.TransitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
    TransitionService transitionService;
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
    RTNormalizerScorer RTNormalizerScorer;
    @Autowired
    TaskService taskService;
    @Autowired
    ChromatogramFilter chromatogramFilter;

    public FeatureByPep getExperimentFeature(TransitionGroup group, IntensityGroup intensityGroupByPep, SigmaSpacing sigmaSpacing) {
        boolean featureFound = true;
        if (group.getDataMap() == null || group.getDataMap().size() == 0) {
            featureFound = false;
        }

        List<RtIntensityPairsDouble> rtIntensityPairsOriginList = new ArrayList<>();
        List<RtIntensityPairsDouble> maxRtIntensityPairsList = new ArrayList<>();
        List<IntensityRtLeftRtRightPairs> intensityRtLeftRtRightPairsList = new ArrayList<>();

        //得到peptideRef对应的intensityList
        List<Float> libraryIntensityList = new ArrayList<>();
        List<Float> libraryIntensityListAll = intensityGroupByPep.getIntensityList();
        int count = 0;

        int count1 = 0, count2 = 0, count3 = 0, count4 = 0, count5 = 0, count6 = 0;
        //对每一个chromatogram进行运算,dataDO中不含有ms1
        List<double[]> noise1000List = new ArrayList<>();
        for (AnalyseDataDO dataDO : group.getDataMap().values()) {

            //如果没有卷积到信号,dataDO为null
            if (!dataDO.getIsHit()) {
                count++;
                continue;
            }

            //得到卷积后的chromatogram的RT,Intensity对
            RtIntensityPairsDouble rtIntensityPairsOrigin = new RtIntensityPairsDouble(dataDO.getRtArray(), dataDO.getIntensityArray());

            //进行高斯平滑,得到平滑后的chromatogram
            long start = System.currentTimeMillis();
            RtIntensityPairsDouble rtIntensityPairsAfterSmooth = gaussFilter.filter(rtIntensityPairsOrigin, sigmaSpacing);
            count1 += (System.currentTimeMillis() - start);
            //计算两个信噪比
            //@Nico parameter configured
            //TODO legacy or corrected noise1000 is not the same
            double[] noises200 = signalToNoiseEstimator.computeSTN(rtIntensityPairsAfterSmooth, 200, 30);
//            double[] noises1000 = signalToNoiseEstimator.computeSTN(rtIntensityPairsAfterSmooth, 1000, 30);
            double[] noisesOri1000 = signalToNoiseEstimator.computeSTN(rtIntensityPairsOrigin, 1000, 30);
            //根据信噪比和峰值形状选择最高峰
            RtIntensityPairsDouble maxPeakPairs = peakPicker.pickMaxPeak(rtIntensityPairsAfterSmooth, noises200);


            //根据信噪比和最高峰选择谱图
            IntensityRtLeftRtRightPairs intensityRtLeftRtRightPairs = chromatogramPicker.pickChromatogram(rtIntensityPairsOrigin, rtIntensityPairsAfterSmooth, noisesOri1000, maxPeakPairs);
            rtIntensityPairsOriginList.add(rtIntensityPairsOrigin);
            maxRtIntensityPairsList.add(maxPeakPairs);
            intensityRtLeftRtRightPairsList.add(intensityRtLeftRtRightPairs);
            libraryIntensityList.add(libraryIntensityListAll.get(count));
            noise1000List.add(noisesOri1000);
            count++;
        }
        if (rtIntensityPairsOriginList.size() == 0) {
            featureFound = false;
        }
        List<List<ExperimentFeature>> experimentFeatures = featureFinder.findFeatures(rtIntensityPairsOriginList, maxRtIntensityPairsList, intensityRtLeftRtRightPairsList);

        FeatureByPep featureResult = new FeatureByPep();
        featureResult.setFeatureFound(featureFound);
        featureResult.setExperimentFeatures(experimentFeatures);
        featureResult.setLibraryIntensityList(libraryIntensityList);
        featureResult.setRtIntensityPairsOriginList(rtIntensityPairsOriginList);
        featureResult.setNoise1000List(noise1000List);

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
