package com.westlake.air.pecs.async;

import com.westlake.air.pecs.algorithm.Airus;
import com.westlake.air.pecs.constants.TaskStatus;
import com.westlake.air.pecs.domain.bean.airus.AirusParams;
import com.westlake.air.pecs.domain.bean.airus.FinalResult;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.service.TaskService;
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
        FinalResult result = airus.doAirus(overviewId, airusParams);
        taskDO.addLog("合并打分完毕,耗时:" + (System.currentTimeMillis() - start) + ",最终识别的肽段数为" + result.getMatchPeptideCount());
        taskDO.finish(TaskStatus.SUCCESS.getName());
        taskService.update(taskDO);
    }
}
