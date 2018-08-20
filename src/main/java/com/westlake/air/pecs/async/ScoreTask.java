package com.westlake.air.pecs.async;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.rtnormalizer.ChromatogramFilter;
import com.westlake.air.pecs.service.ScoreService;
import com.westlake.air.pecs.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

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
    ChromatogramFilter chromatogramFilter;

    @Async
    public void score(String overviewId, Float sigma, Float spacing, TaskDO taskDO) {
        ResultDO<SlopeIntercept> resultDO = scoreService.computeIRt(overviewId, sigma, spacing, taskDO);
        if (resultDO.isFailed()) {
            taskDO.addLog("打分执行失败:" + resultDO.getMsgInfo());
            taskDO.finish(TaskDO.STATUS_FAILED);
            taskService.update(taskDO);
            return;
        }

        taskDO.addLog("IRT计算完毕," + resultDO.getModel().toString());
        taskService.update(taskDO);


    }
}
