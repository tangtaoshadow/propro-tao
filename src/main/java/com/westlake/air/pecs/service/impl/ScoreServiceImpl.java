package com.westlake.air.pecs.service.impl;

import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.query.PageQuery;
import com.westlake.air.pecs.feature.*;
import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.score.*;
import com.westlake.air.pecs.domain.db.AnalyseOverviewDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.domain.db.simple.IntensityGroup;
import com.westlake.air.pecs.domain.db.simple.TransitionGroup;
import com.westlake.air.pecs.rtnormalizer.*;
import com.westlake.air.pecs.service.*;
import com.westlake.air.pecs.utils.MathUtil;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("scoreService")
public class ScoreServiceImpl implements ScoreService {

    public final Logger logger = LoggerFactory.getLogger(ScoreServiceImpl.class);

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
    @Autowired
    FeatureExtractor featureExtractor;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    ScoreService scoreService;

    @Override
    public ResultDO<SlopeIntercept> computeIRt(String overviewId, String iRtLibraryId,  Float sigma, Float spacing, TaskDO taskDO) {

        taskDO.addLog("开始获取肽段分组信息和强度信息");
        taskService.update(taskDO);

        List<TransitionGroup> groups = analyseDataService.getIrtTransitionGroup(overviewId, iRtLibraryId);
        List<IntensityGroup> intensityGroupList = transitionService.getIntensityGroup(iRtLibraryId);

        taskDO.addLog("分组信息获取完毕,开始处理数据");
        taskService.update(taskDO);
        List<List<ScoreRtPair>> scoreRtList = new ArrayList<>();
        List<Float> compoundRt = new ArrayList<>();
        ResultDO<SlopeIntercept> resultDO = new ResultDO<>();
        for(TransitionGroup group : groups){
            SlopeIntercept slopeIntercept = new SlopeIntercept();//void parameter
            FeatureByPep featureByPep = featureExtractor.getExperimentFeature(group, intensityGroupList, slopeIntercept, sigma, spacing);
            if(!featureByPep.isFeatureFound()){
                continue;
            }
            float groupRt = group.getRt().floatValue();
            List<ScoreRtPair> scoreRtPairs = RTNormalizerScorer.score(featureByPep.getRtIntensityPairsOriginList(), featureByPep.getExperimentFeatures(), featureByPep.getLibraryIntensityList(), featureByPep.getNoise1000List(), slopeIntercept, groupRt);
            scoreRtList.add(scoreRtPairs);
            compoundRt.add(group.getRt().floatValue());
        }
        taskDO.addLog("开始搜索最优特征");
        taskService.update(taskDO);
        List<RtPair> pairs = simpleFindBestFeature(scoreRtList, compoundRt);
        List<RtPair> pairsCorrected = removeOutlierIterative(pairs, Constants.MIN_RSQ, Constants.MIN_COVERAGE);
//        if(!computeBinnedCoverage( , pairsCorrected, Constants.RT_BINS, Constants.MIN_PEPTIDES_PER_BIN, Constants.MIN_BINS_FILLED)){
//            System.out.println("There were not enough bins with the minimal number of peptides.");
//        }
        if(pairsCorrected == null || pairsCorrected.size() < 2){
            logger.error(ResultCode.NOT_ENOUGH_IRT_PEPTIDES.getMessage());
            resultDO.setErrorResult(ResultCode.NOT_ENOUGH_IRT_PEPTIDES);
            return resultDO;
        }
        taskDO.addLog("最小二乘法线性拟合RTPairs");
        taskService.update(taskDO);
        SlopeIntercept slopeIntercept = fitRTPairs(pairsCorrected);
        resultDO.setSuccess(true);
        resultDO.setModel(slopeIntercept);
        //TODO: dealing with RTNormalizer results(not knowing the accuracy of final result)

        return resultDO;
    }

    @Override
    public ResultDO<SlopeIntercept> computeIRt(List<AnalyseDataDO> dataList, String iRtLibraryId, Float sigma, Float space) {

        List<TransitionGroup> groups = analyseDataService.getIrtTransitionGroup(dataList, iRtLibraryId);
        List<IntensityGroup> intensityGroupList = transitionService.getIntensityGroup(iRtLibraryId);

        List<List<ScoreRtPair>> scoreRtList = new ArrayList<>();
        List<Float> compoundRt = new ArrayList<>();
        ResultDO<SlopeIntercept> resultDO = new ResultDO<>();
        for(TransitionGroup group : groups){
            SlopeIntercept slopeIntercept = new SlopeIntercept();//void parameter
            FeatureByPep featureByPep = featureExtractor.getExperimentFeature(group, intensityGroupList, slopeIntercept, sigma, space);
            if(!featureByPep.isFeatureFound()){
                continue;
            }
            float groupRt = group.getRt().floatValue();
            List<ScoreRtPair> scoreRtPairs = RTNormalizerScorer.score(featureByPep.getRtIntensityPairsOriginList(), featureByPep.getExperimentFeatures(), featureByPep.getLibraryIntensityList(), featureByPep.getNoise1000List(), slopeIntercept, groupRt);
            scoreRtList.add(scoreRtPairs);
            compoundRt.add(group.getRt().floatValue());
        }

        List<RtPair> pairs = simpleFindBestFeature(scoreRtList, compoundRt);
        List<RtPair> pairsCorrected = removeOutlierIterative(pairs, Constants.MIN_RSQ, Constants.MIN_COVERAGE);
//
//        if(pairsCorrected == null || pairsCorrected.size() < 2){
//            logger.error(ResultCode.NOT_ENOUGH_IRT_PEPTIDES.getMessage());
//            resultDO.setErrorResult(ResultCode.NOT_ENOUGH_IRT_PEPTIDES);
//            return resultDO;
//        }

        SlopeIntercept slopeIntercept = fitRTPairs(pairs);
        resultDO.setSuccess(true);
        resultDO.setModel(slopeIntercept);

        return resultDO;
    }

    /**
     * get rt pairs for every peptideRef
     * @param scoresList peptideRef list of List<ScoreRtPair>
     * @param rt get from groupsResult.getModel()
     * @return rt pairs
     */
    private List<RtPair> simpleFindBestFeature(List<List<ScoreRtPair>> scoresList, List<Float> rt){

        List<RtPair> pairs = new ArrayList<>();

        for(int i=0; i<scoresList.size(); i++){
            List<ScoreRtPair> scores = scoresList.get(i);
            float max = Float.MIN_VALUE;
            RtPair rtPair = new RtPair();
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
        //TODO: @Nico
        minRsq = 0.866f;

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


}
