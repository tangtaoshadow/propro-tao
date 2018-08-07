package com.westlake.air.pecs.service.impl;

import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.RtIntensityPairs;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.AnalyseOverviewDO;
import com.westlake.air.pecs.domain.db.simple.TransitionGroup;
import com.westlake.air.pecs.domain.query.AnalyseDataQuery;
import com.westlake.air.pecs.rtnormalizer.GaussFilter;
import com.westlake.air.pecs.rtnormalizer.PeakPicker;
import com.westlake.air.pecs.rtnormalizer.SignalToNoiseEstimator;
import com.westlake.air.pecs.service.AnalyseDataService;
import com.westlake.air.pecs.service.AnalyseOverviewService;
import com.westlake.air.pecs.service.RTNormalizerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

            for(AnalyseDataDO dataDO : group.getDataList()){
                RtIntensityPairs pairs = new RtIntensityPairs(dataDO.getRtArray(), dataDO.getIntensityArray());
                pairs = gaussFilter.filter(pairs, sigma, spacing);
                //TODO 需要排插一下这两个入参的情况,此处的入参直接写为30,2000
                //计算信噪比
                float[] noises = signalToNoiseEstimator.computeSTN(pairs, 30, 200);

                //根据信噪比和峰值形状选择最高峰
                peakPicker.pickMaxPeak(pairs, noises);
            }
        }
        return null;
    }
}
