package com.westlake.air.propro.service.impl;

import com.alibaba.fastjson.JSON;
import com.westlake.air.propro.algorithm.fitter.LinearFitter;
import com.westlake.air.propro.constants.Constants;
import com.westlake.air.propro.constants.ScoreType;
import com.westlake.air.propro.dao.ConfigDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.aird.Compressor;
import com.westlake.air.propro.domain.bean.aird.WindowRange;
import com.westlake.air.propro.domain.bean.analyse.*;
import com.westlake.air.propro.domain.bean.irt.IrtResult;
import com.westlake.air.propro.domain.db.simple.TargetPeptide;
import com.westlake.air.propro.domain.params.LumsParams;
import com.westlake.air.propro.domain.bean.score.*;
import com.westlake.air.propro.domain.db.*;
import com.westlake.air.propro.domain.query.PeptideQuery;
import com.westlake.air.propro.algorithm.peak.*;
import com.westlake.air.propro.algorithm.parser.AirdFileParser;
import com.westlake.air.propro.algorithm.feature.RtNormalizerScorer;
import com.westlake.air.propro.algorithm.feature.*;
import com.westlake.air.propro.service.*;
import com.westlake.air.propro.utils.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.RandomAccessFile;
import java.util.*;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-15 10:05
 */
@Service("scoresService")
public class ScoreServiceImpl implements ScoreService {

    public final Logger logger = LoggerFactory.getLogger(ScoreServiceImpl.class);

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
    RtNormalizerScorer rtNormalizerScorer;
    @Autowired
    TaskService taskService;
    @Autowired
    FeatureExtractor featureExtractor;
    @Autowired
    ExperimentService experimentService;
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
    @Autowired
    ConfigDAO configDAO;
    @Autowired
    AirdFileParser airdFileParser;
    @Autowired
    LinearFitter linearFitter;
    @Autowired
    SwathIndexService swathIndexService;

    @Override
    public ResultDO<IrtResult> computeIRt(List<AnalyseDataDO> dataList, String iRtLibraryId, SigmaSpacing sigmaSpacing) throws Exception {

        HashMap<String, TargetPeptide> ttMap = peptideService.getTPMap(new PeptideQuery(iRtLibraryId));

        List<List<ScoreRtPair>> scoreRtList = new ArrayList<>();
        List<Double> compoundRt = new ArrayList<>();
        ResultDO<IrtResult> resultDO = new ResultDO<>();
        double minGroupRt = Double.MAX_VALUE, maxGroupRt = Double.MIN_VALUE;
        for (AnalyseDataDO dataDO : dataList) {
            PeptideFeature peptideFeature = featureExtractor.getExperimentFeature(dataDO, ttMap.get(dataDO.getPeptideRef() + "_" + dataDO.getIsDecoy()).buildIntensityMap(), sigmaSpacing);
            if (!peptideFeature.isFeatureFound()) {
                continue;
            }
            double groupRt = dataDO.getRt();
            if (groupRt > maxGroupRt){
                maxGroupRt = groupRt;
            }
            if (groupRt < minGroupRt){
                minGroupRt = groupRt;
            }
            List<ScoreRtPair> scoreRtPairs = rtNormalizerScorer.score(peptideFeature.getPeakGroupList(), peptideFeature.getNormedLibIntMap(), groupRt);
            if (scoreRtPairs.size() == 0){
                continue;
            }
            scoreRtList.add(scoreRtPairs);
            compoundRt.add(groupRt);
        }

        List<Pair<Double,Double>> pairs = simpleFindBestFeature(scoreRtList, compoundRt);
//        List<Pair<Double,Double>> pairsCorrected = removeOutlierIterative(pairs, Constants.MIN_RSQ, Constants.MIN_COVERAGE);
//        if (pairsCorrected == null || pairsCorrected.size() < 2) {
//            logger.error(ResultCode.NOT_ENOUGH_IRT_PEPTIDES.getMessage());
//            resultDO.setErrorResult(ResultCode.NOT_ENOUGH_IRT_PEPTIDES);
//            return resultDO;
//        }

//        SlopeIntercept slopeIntercept = linearFitter.leastSquare(pairsCorrected);
        double delta = (maxGroupRt - minGroupRt)/30d;
//        List<Pair<Double,Double>> pairsCorrected = removeOutlierIterative(pairs, Constants.MIN_RSQ, Constants.MIN_COVERAGE, delta);
        List<Pair<Double,Double>> pairsCorrected = chooseReliablePairs(pairs, delta);
//        int choosedPointCount = pairsCorrected.size();
//        if (choosedPointCount <= 3){
//
//        }else if (choosedPointCount <= pairs.size()/2){
//
//        }
        System.out.println("choose finish ------------------------");
        IrtResult irtResult = new IrtResult();

        List<Double[]> selectedList = new ArrayList<>();
        List<Double[]> unselectedList = new ArrayList<>();
        for (int i = 0; i < pairs.size(); i++) {
            if(pairsCorrected.contains(pairs.get(i))){
                selectedList.add(new Double[]{pairs.get(i).getLeft(),pairs.get(i).getRight()});
            }else{
                unselectedList.add(new Double[]{pairs.get(i).getLeft(),pairs.get(i).getRight()});
            }
        }
        irtResult.setSelectedPairs(selectedList);
        irtResult.setUnselectedPairs(unselectedList);
        SlopeIntercept slopeIntercept = linearFitter.proproFit(pairsCorrected, delta);
        irtResult.setSi(slopeIntercept);
        resultDO.setSuccess(true);
        resultDO.setModel(irtResult);

        return resultDO;
    }


    @Override
    public void scoreForOne(AnalyseDataDO dataDO, TargetPeptide peptide, TreeMap<Float, MzIntensityPairs> rtMap, LumsParams input) {

        if (dataDO.getIntensityMap() == null || dataDO.getIntensityMap().size() <= 2) {
//            logger.info((dataDO.getIsDecoy()?"[Decoy]":"[Target]")+"数据的离子片段少于2个,属于无效数据:PeptideRef:" + dataDO.getPeptideRef());
            dataDO.setIdentifiedStatus(AnalyseDataDO.IDENTIFIED_STATUS_NO_FIT);
            return;
        }
        dataDO.setIsUnique(peptide.getIsUnique());

        //获取标准库中对应的PeptideRef组
        //重要步骤,"或许是目前整个工程最重要的核心算法--选峰算法."--陆妙善
        PeptideFeature peptideFeature = featureExtractor.getExperimentFeature(dataDO, peptide.buildIntensityMap(), input.getSigmaSpacing());
        if (!peptideFeature.isFeatureFound()) {
            dataDO.setIdentifiedStatus(AnalyseDataDO.IDENTIFIED_STATUS_UNKNOWN);
            logger.info("肽段没有被选中的特征：PeptideRef: " + dataDO.getPeptideRef());
            return;
        }
        List<FeatureScores> featureScoresList = new ArrayList<>();
        List<PeakGroup> peakGroupFeatureList = peptideFeature.getPeakGroupList();
        HashMap<String, Double> normedLibIntMap = peptideFeature.getNormedLibIntMap();
        HashMap<String, Float> productMzMap = new HashMap<>();
        HashMap<String, Integer> productChargeMap = new HashMap<>();

        for (String cutInfo : dataDO.getMzMap().keySet()) {
            try {
                if (cutInfo.contains("^")) {
                    String temp = cutInfo;
                    if (cutInfo.contains("[")) {
                        temp = cutInfo.substring(0, cutInfo.indexOf("["));
                    }
                    if (temp.contains("i")) {
                        temp = temp.replace("i", "");
                    }
                    productChargeMap.put(cutInfo, Integer.parseInt(temp.split("\\^")[1]));
                } else {
                    productChargeMap.put(cutInfo, 1);
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.info("cutInfo:" + cutInfo + ";data:" + JSON.toJSONString(dataDO));
            }

            float mz = dataDO.getMzMap().get(cutInfo);
            productMzMap.put(cutInfo, mz);
        }

        HashMap<Integer, String> unimodHashMap = peptide.getUnimodMap();
        String sequence = peptide.getSequence();

        for (PeakGroup peakGroupFeature : peakGroupFeatureList) {
            FeatureScores featureScores = new FeatureScores(input.getScoreTypes().size());
            chromatographicScorer.calculateChromatographicScores(peakGroupFeature, normedLibIntMap, featureScores, input.getScoreTypes());
            if(!dataDO.getIsDecoy() && featureScores.get(ScoreType.XcorrShapeWeighted.getTypeName(), input.getScoreTypes()) != null
                    && featureScores.get(ScoreType.XcorrShapeWeighted.getTypeName(), input.getScoreTypes()) < input.getXcorrShapeWeightThreshold()
                    && featureScores.get(ScoreType.XcorrShape.getTypeName(), input.getScoreTypes()) < input.getXcorrShapeThreshold()){
                continue;
            }
            //根据RT时间和前体MZ获取最近的一个原始谱图
            if (input.isUsedDIAScores()) {
                MzIntensityPairs mzIntensityPairs = swathIndexService.getNearestSpectrumByRt(rtMap, peakGroupFeature.getApexRt());
                if (mzIntensityPairs != null) {
                    Float[] spectrumMzArray = mzIntensityPairs.getMzArray();
                    Float[] spectrumIntArray = mzIntensityPairs.getIntensityArray();
                    if (input.getScoreTypes().contains(ScoreType.IsotopeCorrelationScore.getTypeName()) || input.getScoreTypes().contains(ScoreType.IsotopeOverlapScore.getTypeName())) {
                        diaScorer.calculateDiaIsotopeScores(peakGroupFeature, productMzMap, spectrumMzArray, spectrumIntArray, productChargeMap, featureScores, input.getScoreTypes());
                    }
                    if (input.getScoreTypes().contains(ScoreType.BseriesScore.getTypeName()) || input.getScoreTypes().contains(ScoreType.YseriesScore.getTypeName())) {
                        diaScorer.calculateBYIonScore(spectrumMzArray, spectrumIntArray, unimodHashMap, sequence, 1, featureScores, input.getScoreTypes());
                    }
                    diaScorer.calculateDiaMassDiffScore(productMzMap, spectrumMzArray, spectrumIntArray, normedLibIntMap, featureScores, input.getScoreTypes());

                }
            }
            if (input.getScoreTypes().contains(ScoreType.LogSnScore.getTypeName())) {
                chromatographicScorer.calculateLogSnScore(peakGroupFeature, featureScores, input.getScoreTypes());
            }

//            if (input.getScoreTypes().contains(ScoreType.ElutionModelFitScore.getTypeName())) {
//                elutionScorer.calculateElutionModelScore(peakGroupFeature, featureScores, input.getScoreTypes());
//            }
            if (input.getScoreTypes().contains(ScoreType.IntensityScore.getTypeName())) {
                libraryScorer.calculateIntensityScore(peakGroupFeature, featureScores, input.getScoreTypes());
            }

            libraryScorer.calculateLibraryScores(peakGroupFeature, normedLibIntMap, featureScores, input.getScoreTypes());
//            if (dataDO.getIsDecoy() && featureScores.get(ScoreType.NewScore)>0.9){
//                System.out.println(dataDO.getPeptideRef());
//            }
            if (input.getScoreTypes().contains(ScoreType.NormRtScore.getTypeName())) {
                libraryScorer.calculateNormRtScore(peakGroupFeature, input.getSlopeIntercept(), dataDO.getRt(), featureScores, input.getScoreTypes());
            }
            swathLDAScorer.calculateSwathLdaPrescore(featureScores, input.getScoreTypes());
            featureScores.setRt(peakGroupFeature.getApexRt());
            featureScores.setRtRangeFeature(FeatureUtil.toString(peakGroupFeature.getBestLeftRt(), peakGroupFeature.getBestRightRt()));
            featureScores.setIntensitySum(peakGroupFeature.getPeakGroupInt());
            featureScores.setFragIntFeature(FeatureUtil.toString(peakGroupFeature.getIonIntensity()));
            featureScoresList.add(featureScores);
        }
//        if (dataDO.getIsDecoy()){//min 1
//            featureScoresList = getFilteredScore(featureScoresList, 5, ScoreType.XcorrShapeWeighted.getTypeName());
//        }

        if (featureScoresList.size() == 0) {
            dataDO.setIdentifiedStatus(AnalyseDataDO.IDENTIFIED_STATUS_NO_FIT);
//            if (!dataDO.getIsDecoy()){
//                logger.info("肽段没有被选中的Peak, PeptideRef:" + dataDO.getPeptideRef());
//            }
            return;
        }

        dataDO.setFeatureScoresList(featureScoresList);
    }

    /**
     * get rt pairs for every peptideRef
     *
     * @param scoresList peptideRef list of List<ScoreRtPair>
     * @param rt         get from groupsResult.getModel()
     * @return rt pairs
     */
    private List<Pair<Double,Double>> simpleFindBestFeature(List<List<ScoreRtPair>> scoresList, List<Double> rt) {

        List<Pair<Double,Double>> pairs = new ArrayList<>();

        for (int i = 0; i < scoresList.size(); i++) {
            List<ScoreRtPair> scores = scoresList.get(i);
            double max = Double.MIN_VALUE;
            //find max scoreForAll's rt
            double expRt = 0d;
            for (int j = 0; j < scores.size(); j++) {
                if (scores.get(j).getScore() > max) {
                    max = scores.get(j).getScore();
                    expRt = scores.get(j).getRt();
                }
            }
            if (Constants.ESTIMATE_BEST_PEPTIDES && max < Constants.OVERALL_QUALITY_CUTOFF) {
                continue;
            }
            Pair<Double,Double> rtPair = Pair.of(rt.get(i), expRt);
            pairs.add(rtPair);
        }
        return pairs;
    }

    /**
     * 先进行线性拟合，每次从pairs中选取一个residual最大的点丢弃，获得pairsCorrected
     *
     * @param pairs       RTPairs left:TheoryRt right:ExpRt
     * @param minRsq      goal of iteration
     * @param minCoverage limit of picking
     * @return pairsCorrected
     */
    private List<Pair<Double,Double>> removeOutlierIterative(List<Pair<Double,Double>> pairs, double minRsq, double minCoverage, double delta) {

        int pairsSize = pairs.size();
        if (pairsSize < 3) {
            return null;
        }

        //获取斜率和截距
        double rsq = 0;
        double[] coEff = new double[2];

        WeightedObservedPoints obs = new WeightedObservedPoints();
        while (pairs.size() >= pairsSize * minCoverage && rsq < minRsq) {
            obs.clear();
            for (Pair<Double,Double> rtPair : pairs) {
                obs.add(rtPair.getRight(), rtPair.getLeft());
            }
            PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
            coEff = fitter.fit(obs.toList());
//            SlopeIntercept slopeIntercept = linearFitter.huberFit(pairs, delta);
//            coEff[1] = slopeIntercept.getSlope();
//            coEff[0] = slopeIntercept.getIntercept();

            rsq = MathUtil.getRsq(pairs);
            if (rsq < minRsq) {
                // calculate residual and get max index
                double res, max = 0;
                int maxIndex = 0;
                for (int i = 0; i < pairs.size(); i++) {
                    res = (Math.abs(pairs.get(i).getLeft() - (coEff[0] + coEff[1] * pairs.get(i).getRight())));
                    if (res > max) {
                        max = res;
                        maxIndex = i;
                    }
                }
                //remove outlier of pairs iteratively
                pairs.remove(maxIndex);
            }
        }
        if (rsq < minRsq) {
            System.out.println("RTNormalizer: unable to perform outlier detection.");
            return null;
        } else {
            return pairs;
        }
    }

    /**
     * 判断是否对RT空间实现了正常密度的覆盖
     *
     * @param rtRange           getRTRange
     * @param pairsCorrected    remove outlier之后的pairs
     * @param rtBins            需要分成的bin的数量
     * @param minPeptidesPerBin 每个bin最小分到的数量
     * @param minBinsFilled     需要满足↑条件的bin的数量
     * @return boolean 是否覆盖
     */
    private boolean computeBinnedCoverage(double[] rtRange, List<Pair<Double,Double>> pairsCorrected, int rtBins, int minPeptidesPerBin, int minBinsFilled) {
        int[] binCounter = new int[rtBins];
        double rtDistance = rtRange[1] - rtRange[0];

        //获得theorRt部分的分布
        for (Pair<Double,Double> pair : pairsCorrected) {
            double percent = (pair.getLeft() - rtRange[0]) / rtDistance;
            int bin = (int) (percent * rtBins);
            if (bin >= rtBins) {
                bin = rtBins - 1;
            }
            binCounter[bin]++;
        }

        //判断分布是否覆盖
        int binFilled = 0;
        for (int binCount : binCounter) {
            if(binCount >= minPeptidesPerBin) {
                binFilled++;
            }
        }
        return binFilled >= minBinsFilled;
    }



    private List<Pair<Double,Double>> chooseReliablePairs(List<Pair<Double,Double>> rtPairs, double delta) throws Exception {
        SlopeIntercept slopeIntercept = linearFitter.huberFit(rtPairs, delta);
        TreeMap<Double, Pair<Double,Double>> errorMap = new TreeMap<>();
        for (Pair<Double, Double> pair: rtPairs){
            errorMap.put(Math.abs(pair.getRight() * slopeIntercept.getSlope() + slopeIntercept.getIntercept() - pair.getLeft()), pair);
        }
        List<Pair<Double,Double>> sortedPairs = new ArrayList<>(errorMap.values());
        int cutLine = 2;
        for (int i = sortedPairs.size(); i > 2; i--){
            if (MathUtil.getRsq(sortedPairs.subList(0,i)) >= 0.95){
                cutLine = i;
                break;
            }
        }
        return sortedPairs.subList(0,cutLine);
    }

    private List<FeatureScores> getFilteredScore(List<FeatureScores> featureScoresList, int topN, String scoreName, List<String> scoreTypes){
        if (featureScoresList.size() < topN){
            return featureScoresList;
        }
        List<FeatureScores> filteredScoreList = SortUtil.sortBySelectedScore(featureScoresList, scoreName, true, scoreTypes);
        return filteredScoreList.subList(0, topN);
    }
}