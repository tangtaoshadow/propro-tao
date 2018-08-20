package com.westlake.air.pecs.async;

import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.score.FeatureByPep;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.AnalyseOverviewDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.domain.db.simple.IntensityGroup;
import com.westlake.air.pecs.domain.db.simple.TransitionGroup;
import com.westlake.air.pecs.domain.query.PageQuery;
import com.westlake.air.pecs.feature.FeatureExtractor;
import com.westlake.air.pecs.rtnormalizer.ChromatogramFilter;
import com.westlake.air.pecs.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

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

    @Async
    public void score(String overviewId, Float sigma, Float spacing, TaskDO taskDO) {

        ResultDO<AnalyseOverviewDO> resultDO = analyseOverviewService.getById(overviewId);
        if (resultDO.isFailed()) {
            taskDO.addLog(ResultCode.ANALYSE_OVERVIEW_NOT_EXISTED.getMessage());
            taskDO.finish(TaskDO.STATUS_FAILED);
            taskService.update(taskDO);
            return;
        }

        AnalyseOverviewDO analyseOverviewDO = resultDO.getModel();
        ResultDO<SlopeIntercept> resultDOIRT = scoreService.computeIRt(overviewId, analyseOverviewDO.getIRtLibraryId(), sigma, spacing, taskDO);
        if (resultDO.isFailed()) {
            taskDO.addLog("打分执行失败:" + resultDO.getMsgInfo());
            taskDO.finish(TaskDO.STATUS_FAILED);
            taskService.update(taskDO);
            return;
        }

        taskDO.addLog("IRT计算完毕," + resultDO.getModel().toString());
        taskService.update(taskDO);

        ResultDO<List<TransitionGroup>> dataListResult = analyseDataService.getTransitionGroup(overviewId, analyseOverviewDO.getLibraryId(), null);
        if(dataListResult.isFailed()){
            taskDO.addLog("获取TransitionGroup失败:" + dataListResult.getMsgInfo());
            taskDO.finish(TaskDO.STATUS_FAILED);
            taskService.update(taskDO);
            return;
        }

        for (TransitionGroup group : dataListResult.getModel()) {
            List<IntensityGroup> intensityGroupList = transitionService.getIntensityGroup(analyseOverviewDO.getLibraryId());
            FeatureByPep featureByPep = featureExtractor.getExperimentFeature(group, intensityGroupList, resultDOIRT.getModel(), sigma, spacing);
            if(featureByPep.isFeatureFound()){

            }
        }
    }
}
