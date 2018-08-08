package com.westlake.air.pecs.service.impl;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.*;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.AnalyseOverviewDO;
import com.westlake.air.pecs.domain.db.simple.IntensityGroup;
import com.westlake.air.pecs.domain.db.simple.TransitionGroup;
import com.westlake.air.pecs.domain.query.AnalyseDataQuery;
import com.westlake.air.pecs.domain.query.TransitionQuery;
import com.westlake.air.pecs.rtnormalizer.*;
import com.westlake.air.pecs.domain.bean.ExperimentFeature;
import com.westlake.air.pecs.service.AnalyseDataService;
import com.westlake.air.pecs.service.AnalyseOverviewService;
import com.westlake.air.pecs.service.RTNormalizerService;
import com.westlake.air.pecs.service.TransitionService;
import com.westlake.air.pecs.utils.MathUtil;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
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
    PeakScorer peakScorer;

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

        ResultDO<List<TransitionGroup>> groupsResult = analyseDataService.getTransitionGroup(overviewId, overviewDOResult.getModel().getVLibraryId());
        if(groupsResult.isFailed()){
            ResultDO resultDO = new ResultDO(false);
            resultDO.setErrorResult(groupsResult.getMsgCode(), groupsResult.getMsgInfo());
            return resultDO;
        }

        List<IntensityGroup> intensityGroupList = transitionService.getIntensityGroup(overviewDOResult.getModel().getVLibraryId());

        List<List<ScoreRtPair>> scoresList = new ArrayList<>();
        List<Float> compoundRt = new ArrayList<>();
        ResultDO<SlopeIntercept> slopeInterceptResultDO = new ResultDO<>();
        for(TransitionGroup group : groupsResult.getModel()){
            if(group.getDataList() == null || group.getDataList().size() == 0){
                continue;
            }

            compoundRt.add(group.getRt().floatValue());
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
            List<List<ExperimentFeature>> experimentFeatures = featureFinder.findFeatures(rtIntensityPairsOriginList, maxRtIntensityPairsList, intensityRtLeftRtRightPairsList);
            //list of library intensity List(Double)
            String peptideRef = group.getPeptideRef();
            List<ScoreRtPair> scoreRtPairs = peakScorer.score(rtIntensityPairsOriginList, experimentFeatures, getIntensityGroupByPep(intensityGroupList, peptideRef).getIntensityList(), 1000, 30);
            scoresList.add(scoreRtPairs);
            compoundRt.add(group.getRt().floatValue());
        }
        List<RtPair> pairs = simpleFindBestFeature(scoresList, compoundRt);
        List<RtPair> pairsCorrected = removeOutlierIterative(pairs, Constants.MIN_RSQ, Constants.MIN_COVERAGE);
//        if(!computeBinnedCoverage( , pairsCorrected, Constants.RT_BINS, Constants.MIN_PEPTIDES_PER_BIN, Constants.MIN_BINS_FILLED)){
//            System.out.println("There were not enough bins with the minimal number of peptides.");
//        }
        if(pairsCorrected == null || pairsCorrected.size() < 2){
            System.out.println("There are less than 2 iRT normalization peptides, not enough for an RT correction.");
            slopeInterceptResultDO.setMsgInfo("There are less than 2 iRT normalization peptides, not enough for an RT correction.");
            return slopeInterceptResultDO;
        }

        SlopeIntercept slopeIntercept = fitRTPairs(pairsCorrected);
        slopeInterceptResultDO.setSuccess(true);
        slopeInterceptResultDO.setModel(slopeIntercept);

        return slopeInterceptResultDO;
    }

    /**
     * get rt pairs for every peptideRef
     * @param scoresList peptideRef list of List<ScoreRtPair>
     * @param rt get from groupsResult.getModel()
     * @return rt pairs
     */
    private List<RtPair> simpleFindBestFeature(List<List<ScoreRtPair>> scoresList, List<Float> rt){
        float max = Float.MIN_VALUE;
        List<RtPair> pairs = new ArrayList<>();
        RtPair rtPair = new RtPair();
        for(int i=0; i<scoresList.size(); i++){
            List<ScoreRtPair> scores = scoresList.get(i);
            //find max score's rt
            for(int j=0; j<scores.size(); j++){
                if(scores.get(j).getScore() > max){
                    max = scores.get(j).getScore();
                    rtPair.setExpRt(scores.get(j).getRt());
                }
            }
            rtPair.setTheoRt(rt.get(i));
            pairs.add(rtPair);
        }
        return pairs;
    }

    /**
     * 先进行线性拟合，每次从pairs中选取一个residual最大的点丢弃，获得pairsCorrected
     * @param pairs RTPairs
     * @param minRsq goal of iteration
     * @param minCoverage limit of picking
     * @return pairsCorrected
     */
    private List<RtPair> removeOutlierIterative(List<RtPair> pairs, float minRsq, float minCoverage){

        int pairsSize = pairs.size();
        if( pairsSize < 3){
            return null;
        }

        //获取斜率和截距
        float rsq = 0;
        double[] coEff;

        WeightedObservedPoints obs = new WeightedObservedPoints();
        while(pairs.size() >= pairsSize * minCoverage && rsq< minRsq) {
            obs.clear();
            for(RtPair rtPair:pairs){
                obs.add(rtPair.getExpRt(),rtPair.getTheoRt());
            }
            PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
            coEff = fitter.fit(obs.toList());

            rsq = MathUtil.getRsq(pairs);
            if (rsq < minRsq) {
                // calculate residual and get max index
                float res, max = 0;
                int maxIndex = 0;
                for (int i = 0; i < pairs.size(); i++) {
                    res = (float) (Math.abs(pairs.get(i).getTheoRt() - (coEff[0] + coEff[1] * pairs.get(i).getExpRt())));
                    if (res > max) {
                        max = res;
                        maxIndex = i;
                    }
                }
                //remove outlier of pairs iteratively
                pairs.remove(maxIndex);
            }
        }
        if(rsq < minRsq){
            System.out.println("RTNormalizer: unable to perform outlier detection.");
            return null; //TODO: RTNormalizer: unable to perform outlier detection
        }else {
            return pairs;
        }
    }

    /**
     * 判断是否对RT空间实现了正常密度的覆盖
     * @param rtRange getRTRange
     * @param pairsCorrected remove outlier之后的pairs
     * @param rtBins 需要分成的bin的数量
     * @param minPeptidesPerBin 每个bin最小分到的数量
     * @param minBinsFilled 需要满足↑条件的bin的数量
     * @return boolean 是否覆盖
     */
    private boolean computeBinnedCoverage(float[] rtRange, List<RtPair> pairsCorrected, int rtBins, int minPeptidesPerBin, int minBinsFilled){
        int[] binCounter = new int[rtBins];
        float rtDistance = rtRange[1] - rtRange[0];

        //获得theorRt部分的分布
        for(RtPair pair: pairsCorrected){
            float percent = (pair.getTheoRt() - rtRange[0])/rtDistance;
            int bin = (int)(percent * rtBins);
            if(bin>=rtBins){
                bin = rtBins -1;
            }
            binCounter[bin] ++;
        }

        //判断分布是否覆盖
        int binFilled = 0;
        for(int binCount: binCounter){
            if(binCount >= minPeptidesPerBin) binFilled++;
        }
        return binFilled >= minBinsFilled;
    }

    /**
     * 最小二乘法线性拟合RTPairs
     * @param rtPairs <exp_rt, theor_rt>
     * @return 斜率和截距
     */
    private SlopeIntercept fitRTPairs(List<RtPair> rtPairs){
        WeightedObservedPoints obs = new WeightedObservedPoints();
        for(RtPair rtPair:rtPairs){
            obs.add(rtPair.getExpRt(),rtPair.getTheoRt());
        }
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
        double[] coeff = fitter.fit(obs.toList());
        SlopeIntercept slopeIntercept = new SlopeIntercept();
        slopeIntercept.setSlope((float)coeff[1]);
        slopeIntercept.setIntercept((float)coeff[0]);
        return slopeIntercept;
    }

    private IntensityGroup getIntensityGroupByPep(List<IntensityGroup> intensityGroupList, String peptideRef){
        for(IntensityGroup intensityGroup: intensityGroupList){
            if(intensityGroup.getPeptideRef().equals(peptideRef)){
                return intensityGroup;
            }
        }
        return null;
    }
}
