package com.westlake.air.pecs.async;

import com.westlake.air.pecs.constants.TaskStatus;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.SwathInput;
import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.AnalyseOverviewDO;
import com.westlake.air.pecs.domain.db.ScoreDistribution;
import com.westlake.air.pecs.domain.db.TaskDO;
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

    @Async
    public void score(String overviewId, SlopeIntercept slopeIntercept, String libraryId, SigmaSpacing sigmaSpacing, TaskDO taskDO) {
        long start = System.currentTimeMillis();
        taskDO.addLog("开始查询所有卷积结果");
        taskService.update(taskDO);
        List<AnalyseDataDO> dataList = analyseDataService.getAllByOverviewId(overviewId);

        taskDO.addLog("卷积结果获取完毕,耗时:" + (System.currentTimeMillis() - start) + ".开始进行打分");
        taskService.update(taskDO);

        start = System.currentTimeMillis();
        SwathInput input = new SwathInput();
        input.setLibraryId(libraryId);
        input.setSigmaSpacing(sigmaSpacing);
        input.setSlopeIntercept(slopeIntercept);
        input.setOverviewId(overviewId);

        scoresService.score(dataList, input);

        taskDO.addLog("子分数打分成功,耗时:" + (System.currentTimeMillis() - start) + ".开始清理子分数总览图");
        taskService.update(taskDO);

        start = System.currentTimeMillis();
        scoresService.buildScoreDistributions(overviewId);
        taskDO.addLog("生成子分数总览图完毕,流程结束,耗时:" + (System.currentTimeMillis() - start));
        taskDO.finish(TaskStatus.SUCCESS.getName());
        taskService.update(taskDO);

    }

    @Async
    public void exportForPyProphet(String overviewId, TaskDO taskDO) {
        long start = System.currentTimeMillis();
        taskDO.addLog("开始进行子分数TSV文件导出");
        taskService.update(taskDO);
        ResultDO resultDO = scoresService.exportForPyProphet(overviewId);
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

    @Async
    public void buildScoreDistributions(String overviewId, TaskDO taskDO) {
        long start = System.currentTimeMillis();
        taskDO.addLog("开始构建子分数分布图");
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
