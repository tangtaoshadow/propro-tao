package com.westlake.air.pecs.service;

import com.westlake.air.pecs.constants.TaskTemplate;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.domain.query.TaskQuery;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-15 10:05
 */
public interface TaskService {

    Long count(TaskQuery query);

    ResultDO<List<TaskDO>> getList(TaskQuery targetQuery);

    ResultDO insert(TaskDO taskDO);

    ResultDO delete(String id);

    ResultDO<TaskDO> getById(String id);

    ResultDO doTask(TaskTemplate taskTemplate);
}
