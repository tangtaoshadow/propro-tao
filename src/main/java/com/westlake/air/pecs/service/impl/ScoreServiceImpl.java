package com.westlake.air.pecs.service.impl;

import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairs;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
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
import com.westlake.air.pecs.scorer.*;
import com.westlake.air.pecs.service.*;
import com.westlake.air.pecs.utils.MathUtil;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
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
    @Autowired
    ChromatographicScorer chromatographicScorer;
    @Autowired
    DIAScorer diaScorer;
    @Autowired
    ElutionScorer elutionScorer;
    @Autowired
    LibraryScorer libraryScorer;
    @Autowired
    SwathLDAScorer swathLDAScorer;

    @Override
    public ResultDO<SlopeIntercept> computeIRt(String overviewId, String iRtLibraryId, SigmaSpacing sigmaSpacing, TaskDO taskDO) {

        taskDO.addLog("开始获取肽段分组信息和强度信息");
        taskService.update(taskDO);

        List<TransitionGroup> groups = analyseDataService.getIrtTransitionGroup(overviewId, iRtLibraryId);
        HashMap<String, IntensityGroup> intensityGroupMap = transitionService.getIntensityGroupMap(iRtLibraryId);

        taskDO.addLog("分组信息获取完毕,开始处理数据");
        taskService.update(taskDO);
        List<List<ScoreRtPair>> scoreRtList = new ArrayList<>();
        List<Double> compoundRt = new ArrayList<>();
        ResultDO<SlopeIntercept> resultDO = new ResultDO<>();
        for(TransitionGroup group : groups){
            FeatureByPep featureByPep = featureExtractor.getExperimentFeature(group, intensityGroupMap.get(group.getPeptideRef()), sigmaSpacing);
            if(!featureByPep.isFeatureFound()){
                continue;
            }
            double groupRt = group.getRt();
            List<ScoreRtPair> scoreRtPairs = RTNormalizerScorer.score(featureByPep.getRtIntensityPairsOriginList(), featureByPep.getExperimentFeatures(), featureByPep.getLibraryIntensityList(), featureByPep.getNoise1000List(), new SlopeIntercept(), groupRt);
            scoreRtList.add(scoreRtPairs);
            compoundRt.add(groupRt);
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
    public ResultDO<SlopeIntercept> computeIRt(List<AnalyseDataDO> dataList, String iRtLibraryId, SigmaSpacing sigmaSpacing) {

        List<TransitionGroup> groups = analyseDataService.getIrtTransitionGroup(dataList, iRtLibraryId);
        HashMap<String, IntensityGroup> intensityGroupMap = transitionService.getIntensityGroupMap(iRtLibraryId);

        List<List<ScoreRtPair>> scoreRtList = new ArrayList<>();
        List<Double> compoundRt = new ArrayList<>();
        ResultDO<SlopeIntercept> resultDO = new ResultDO<>();
        for(TransitionGroup group : groups){
            SlopeIntercept slopeIntercept = new SlopeIntercept();//void parameter
            FeatureByPep featureByPep = featureExtractor.getExperimentFeature(group, intensityGroupMap.get(group.getPeptideRef()), sigmaSpacing);
            if(!featureByPep.isFeatureFound()){
                continue;
            }
            float groupRt = group.getRt().floatValue();
            List<ScoreRtPair> scoreRtPairs = RTNormalizerScorer.score(featureByPep.getRtIntensityPairsOriginList(), featureByPep.getExperimentFeatures(), featureByPep.getLibraryIntensityList(), featureByPep.getNoise1000List(), slopeIntercept, groupRt);
            scoreRtList.add(scoreRtPairs);
            compoundRt.add(group.getRt());
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

    @Override
    public void score(List<AnalyseDataDO> dataList, SlopeIntercept slopeIntercept, String libraryId, SigmaSpacing sigmaSpacing) {

        List<TransitionGroup> groups = analyseDataService.getTransitionGroup(dataList);

        HashMap<String, IntensityGroup> intensityGroupMap = transitionService.getIntensityGroupMap(libraryId);
        List<PecsScore> pecsScoreList = new ArrayList<>();

        for (TransitionGroup group : groups) {
            List<FeatureScores> featureScoresList = new ArrayList<>();
            FeatureByPep featureByPep = featureExtractor.getExperimentFeature(group, intensityGroupMap.get(group.getPeptideRef()), sigmaSpacing);

            if(!featureByPep.isFeatureFound()){
                continue;
            }
            List<List<ExperimentFeature>> experimentFeatures = featureByPep.getExperimentFeatures();
            List<RtIntensityPairsDouble> chromatogramList = featureByPep.getRtIntensityPairsOriginList();
            List<Float> libraryIntensityList = featureByPep.getLibraryIntensityList();
            List<double[]> noise1000List = featureByPep.getNoise1000List();
            List<Float> productMzList = new ArrayList<>();
            for (AnalyseDataDO dataDO : group.getDataMap().values()){
                productMzList.add(dataDO.getMz());
            }

            //TODO  mrmFeature - peptideRef - ...
            //数据格式见下面的new
            //spectrum: get by peptideRef RT(nearest)
            //productChargeList: product charge of found transition, list int
            //unimodHashMap: unimod HashMap of peptide(get by peptideRef)
            //sequence: sequence of peptide(get by peptideRef)
            List<Float> spectrumMzArray = new ArrayList<>();
            List<Float> spectrumIntArray = new ArrayList<>();

            List<Integer> productChargeList = new ArrayList<>();
            for(AnalyseDataDO data : group.getDataMap().values()){
                String cutInfo = data.getCutInfo();
                if(cutInfo.contains("^")){
                    productChargeList.add(Integer.parseInt(cutInfo.split("\\^")[1]));
                }else{
                    productChargeList.add(1);
                }
            }

            HashMap<Integer, String> unimodHashMap = group.getUnimodMap();

            String sequence = "";
            //for each mrmFeature, calculate scores

            for(List<ExperimentFeature> experimentFeatureList : experimentFeatures) {

                FeatureScores featureScores = new FeatureScores();
                chromatographicScorer.calculateChromatographicScores(chromatogramList, experimentFeatureList, libraryIntensityList, noise1000List, featureScores);
                chromatographicScorer.calculateIntensityScore(experimentFeatureList, featureScores);
//                diaScorer.calculateDiaMassDiffScore(productMzList, spectrumMzArray, spectrumIntArray, libraryIntensityList, featureScores);
//                diaScorer.calculateDiaIsotopeScores(experimentFeatureList, productMzList, spectrumMzArray, spectrumIntArray, productChargeList, featureScores);
////                //TODO @Nico charge from transition?
//                diaScorer.calculateBYIonScore(spectrumMzArray, spectrumIntArray, unimodHashMap, sequence, 1, featureScores);
//                elutionScorer.calculateElutionModelScore(experimentFeatureList, featureScores);
                libraryScorer.calculateIntensityScore(experimentFeatureList, featureScores);
                libraryScorer.calculateLibraryScores(experimentFeatureList, libraryIntensityList, slopeIntercept, group.getRt().floatValue(), featureScores);
                swathLDAScorer.calculateSwathLdaPrescore(featureScores);

                featureScoresList.add(featureScores);
            }

            PecsScore pecsScore = new PecsScore();
            pecsScore.setPeptideRef(group.getPeptideRef());
            pecsScore.setFeatureScoresList(featureScoresList);
            pecsScoreList.add(pecsScore);
        }
        //have pecsScoreList
    }

    /**
     * get rt pairs for every peptideRef
     * @param scoresList peptideRef list of List<ScoreRtPair>
     * @param rt get from groupsResult.getModel()
     * @return rt pairs
     */
    private List<RtPair> simpleFindBestFeature(List<List<ScoreRtPair>> scoresList, List<Double> rt){

        List<RtPair> pairs = new ArrayList<>();

        for(int i=0; i<scoresList.size(); i++){
            List<ScoreRtPair> scores = scoresList.get(i);
            double max = Double.MIN_VALUE;
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
    private List<RtPair> removeOutlierIterative(List<RtPair> pairs, double minRsq, float minCoverage){

        int pairsSize = pairs.size();
        if( pairsSize < 3){
            return null;
        }

        //获取斜率和截距
        double rsq = 0;
        double[] coEff;

        WeightedObservedPoints obs = new WeightedObservedPoints();
        while(pairs.size() >= pairsSize * minCoverage && rsq< minRsq) {
            obs.clear();
            for(RtPair rtPair:pairs){
                obs.add(rtPair.getExpRt(),rtPair.getTheoRt());
            }
            PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
            coEff = fitter.fit(obs.toList());

            rsq =  MathUtil.getRsq(pairs);
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
    private boolean computeBinnedCoverage(double[] rtRange, List<RtPair> pairsCorrected, int rtBins, int minPeptidesPerBin, int minBinsFilled){
        int[] binCounter = new int[rtBins];
        double rtDistance = rtRange[1] - rtRange[0];

        //获得theorRt部分的分布
        for(RtPair pair: pairsCorrected){
            double percent = (pair.getTheoRt() - rtRange[0])/rtDistance;
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
        slopeIntercept.setIntercept((float) coeff[0]);
        return slopeIntercept;
    }


}
