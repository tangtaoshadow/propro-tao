package com.westlake.air.pecs.async;

import com.westlake.air.pecs.constants.TaskStatus;
import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.domain.params.LumsParams;
import com.westlake.air.pecs.service.AnalyseDataService;
import com.westlake.air.pecs.service.AnalyseOverviewService;
import com.westlake.air.pecs.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashSet;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-20 15:06
 */
@Component("scoreTask")
public class ScoreTask extends BaseTask {

    @Autowired
    ScoreService scoreService;
    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;

    @Async(value = "scoreExecutor")
    public void score(String overviewId, ExperimentDO experimentDO, SlopeIntercept slopeIntercept, String libraryId, SigmaSpacing sigmaSpacing, HashSet<String> scoreTypes, TaskDO taskDO) {
        long start = System.currentTimeMillis();
        taskDO.setStatus(TaskStatus.RUNNING.getName());
        taskService.update(taskDO);

        LumsParams input = new LumsParams();
        input.setLibraryId(libraryId);
        input.setSigmaSpacing(sigmaSpacing);
        input.setSlopeIntercept(slopeIntercept);
        input.setOverviewId(overviewId);
        input.setExperimentDO(experimentDO);
        if(scoreTypes != null){
            input.setScoreTypes(scoreTypes);
        }
        input.setUsedDIAScores(true);

        taskDO.addLog("开始准备新打分");
        taskDO.setStatus(TaskStatus.RUNNING.getName());
        taskService.update(taskDO);

        score(overviewId, input, taskDO);

        taskDO.addLog("子分数打分成功,耗时:" + (System.currentTimeMillis() - start));
        taskDO.finish(TaskStatus.SUCCESS.getName());
        taskService.update(taskDO);
    }
}
