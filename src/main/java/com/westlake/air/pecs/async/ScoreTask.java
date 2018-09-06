package com.westlake.air.pecs.async;

import com.westlake.air.pecs.domain.bean.SwathInput;
import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.service.AnalyseDataService;
import com.westlake.air.pecs.service.ScoreService;
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
    ScoreService scoreService;
    @Autowired
    AnalyseDataService analyseDataService;

    @Async
    public void score(String overviewId, SlopeIntercept slopeIntercept, String libraryId, SigmaSpacing sigmaSpacing, TaskDO taskDO) {
        long start = System.currentTimeMillis();
        taskDO.addLog("开始查询所有卷积结果");
        taskService.update(taskDO);
        List<AnalyseDataDO> dataList = analyseDataService.getAllByOverviewId(overviewId);

        taskDO.addLog("卷积结果获取完毕,耗时:"+(System.currentTimeMillis() - start)+".开始进行打分");
        taskService.update(taskDO);

        start = System.currentTimeMillis();
        SwathInput input = new SwathInput();
        input.setLibraryId(libraryId);
        input.setSigmaSpacing(sigmaSpacing);
        input.setSlopeIntercept(slopeIntercept);
        scoreService.score(dataList, input);
        taskDO.addLog("打分完毕,耗时:" + (System.currentTimeMillis() - start));
        taskDO.finish(TaskDO.STATUS_SUCCESS);
        taskService.update(taskDO);
    }
}
