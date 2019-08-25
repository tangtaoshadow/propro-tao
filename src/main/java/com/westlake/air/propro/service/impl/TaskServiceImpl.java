package com.westlake.air.propro.service.impl;

import com.westlake.air.propro.constants.enums.ResultCode;
import com.westlake.air.propro.constants.enums.TaskTemplate;
import com.westlake.air.propro.dao.TaskDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.TaskDO;
import com.westlake.air.propro.domain.query.TaskQuery;
import com.westlake.air.propro.service.TaskService;
import org.apache.commons.lang3.StringUtils;
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
    public List<TaskDO> getAll(TaskQuery targetQuery) {
        return taskDAO.getAll(targetQuery);
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
            taskDO.setLastModifiedDate(new Date());
            taskDAO.insert(taskDO);
            return ResultDO.build(taskDO);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return ResultDO.buildError(ResultCode.INSERT_ERROR);
        }
    }

    @Override
    public ResultDO update(TaskDO taskDO) {
        if (taskDO.getId() == null || taskDO.getId().isEmpty()) {
            return ResultDO.buildError(ResultCode.ID_CANNOT_BE_NULL_OR_ZERO);
        }

        try {
            taskDO.setLastModifiedDate(new Date());
            taskDAO.update(taskDO);
            return ResultDO.build(taskDO);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return ResultDO.buildError(ResultCode.INSERT_ERROR);
        }
    }

    @Override
    public ResultDO update(TaskDO taskDO, String newLog) {
        logger.info(newLog);
        taskDO.addLog(newLog);
        return update(taskDO);
    }

    @Override
    public ResultDO update(TaskDO taskDO, String status, String newLog) {
        if (StringUtils.isNotEmpty(newLog)) {
            logger.info(newLog);
            taskDO.addLog(newLog);
        }

        taskDO.setStatus(status);
        taskDO.addLog("Task Status:" + status);
        return update(taskDO);
    }

    @Override
    public ResultDO finish(TaskDO taskDO, String status, String newLog) {
        if (StringUtils.isNotEmpty(newLog)) {
            taskDO.addLog(newLog);
        }

        taskDO.finish(status);
        logger.info("Task完整耗时测试：" + (taskDO.getTotalCost() > 1000 ? (taskDO.getTotalCost() / 1000 + "秒") : (taskDO.getTotalCost() + "毫秒")));
        return update(taskDO);
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

    @Override
    public ResultDO doTask(TaskTemplate taskTemplate) {
        return null;
    }
}
