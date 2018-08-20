package com.westlake.air.pecs.service;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.TaskDO;

import java.util.List;

public interface ScoreService {

    ResultDO<SlopeIntercept> computeIRt(String overviewId, Float sigma, Float space, TaskDO taskDO);
}
