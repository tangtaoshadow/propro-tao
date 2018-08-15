package com.westlake.air.pecs.service.impl;

import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.dao.TaskDAO;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.domain.query.TaskQuery;
import com.westlake.air.pecs.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-15 10:05
 */
@Service("taskService")
public class TaskServiceImpl implements TaskService {

    public final Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);

    @Autowired
    TaskDAO taskDAO;

    @Override
    public Long count(TaskQuery query) {
        return taskDAO.count(query);
    }

    @Override
    public ResultDO<List<TaskDO>> getList(TaskQuery targetQuery) {
        List<TaskDO> taskList = taskDAO.getList(targetQuery);
        long totalCount = taskDAO.count(targetQuery);
        ResultDO<List<TaskDO>> resultDO = new ResultDO<>(true);
        resultDO.setModel(taskList);
        resultDO.setTotalNum(totalCount);
        resultDO.setPageSize(targetQuery.getPageSize());

        return resultDO;
    }

    @Override
    public ResultDO insert(TaskDO taskDO) {
        try {
            taskDO.setCreateDate(new Date());
            taskDAO.insert(taskDO);
            return ResultDO.build(taskDO);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return ResultDO.buildError(ResultCode.INSERT_ERROR);
        }
    }

    @Override
    public ResultDO delete(String id) {
        if (id == null || id.isEmpty()) {
            return ResultDO.buildError(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }
        try {
            taskDAO.delete(id);
            return new ResultDO(true);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return ResultDO.buildError(ResultCode.DELETE_ERROR);
        }
    }

    @Override
    public ResultDO<TaskDO> getById(String id) {
        try {
            TaskDO taskDO = taskDAO.getById(id);
            if (taskDO == null) {
                return ResultDO.buildError(ResultCode.OBJECT_NOT_EXISTED);
            } else {
                ResultDO<TaskDO> resultDO = new ResultDO<>(true);
                resultDO.setModel(taskDO);
                return resultDO;
            }
        } catch (Exception e) {
            return ResultDO.buildError(ResultCode.QUERY_ERROR);
        }
    }
}
