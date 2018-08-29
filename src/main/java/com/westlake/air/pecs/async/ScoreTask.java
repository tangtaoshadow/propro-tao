package com.westlake.air.pecs.async;

import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairs;
import com.westlake.air.pecs.domain.bean.score.*;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.AnalyseOverviewDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.domain.db.simple.IntensityGroup;
import com.westlake.air.pecs.domain.db.simple.TransitionGroup;
import com.westlake.air.pecs.feature.FeatureExtractor;
import com.westlake.air.pecs.rtnormalizer.ChromatogramFilter;
import com.westlake.air.pecs.scorer.*;
import com.westlake.air.pecs.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-20 15:06
 */
@Component("scoreTask")
public class ScoreTask {

    public final Logger logger = LoggerFactory.getLogger(ScoreTask.class);

    @Autowired
    TaskService taskService;
    @Autowired
    ScoreService scoreService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    FeatureExtractor featureExtractor;
    @Autowired
    TransitionService transitionService;
    @Autowired
    ChromatogramFilter chromatogramFilter;
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

    @Async
    public void score(String overviewId, Float sigma, Float spacing, TaskDO taskDO) {

        ResultDO<AnalyseOverviewDO> resultDO = analyseOverviewService.getById(overviewId);
        if (resultDO.isFailed()) {
            taskDO.addLog(ResultCode.ANALYSE_OVERVIEW_NOT_EXISTED.getMessage());
            taskDO.finish(TaskDO.STATUS_FAILED);
            taskService.update(taskDO);
            return;
        }

        AnalyseOverviewDO overviewDO = resultDO.getModel();
        ResultDO<SlopeIntercept> resultDOIRT = scoreService.computeIRt(overviewId, overviewDO.getIRtLibraryId(), sigma, spacing, taskDO);
        if (resultDO.isFailed()) {
            taskDO.addLog("打分执行失败:" + resultDO.getMsgInfo());
            taskDO.finish(TaskDO.STATUS_FAILED);
            taskService.update(taskDO);
            return;
        }

        taskDO.addLog("IRT计算完毕," + resultDO.getModel().toString());
        taskService.update(taskDO);

        ResultDO<List<TransitionGroup>> dataListResult = analyseDataService.getTransitionGroup(overviewDO, null);
        if(dataListResult.isFailed()){
            taskDO.addLog("获取TransitionGroup失败:" + dataListResult.getMsgInfo());
            taskDO.finish(TaskDO.STATUS_FAILED);
            taskService.update(taskDO);
            return;
        }

        List<IntensityGroup> intensityGroupList = transitionService.getIntensityGroup(overviewDO.getLibraryId());
        List<PecsScore> pecsScoreList = new ArrayList<>();
        for (TransitionGroup group : dataListResult.getModel()) {
            List<FeatureScores> featureScoresList = new ArrayList<>();
            FeatureByPep featureByPep = featureExtractor.getExperimentFeature(group, intensityGroupList, resultDOIRT.getModel(), sigma, spacing);
            if(!featureByPep.isFeatureFound()){
                continue;
            }
            List<List<ExperimentFeature>> experimentFeatures = featureByPep.getExperimentFeatures();
            List<RtIntensityPairs> chromatogramList = featureByPep.getRtIntensityPairsOriginList();
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
            HashMap<Integer, String> unimodHashMap = new HashMap<>();
            String sequence = "";
            //for each mrmFeature, calculate scores
            for(List<ExperimentFeature> experimentFeatureList : experimentFeatures) {
                FeatureScores featureScores = new FeatureScores();
                chromatographicScorer.calculateChromatographicScores(chromatogramList, experimentFeatureList, libraryIntensityList, noise1000List, featureScores);
                chromatographicScorer.calculateIntensityScore(experimentFeatureList, featureScores);
                diaScorer.calculateDiaMassDiffScore(productMzList, spectrumMzArray, spectrumIntArray, libraryIntensityList, featureScores);
                diaScorer.calculateDiaIsotopeScores(experimentFeatureList, productMzList, spectrumMzArray, spectrumIntArray, productChargeList, featureScores);
////                //TODO @Nico charge from transition?
                diaScorer.calculateBYIonScore(spectrumMzArray, spectrumIntArray, unimodHashMap, sequence, 1, featureScores);
                elutionScorer.calculateElutionModelScore(experimentFeatureList, featureScores);
                libraryScorer.calculateIntensityScore(experimentFeatureList, featureScores);
                libraryScorer.calculateLibraryScores(experimentFeatureList, libraryIntensityList, resultDOIRT.getModel(), group.getRt().floatValue(), featureScores);
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
}
