package com.westlake.air.pecs.service.impl;

import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.IntensityRtLeftRtRightPairs;
import com.westlake.air.pecs.domain.bean.RtIntensityPairs;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.AnalyseOverviewDO;
import com.westlake.air.pecs.domain.db.simple.TransitionGroup;
import com.westlake.air.pecs.domain.query.AnalyseDataQuery;
import com.westlake.air.pecs.rtnormalizer.*;
import com.westlake.air.pecs.service.AnalyseDataService;
import com.westlake.air.pecs.service.AnalyseOverviewService;
import com.westlake.air.pecs.service.RTNormalizerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("rtNormalizerService")
public class RtNormalizerServiceImpl implements RTNormalizerService {

    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
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

    @Override
    public ResultDO compute(String overviewId, Float sigma, Float spacing) {
        if(sigma == null){
            sigma = 30f;
        }
        if(spacing == null){
            spacing = 0.01f;
        }
        ResultDO<AnalyseOverviewDO> overviewDOResult = analyseOverviewService.getById(overviewId);
        if(overviewDOResult.isFailed()){
            return ResultDO.buildError(ResultCode.ANALYSE_OVERVIEW_NOT_EXISTED);
        }

        AnalyseDataQuery query = new AnalyseDataQuery();
        query.setLibraryId(overviewDOResult.getModel().getVLibraryId());
        query.setOverviewId(overviewId);
        ResultDO<List<TransitionGroup>> groupsResult = analyseDataService.getTransitionGroup(query,true);
        if(groupsResult.isFailed()){
            ResultDO resultDO = new ResultDO(false);
            resultDO.setErrorResult(groupsResult.getMsgCode(), groupsResult.getMsgInfo());
            return resultDO;
        }

        for(TransitionGroup group : groupsResult.getModel()){
            if(group.getDataList() == null || group.getDataList().size() == 0){
                continue;
            }

            List<RtIntensityPairs> rtIntensityPairsOriginList = new ArrayList<>();
            List<RtIntensityPairs> maxRtIntensityPairsList = new ArrayList<>();
            List<IntensityRtLeftRtRightPairs> intensityRtLeftRtRightPairsList = new ArrayList<>();
            for(AnalyseDataDO dataDO : group.getDataList()){
                //不考虑MS1的卷积结果
                if(dataDO.getMsLevel() == 1){
                    continue;
                }
                RtIntensityPairs rtIntensityPairsOrigin = new RtIntensityPairs(dataDO.getRtArray(), dataDO.getIntensityArray());
                RtIntensityPairs rtIntensityPairsAfterSmooth = gaussFilter.filter(rtIntensityPairsOrigin, sigma, spacing);
                //TODO 需要排插一下这两个入参的情况,此处的入参直接写为30,2000
                //计算两个信噪比
                float[] noises200 = signalToNoiseEstimator.computeSTN(rtIntensityPairsAfterSmooth, 200, 30);
                float[] noises1000 = signalToNoiseEstimator.computeSTN(rtIntensityPairsAfterSmooth, 1000, 30);
                //根据信噪比和峰值形状选择最高峰
                RtIntensityPairs maxPeakPairs = peakPicker.pickMaxPeak(rtIntensityPairsAfterSmooth, noises200);
                //根据信噪比和最高峰选择谱图
                IntensityRtLeftRtRightPairs intensityRtLeftRtRightPairs = chromatogramPicker.pickChromatogram(rtIntensityPairsAfterSmooth, noises1000, maxPeakPairs);
                rtIntensityPairsOriginList.add(rtIntensityPairsOrigin);
                maxRtIntensityPairsList.add(maxPeakPairs);
                intensityRtLeftRtRightPairsList.add(intensityRtLeftRtRightPairs);
            }
            featureFinder.findFeatures(rtIntensityPairsOriginList, maxRtIntensityPairsList, intensityRtLeftRtRightPairsList);
        }
        return null;
    }
}
