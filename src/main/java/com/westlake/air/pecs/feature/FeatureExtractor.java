package com.westlake.air.pecs.feature;

import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairs;
import com.westlake.air.pecs.domain.bean.score.ExperimentFeature;
import com.westlake.air.pecs.domain.bean.score.FeatureByPep;
import com.westlake.air.pecs.domain.bean.score.IntensityRtLeftRtRightPairs;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.simple.IntensityGroup;
import com.westlake.air.pecs.domain.db.simple.TransitionGroup;
import com.westlake.air.pecs.rtnormalizer.*;
import com.westlake.air.pecs.service.AnalyseDataService;
import com.westlake.air.pecs.service.AnalyseOverviewService;
import com.westlake.air.pecs.service.TaskService;
import com.westlake.air.pecs.service.TransitionService;
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

    public FeatureByPep getExperimentFeature(TransitionGroup group, List<IntensityGroup> intensityGroupList, SlopeIntercept slopeIntercept, Float sigma, Float spacing) {
        boolean featureFound = true;
        if (group.getDataMap() == null || group.getDataMap().size() == 0) {
            featureFound = false;
        }
        if(sigma == null){
            sigma = 30f;
        }
        if(spacing == null){
            spacing = 0.01f;
        }
        String peptideRef = group.getPeptideRef();
        List<RtIntensityPairs> rtIntensityPairsOriginList = new ArrayList<>();
        List<RtIntensityPairs> maxRtIntensityPairsList = new ArrayList<>();
        List<IntensityRtLeftRtRightPairs> intensityRtLeftRtRightPairsList = new ArrayList<>();

        //得到peptideRef对应的intensityList
        List<Float> libraryIntensityList = new ArrayList<>();
        IntensityGroup intensityGroupByPep = getIntensityGroupByPep(intensityGroupList, peptideRef);
        assert intensityGroupByPep != null;
        List<Float> libraryIntensityListAll = intensityGroupByPep.getIntensityList();
        assert libraryIntensityListAll.size() != 0;
        int count = 0;

        //对每一个chromatogram进行运算，dataDO中不含有ms1
        for (AnalyseDataDO dataDO : group.getDataMap().values()) {

            //如果没有卷积到信号，dataDO为null
            if (dataDO == null) {
                count++;
                continue;
            }

            //得到卷积后的chromatogram的RT、Intensity对
            RtIntensityPairs rtIntensityPairsOrigin = new RtIntensityPairs(dataDO.getRtArray(), dataDO.getIntensityArray());
            if(!(slopeIntercept.getIntercept() == 0f && slopeIntercept.getSlope() == 0f)) {
                rtIntensityPairsOrigin = chromatogramFilter.pickChromatogramByRt(rtIntensityPairsOrigin, group.getRt().floatValue(), slopeIntercept);
            }

            //进行高斯平滑，得到平滑后的chromatogram
            RtIntensityPairs rtIntensityPairsAfterSmooth = gaussFilter.filter(rtIntensityPairsOrigin, sigma, spacing);

            //计算两个信噪比
            //@Nico parameter configured
            float[] noises200 = signalToNoiseEstimator.computeSTN(rtIntensityPairsAfterSmooth, 200, 30);
            float[] noises1000 = signalToNoiseEstimator.computeSTN(rtIntensityPairsAfterSmooth, 1000, 30);

            //根据信噪比和峰值形状选择最高峰
            RtIntensityPairs maxPeakPairs = peakPicker.pickMaxPeak(rtIntensityPairsAfterSmooth, noises200);

            //根据信噪比和最高峰选择谱图
            IntensityRtLeftRtRightPairs intensityRtLeftRtRightPairs = chromatogramPicker.pickChromatogram(rtIntensityPairsOrigin, rtIntensityPairsAfterSmooth, noises1000, maxPeakPairs);
            rtIntensityPairsOriginList.add(rtIntensityPairsOrigin);
            maxRtIntensityPairsList.add(maxPeakPairs);
            intensityRtLeftRtRightPairsList.add(intensityRtLeftRtRightPairs);
            libraryIntensityList.add(libraryIntensityListAll.get(count));
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

        return featureResult;
    }

    /**
     * get intensityGroup corresponding to peptideRef
     * @param intensityGroupList intensity group of all peptides
     * @param peptideRef chosen peptide
     * @return intensity group of peptideRef
     */
    private IntensityGroup getIntensityGroupByPep(List<IntensityGroup> intensityGroupList, String peptideRef){
        for(IntensityGroup intensityGroup: intensityGroupList){
            if(intensityGroup.getPeptideRef().equals(peptideRef)){
                return intensityGroup;
            }
        }
        System.out.println("GetIntensityGroupByPep Error.");
        return null;
    }
}
