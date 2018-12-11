package com.westlake.air.pecs.async;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.constants.TaskStatus;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.SwathParams;
import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.*;
import com.westlake.air.pecs.service.AnalyseDataService;
import com.westlake.air.pecs.service.AnalyseOverviewService;
import com.westlake.air.pecs.service.ScoresService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-20 15:06
 */
@Component("scoreTask")
public class ScoreTask extends BaseTask {

    @Autowired
    ScoresService scoresService;
    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;

    @Async(value = "scoreExecutor")
    public void score(String overviewId, ExperimentDO experimentDO, SlopeIntercept slopeIntercept, String libraryId, SigmaSpacing sigmaSpacing, TaskDO taskDO) {
        long start = System.currentTimeMillis();
        SwathParams input = new SwathParams();
        input.setLibraryId(libraryId);
        input.setSigmaSpacing(sigmaSpacing);
        input.setSlopeIntercept(slopeIntercept);
        input.setOverviewId(overviewId);
        input.setExperimentDO(experimentDO);

        logger.info("首先删除所有旧打分数据");
        scoresService.deleteAllByOverviewId(input.getOverviewId());

        taskDO.addLog("删除旧打分结果完毕,开始准备新打分");
        taskDO.setStatus(TaskStatus.RUNNING.getName());
        taskService.update(taskDO);

        score(overviewId, input, taskDO);

        taskDO.addLog("子分数打分成功,耗时:" + (System.currentTimeMillis() - start));
//        taskService.update(taskDO);
//        start = System.currentTimeMillis();
//        scoresService.buildScoreDistributions(overviewId);
//        taskDO.addLog("生成子分数总览图完毕,流程结束,耗时:" + (System.currentTimeMillis() - start));
        taskDO.finish(TaskStatus.SUCCESS.getName());
        taskService.update(taskDO);

    }

    @Async(value = "commonExecutor")
    public void exportForPyProphet(String overviewId, TaskDO taskDO) {
        long start = System.currentTimeMillis();
        taskDO.addLog("开始进行子分数TSV文件导出");
        taskDO.setStatus(TaskStatus.RUNNING.getName());
        taskService.update(taskDO);
        ResultDO resultDO = scoresService.exportForPyProphet(overviewId, Constants.TAB);
        if (resultDO.isSuccess()) {
            taskDO.addLog("文件导出成功,耗时:" + (System.currentTimeMillis() - start));
            taskDO.finish(TaskStatus.SUCCESS.getName());
            taskService.update(taskDO);
        } else {
            taskDO.addLog("文件导出失败,耗时:" + (System.currentTimeMillis() - start));
            taskDO.addLog(resultDO.getMsgInfo());
            taskDO.finish(TaskStatus.FAILED.getName());
            taskService.update(taskDO);
        }
    }

    @Async(value = "commonExecutor")
    public void buildScoreDistributions(String overviewId, TaskDO taskDO) {
        long start = System.currentTimeMillis();
        taskDO.addLog("开始构建子分数分布图");
        taskDO.setStatus(TaskStatus.RUNNING.getName());
        taskService.update(taskDO);
        ResultDO<List<ScoreDistribution>> resultDO = scoresService.buildScoreDistributions(overviewId);
        if (resultDO.isSuccess()) {
            taskDO.addLog("子分数分布图构建成功,耗时:" + (System.currentTimeMillis() - start));
            taskDO.finish(TaskStatus.SUCCESS.getName());
            taskService.update(taskDO);
        } else {
            taskDO.addLog("子分数分布图构建失败,耗时:" + (System.currentTimeMillis() - start));
            taskDO.addLog(resultDO.getMsgInfo());
            taskDO.finish(TaskStatus.FAILED.getName());
            taskService.update(taskDO);
        }
    }
}
