package com.westlake.air.pecs.async;

import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-20 15:06
 */
@Component("scoreTask")
public class ScoreTask extends BaseTask{

    @Autowired
    ScoreService scoreService;

    @Async
    public void score(String overviewId, Float sigma, Float spacing, TaskDO taskDO) {
        scoreService.score(overviewId, sigma, spacing, taskDO);
    }
}
