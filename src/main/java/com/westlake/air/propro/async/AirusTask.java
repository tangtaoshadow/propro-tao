package com.westlake.air.propro.async;

import com.westlake.air.propro.algorithm.learner.Airus;
import com.westlake.air.propro.constants.TaskStatus;
import com.westlake.air.propro.domain.bean.airus.AirusParams;
import com.westlake.air.propro.domain.bean.airus.FinalResult;
import com.westlake.air.propro.domain.db.TaskDO;
import com.westlake.air.propro.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component("airusTask")
public class AirusTask {

    @Autowired
    Airus airus;
    @Autowired
    TaskService taskService;

    @Async(value = "airusExecutor")
    public void airus(String overviewId, AirusParams airusParams, TaskDO taskDO) {
        long start = System.currentTimeMillis();
        taskDO.start();
        taskDO.setStatus(TaskStatus.RUNNING.getName());
        taskService.update(taskDO);
        FinalResult result = airus.doAirus(overviewId, airusParams);
        if (result.getErrorInfo() != null) {
            taskDO.addLog("合并打分完毕出错:" + result.getErrorInfo());
            taskDO.finish(TaskStatus.FAILED.getName());
            taskService.update(taskDO);
        } else {
            taskDO.addLog("合并打分完毕,耗时:" + (System.currentTimeMillis() - start) + ",最终识别的肽段数为" + result.getMatchedPeptideCount());
            taskDO.finish(TaskStatus.SUCCESS.getName());
            taskService.update(taskDO);
        }
    }
}
