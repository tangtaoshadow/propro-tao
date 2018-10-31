package com.westlake.air.pecs.service.impl;

import com.alibaba.fastjson.JSON;
import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.dao.ConfigDAO;
import com.westlake.air.pecs.dao.ScoresDAO;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.SwathInput;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.pecs.domain.bean.score.*;
import com.westlake.air.pecs.domain.db.*;
import com.westlake.air.pecs.domain.db.simple.IntensityGroup;
import com.westlake.air.pecs.domain.db.simple.TransitionGroup;
import com.westlake.air.pecs.domain.query.ScoresQuery;
import com.westlake.air.pecs.feature.*;
import com.westlake.air.pecs.rtnormalizer.ChromatogramFilter;
import com.westlake.air.pecs.rtnormalizer.RTNormalizerScorer;
import com.westlake.air.pecs.scorer.*;
import com.westlake.air.pecs.service.*;
import com.westlake.air.pecs.utils.FileUtil;
import com.westlake.air.pecs.utils.MathUtil;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-15 10:05
 */
@Service("scoresService")
public class ScoresServiceImpl implements ScoresService {

    public final Logger logger = LoggerFactory.getLogger(ScoresServiceImpl.class);

    @Autowired
    ScoresDAO scoresDAO;
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

    @Override
    public Long count(ScoresQuery query) {
        return scoresDAO.count(query);
    }

    @Override
    public ResultDO<List<ScoresDO>> getList(ScoresQuery targetQuery) {
        List<ScoresDO> scoresList = scoresDAO.getList(targetQuery);
        long totalCount = scoresDAO.count(targetQuery);
        ResultDO<List<ScoresDO>> resultDO = new ResultDO<>(true);
        resultDO.setModel(scoresList);
        resultDO.setTotalNum(totalCount);
        resultDO.setPageSize(targetQuery.getPageSize());

        return resultDO;
    }

    @Override
    public List<ScoresDO> getAllByOverviewId(String overviewId) {
        return scoresDAO.getAllByOverviewId(overviewId);
    }

    @Override
    public HashMap<String, ScoresDO> getAllMapByOverviewId(String overviewId) {
        List<ScoresDO> scoresList = scoresDAO.getAllByOverviewId(overviewId);
        HashMap<String, ScoresDO> map = new HashMap<>();
        for (ScoresDO scoresDO : scoresList) {
            String key = scoresDO.getIsDecoy() + "_" + scoresDO.getPeptideRef();
            map.put(key, scoresDO);
        }
        return map;
    }

    @Override
    public ResultDO insert(ScoresDO scoresDO) {
        try {
            scoresDO.setCreateDate(new Date());
            scoresDAO.insert(scoresDO);
            return ResultDO.build(scoresDO);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return ResultDO.buildError(ResultCode.INSERT_ERROR);
        }
    }

    @Override
    public ResultDO update(ScoresDO scoresDO) {
        if (scoresDO.getId() == null || scoresDO.getId().isEmpty()) {
            return ResultDO.buildError(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }

        try {
            scoresDO.setLastModifiedDate(new Date());
            scoresDAO.update(scoresDO);
            return ResultDO.build(scoresDO);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return ResultDO.buildError(ResultCode.UPDATE_ERROR);
        }
    }

    @Override
    public ResultDO delete(String id) {
        if (id == null || id.isEmpty()) {
            return ResultDO.buildError(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        try {
            scoresDAO.delete(id);
            return new ResultDO(true);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return ResultDO.buildError(ResultCode.DELETE_ERROR);
        }
    }

    @Override
    public ResultDO deleteAllByOverviewId(String overviewId) {
        if (overviewId == null || overviewId.isEmpty()) {
            return ResultDO.buildError(ResultCode.ANALYSE_OVERVIEW_ID_CAN_NOT_BE_EMPTY);
        }
        try {
            scoresDAO.deleteAllByOverviewId(overviewId);
            return new ResultDO(true);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return ResultDO.buildError(ResultCode.DELETE_ERROR);
        }
    }

    @Override
    public ResultDO<ScoresDO> getById(String id) {
        try {
            ScoresDO scoresDO = scoresDAO.getById(id);
            if (scoresDO == null) {
                return ResultDO.buildError(ResultCode.OBJECT_NOT_EXISTED);
            } else {
                return ResultDO.build(scoresDO);
            }
        } catch (Exception e) {
            return ResultDO.buildError(ResultCode.QUERY_ERROR);
        }
    }

    @Override
    public ResultDO<ScoresDO> getByPeptideRef(String peptideRef) {
        try {
            ScoresDO scoresDO = scoresDAO.getByPeptideRef(peptideRef);
            if (scoresDO == null) {
                return ResultDO.buildError(ResultCode.OBJECT_NOT_EXISTED);
            } else {
                return ResultDO.build(scoresDO);
            }
        } catch (Exception e) {
            return ResultDO.buildError(ResultCode.QUERY_ERROR);
        }
    }

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
        for (TransitionGroup group : groups) {
            FeatureByPep featureByPep = featureExtractor.getExperimentFeature(group, intensityGroupMap.get(group.getPeptideRef()), sigmaSpacing);
            if (!featureByPep.isFeatureFound()) {
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
        if (pairsCorrected == null || pairsCorrected.size() < 2) {
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
        for (TransitionGroup group : groups) {
            SlopeIntercept slopeIntercept = new SlopeIntercept();//void parameter
            FeatureByPep featureByPep = featureExtractor.getExperimentFeature(group, intensityGroupMap.get(group.getPeptideRef() + "_" + group.getIsDecoy()), sigmaSpacing);
            if (!featureByPep.isFeatureFound()) {
                continue;
            }
            double groupRt = group.getRt();
            List<ScoreRtPair> scoreRtPairs = RTNormalizerScorer.score(featureByPep.getRtIntensityPairsOriginList(), featureByPep.getExperimentFeatures(), featureByPep.getLibraryIntensityList(), featureByPep.getNoise1000List(), slopeIntercept, groupRt);
            scoreRtList.add(scoreRtPairs);
            compoundRt.add(groupRt);
        }

        List<RtPair> pairs = simpleFindBestFeature(scoreRtList, compoundRt);
        List<RtPair> pairsCorrected = removeOutlierIterative(pairs, Constants.MIN_RSQ, Constants.MIN_COVERAGE);

        if (pairsCorrected == null || pairsCorrected.size() < 2) {
            logger.error(ResultCode.NOT_ENOUGH_IRT_PEPTIDES.getMessage());
            resultDO.setErrorResult(ResultCode.NOT_ENOUGH_IRT_PEPTIDES);
            return resultDO;
        }

        SlopeIntercept slopeIntercept = fitRTPairs(pairsCorrected);
        resultDO.setSuccess(true);
        resultDO.setModel(slopeIntercept);

        return resultDO;
    }

    @Override
    public List<ScoresDO> score(List<AnalyseDataDO> dataList, SwathInput input) {

        if (dataList == null || dataList.size() == 0) {
            return null;
        }
        input.setOverviewId(dataList.get(0).getOverviewId());//取一个AnalyseDataDO的OverviewId

        //开始打分前先删除原有的打分数据
        scoresDAO.deleteAllByOverviewId(input.getOverviewId());
        logger.info("原有打分数据删除完毕");
        List<TransitionGroup> groups = analyseDataService.getTransitionGroup(dataList);

        HashMap<String, IntensityGroup> intensityGroupMap = transitionService.getIntensityGroupMap(input.getLibraryId());
        List<ScoresDO> pecsScoreList = new ArrayList<>();

        int count = 0;
        for (TransitionGroup group : groups) {
            List<FeatureScores> featureScoresList = new ArrayList<>();
            FeatureByPep featureByPep = featureExtractor.getExperimentFeature(group, intensityGroupMap.get(group.getPeptideRef() + "_" + group.getIsDecoy()), input.getSigmaSpacing());

            if (!featureByPep.isFeatureFound()) {
                continue;
            }
            List<List<ExperimentFeature>> experimentFeatures = featureByPep.getExperimentFeatures();
            List<RtIntensityPairsDouble> chromatogramList = featureByPep.getRtIntensityPairsOriginList();
            List<Double> libraryIntensityList = featureByPep.getLibraryIntensityList();
            List<double[]> noise1000List = featureByPep.getNoise1000List();
            List<Double> productMzList = new ArrayList<>();
            for (AnalyseDataDO dataDO : group.getDataMap().values()) {// TODO @Nico 精度必要性
                productMzList.add(Double.parseDouble(Float.toString(dataDO.getMz())));
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
            for (AnalyseDataDO data : group.getDataMap().values()) {
                String cutInfo = data.getCutInfo();
                try {
                    if (cutInfo.contains("^")) {
                        productChargeList.add(Integer.parseInt(cutInfo.split("\\^")[1]));
                    } else {
                        productChargeList.add(1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.info("cutInfo:" + cutInfo + ";data:" + JSON.toJSONString(data));
                }

            }

            HashMap<Integer, String> unimodHashMap = group.getUnimodMap();
            String sequence = group.getPeptideRef().split("_")[0];
            //for each mrmFeature, calculate scores

            for (List<ExperimentFeature> experimentFeatureList : experimentFeatures) {

                FeatureScores featureScores = new FeatureScores();
                chromatographicScorer.calculateChromatographicScores(experimentFeatureList, libraryIntensityList, featureScores);
                chromatographicScorer.calculateLogSnScore(chromatogramList, experimentFeatureList, noise1000List, featureScores);

//                diaScorer.calculateDiaMassDiffScore(productMzList, spectrumMzArray, spectrumIntArray, libraryIntensityList, featureScores);
//                diaScorer.calculateDiaIsotopeScores(experimentFeatureList, productMzList, spectrumMzArray, spectrumIntArray, productChargeList, featureScores);

                //TODO @Nico charge from transition?
//                diaScorer.calculateBYIonScore(spectrumMzArray, spectrumIntArray, unimodHashMap, sequence, 1, featureScores);
//                elutionScorer.calculateElutionModelScore(experimentFeatureList, featureScores);
                libraryScorer.calculateIntensityScore(experimentFeatureList, featureScores);
                libraryScorer.calculateLibraryScores(experimentFeatureList, libraryIntensityList, featureScores);
                libraryScorer.calculateNormRtScore(experimentFeatureList, input.getSlopeIntercept(), group.getRt(), featureScores);
                swathLDAScorer.calculateSwathLdaPrescore(featureScores);
                featureScores.setRt(experimentFeatureList.get(0).getRt());
                featureScoresList.add(featureScores);
            }

            ScoresDO pecsScore = new ScoresDO();
            pecsScore.setOverviewId(input.getOverviewId());
            pecsScore.setPeptideRef(group.getPeptideRef());
            pecsScore.setIsDecoy(group.getIsDecoy());
            pecsScore.setFeatureScoresList(featureScoresList);
            pecsScore.setCreateDate(new Date());
            pecsScore.setLastModifiedDate(new Date());
            pecsScoreList.add(pecsScore);

            count++;
            if (count % 100 == 0) {
                logger.info(count + "个Group已经打分完毕,总共有" + groups.size() + "个Group");
            }
        }
        scoresDAO.insert(pecsScoreList);
        logger.info("打分插入完毕");
        return pecsScoreList;
    }

    @Override
    public ResultDO exportForPyProphet(String overviewId) {

        ConfigDO configDO = configDAO.getConfig();
        String exportPath = configDO.getExportScoresFilePath();
        ResultDO<AnalyseOverviewDO> result = analyseOverviewService.getById(overviewId);
        if (result.isFailed()) {
            return ResultDO.buildError(ResultCode.SCORES_NOT_EXISTED);
        }

        AnalyseOverviewDO overviewDO = result.getModel();
        String outputFileName = exportPath + "/" + overviewDO.getExpName() + "-" + overviewDO.getLibraryName() + "-" + overviewId + ".tsv";

        //Generate the txt for pyprophet
        List<ScoresDO> scores = getAllByOverviewId(overviewId);
        String pyprophetColumns = "transition_group_id\trun_id\tdecoy\t" + FeatureScores.ScoreType.getPyProphetScoresColumns();
        StringBuilder sb = new StringBuilder(pyprophetColumns);
        List<FeatureScores.ScoreType> scoreTypes = FeatureScores.ScoreType.getUsedTypes();
        for (ScoresDO score : scores) {
            for (FeatureScores fs : score.getFeatureScoresList()) {
                sb.append((score.getIsDecoy() ? "DECOY_" : "") + score.getPeptideRef()).append(Constants.TAB);
                sb.append(0).append(Constants.TAB);
                sb.append(score.getIsDecoy() ? 1 : 0).append(Constants.TAB);
                for (int i = 0; i < scoreTypes.size(); i++) {
                    if (i == scoreTypes.size() - 1) {
                        sb.append(fs.get(scoreTypes.get(i))).append(Constants.CHANGE_LINE);
                    } else {
                        sb.append(fs.get(scoreTypes.get(i))).append(Constants.TAB);
                    }
                }
            }
        }

        try {
            FileUtil.writeFile(outputFileName, sb.toString(), true);
        } catch (IOException e) {
            return ResultDO.buildError(ResultCode.IO_EXCEPTION);
        }

        return new ResultDO(true);
    }

    @Override
    public ResultDO<List<ScoreDistribution>> buildScoreDistributions(String overviewId) {
        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);
        if (overviewResult.isFailed()) {
            return ResultDO.buildError(ResultCode.ANALYSE_OVERVIEW_NOT_EXISTED);
        }

        List<ScoresDO> scores = scoresDAO.getAllByOverviewId(overviewId);
        if (scores == null || scores.isEmpty()) {
            return ResultDO.buildError(ResultCode.SCORES_NOT_EXISTED);
        }
        List<FeatureScores.ScoreType> scoreTypes = FeatureScores.ScoreType.getUsedTypes();

        ResultDO<List<ScoreDistribution>> resultDO = new ResultDO<>(true);

        HashMap<String, List<Double>> resultMap = new HashMap<>();
        HashMap<String, List<Double>> resultDecoyMap = new HashMap<>();
        for (ScoresDO score : scores) {
            HashMap<String, Double> bestScoreMap = new HashMap<>();
            //计算最优分数
            for (FeatureScores fs : score.getFeatureScoresList()) {
                for (FeatureScores.ScoreType scoreType : scoreTypes) {
                    if (bestScoreMap.get(scoreType.getTypeName()) == null) {
                        bestScoreMap.put(scoreType.getTypeName(), fs.get(scoreType));
                        continue;
                    }

                    if (scoreType.getBiggerIsBetter() && fs.get(scoreType) > bestScoreMap.get(scoreType.getTypeName())) {
                        bestScoreMap.put(scoreType.getTypeName(), fs.get(scoreType));
                        continue;
                    }

                    if (!scoreType.getBiggerIsBetter() && fs.get(scoreType) < bestScoreMap.get(scoreType.getTypeName())) {
                        bestScoreMap.put(scoreType.getTypeName(), fs.get(scoreType));
                        continue;
                    }
                }
            }
            if (score.getIsDecoy()) {
                for (String key : bestScoreMap.keySet()) {
                    resultDecoyMap.computeIfAbsent(key, k -> new ArrayList<>());
                    resultDecoyMap.get(key).add(bestScoreMap.get(key));
                }
            } else {
                for (String key : bestScoreMap.keySet()) {
                    resultMap.computeIfAbsent(key, k -> new ArrayList<>());
                    resultMap.get(key).add(bestScoreMap.get(key));
                }
            }
        }

        List<ScoreDistribution> distributions = new ArrayList<>();

        buildScoreDisList(resultMap, resultDecoyMap, distributions);
        AnalyseOverviewDO overviewDO = overviewResult.getModel();
        overviewDO.setScoreDistributions(distributions);
        analyseOverviewService.update(overviewDO);

        resultDO.setModel(distributions);
        return resultDO;
    }

    /**
     * 根据分数的分布情况,对所有的分数进行分组,总共分为Constants.SCORE_RANGE组,如果所有分数都相同,那么不分组
     *
     * @param scoreMap
     * @return
     */
    private void buildScoreDisList(HashMap<String, List<Double>> scoreMap, HashMap<String, List<Double>> decoyScoreMap, List<ScoreDistribution> distributions) {

        for (String key : scoreMap.keySet()) {
            ScoreDistribution sd = new ScoreDistribution(key);
            List<Double> oneScores = scoreMap.get(key);
            List<Double> decoyOneScores = decoyScoreMap.get(key);
            Collections.sort(oneScores);
            Collections.sort(decoyOneScores);

            double min = Math.floor(oneScores.get(0) > decoyOneScores.get(0) ? decoyOneScores.get(0) : oneScores.get(0));
            double max = Math.ceil(oneScores.get(oneScores.size() - 1) > decoyOneScores.get(decoyOneScores.size() - 1) ? oneScores.get(oneScores.size() - 1) : decoyOneScores.get(decoyOneScores.size() - 1));
            double range = max - min;
            if (range == 0) {
                distributions.add(sd);
                continue;
            }
            double stepOri = range / Constants.SCORE_RANGE;

            NumberFormat nf = NumberFormat.getNumberInstance();
            nf.setMaximumFractionDigits(2);

            double step = Double.valueOf(nf.format(stepOri));

            String[] ranges = new String[Constants.SCORE_RANGE];
            Integer[] targetCount = new Integer[Constants.SCORE_RANGE];
            Integer[] decoyCount = new Integer[Constants.SCORE_RANGE];
            for (int i = 0; i < Constants.SCORE_RANGE; i++) {
                targetCount[i] = 0;
                decoyCount[i] = 0;
                if (i != (Constants.SCORE_RANGE - 1)) {
                    ranges[i] = nf.format(min + step * (i - 1)) + "~" + nf.format(min + step * i);
                } else {
                    ranges[i] = nf.format(min + step * (i - 1)) + "~" + nf.format(max);
                }
            }
            for (Double d : oneScores) {
                int count = (int) Math.ceil((d - min) / step);
                //如果count为0,则直接调整到第一个区间范围内
                if(count == 0){
                    count = 1;
                }
                targetCount[count-1] = targetCount[count-1] + 1;
            }

            for (Double d : decoyOneScores) {
                int count = (int) Math.ceil((d - min) / step);
                //如果count为0,则直接调整到第一个区间范围内
                if(count == 0){
                    count = 1;
                }
                decoyCount[count-1] = decoyCount[count-1] + 1;
            }
            sd.buildData(ranges, targetCount, decoyCount);
            distributions.add(sd);
        }

    }

    /**
     * get rt pairs for every peptideRef
     *
     * @param scoresList peptideRef list of List<ScoreRtPair>
     * @param rt         get from groupsResult.getModel()
     * @return rt pairs
     */
    private List<RtPair> simpleFindBestFeature(List<List<ScoreRtPair>> scoresList, List<Double> rt) {

        List<RtPair> pairs = new ArrayList<>();

        for (int i = 0; i < scoresList.size(); i++) {
            List<ScoreRtPair> scores = scoresList.get(i);
            double max = Double.MIN_VALUE;
            RtPair rtPair = new RtPair();
            //find max score's rt
            for (int j = 0; j < scores.size(); j++) {
                if (scores.get(j).getScore() > max) {
                    max = scores.get(j).getScore();
                    rtPair.setExpRt(scores.get(j).getRt());
                }
            }
            if (Constants.ESTIMATE_BEST_PEPTIDES && max < Constants.OVERALL_QUALITY_CUTOFF) {
                continue;
            }
            rtPair.setTheoRt(rt.get(i));
            pairs.add(rtPair);
        }
        return pairs;
    }

    /**
     * 先进行线性拟合，每次从pairs中选取一个residual最大的点丢弃，获得pairsCorrected
     *
     * @param pairs       RTPairs
     * @param minRsq      goal of iteration
     * @param minCoverage limit of picking
     * @return pairsCorrected
     */
    private List<RtPair> removeOutlierIterative(List<RtPair> pairs, double minRsq, double minCoverage) {

        int pairsSize = pairs.size();
        if (pairsSize < 3) {
            return null;
        }

        //获取斜率和截距
        double rsq = 0;
        double[] coEff;

        WeightedObservedPoints obs = new WeightedObservedPoints();
        while (pairs.size() >= pairsSize * minCoverage && rsq < minRsq) {
            obs.clear();
            for (RtPair rtPair : pairs) {
                obs.add(rtPair.getExpRt(), rtPair.getTheoRt());
            }
            PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
            coEff = fitter.fit(obs.toList());

            rsq = MathUtil.getRsq(pairs);
            if (rsq < minRsq) {
                // calculate residual and get max index
                double res, max = 0;
                int maxIndex = 0;
                for (int i = 0; i < pairs.size(); i++) {
                    res = (Math.abs(pairs.get(i).getTheoRt() - (coEff[0] + coEff[1] * pairs.get(i).getExpRt())));
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
    private boolean computeBinnedCoverage(double[] rtRange, List<RtPair> pairsCorrected, int rtBins, int minPeptidesPerBin, int minBinsFilled) {
        int[] binCounter = new int[rtBins];
        double rtDistance = rtRange[1] - rtRange[0];

        //获得theorRt部分的分布
        for (RtPair pair : pairsCorrected) {
            double percent = (pair.getTheoRt() - rtRange[0]) / rtDistance;
            int bin = (int) (percent * rtBins);
            if (bin >= rtBins) {
                bin = rtBins - 1;
            }
            binCounter[bin]++;
        }

        //判断分布是否覆盖
        int binFilled = 0;
        for (int binCount : binCounter) {
            if (binCount >= minPeptidesPerBin) binFilled++;
        }
        return binFilled >= minBinsFilled;
    }

    /**
     * 最小二乘法线性拟合RTPairs
     *
     * @param rtPairs <exp_rt, theor_rt>
     * @return 斜率和截距
     */
    private SlopeIntercept fitRTPairs(List<RtPair> rtPairs) {
        WeightedObservedPoints obs = new WeightedObservedPoints();
        for (RtPair rtPair : rtPairs) {
            obs.add(rtPair.getExpRt(), rtPair.getTheoRt());
        }
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
        double[] coeff = fitter.fit(obs.toList());
        SlopeIntercept slopeIntercept = new SlopeIntercept();
        slopeIntercept.setSlope((float) coeff[1]);
        slopeIntercept.setIntercept((float) coeff[0]);
        return slopeIntercept;
    }
}
