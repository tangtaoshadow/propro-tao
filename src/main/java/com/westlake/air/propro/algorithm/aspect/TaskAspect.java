package com.westlake.air.propro.algorithm.aspect;

import com.westlake.air.propro.constants.TaskStatus;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.TaskDO;
import com.westlake.air.propro.domain.params.LumsParams;
import com.westlake.air.propro.service.TaskService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Aspect
@Component
public class TaskAspect {

    public final Logger logger = LoggerFactory.getLogger(TaskAspect.class);

    @Autowired
    TaskService taskService;

    @Around("execution(* com.westlake.air.propro.async.task.ExperimentTask.irt(..))")
    public Object irtAround(ProceedingJoinPoint joinPoint) {
        Object result = null;
        Object[] args = joinPoint.getArgs();
        TaskDO taskDO = (TaskDO) args[0];
        LibraryDO library = (LibraryDO) args[1];
        ArrayList exps = (ArrayList) args[2];
        taskDO.start();
        if (exps != null && library != null) {
            taskService.update(taskDO, TaskStatus.RUNNING.getName(), "开始分析IRT校准库并且计算iRT值,总计" + exps.size() + "个目标实验,Library ID:" + library.getId() + ";Type:" + library.getType());
        }

        try {
            result = joinPoint.proceed(args);
        } catch (Throwable e) {
            e.printStackTrace();
            taskService.finish(taskDO, TaskStatus.FAILED.getName(), "Error:" + e.getMessage());
        }

        taskService.finish(taskDO, TaskStatus.SUCCESS.getName(), "SUCCESS");

        return result;
    }

    @Around("execution(* com.westlake.air.propro.async.task.ExperimentTask.extract(..))")
    public Object extractAround(ProceedingJoinPoint joinPoint) {
        Object result = null;
        Object[] args = joinPoint.getArgs();
        TaskDO taskDO = (TaskDO) args[0];
        LumsParams lumsParams = (LumsParams) args[1];
        taskDO.start();
        taskDO.setStatus(TaskStatus.RUNNING.getName());
        taskDO.addLog("数据提取窗口:" + lumsParams.getExtractParams().toString())
                .addLog("Sigma:" + lumsParams.getSigmaSpacing().getSigma() + ",Spacing:" + lumsParams.getSigmaSpacing().getSpacing())
                .addLog("使用标准库ID:" + lumsParams.getLibrary().getId())
                .addLog("FDR:" + lumsParams.getFdr())
                .addLog("Note:" + lumsParams.getNote())
                .addLog("使用限制阈值Shape/ShapeWeight:" + lumsParams.getXcorrShapeThreshold() + "/" + lumsParams.getXcorrShapeWeightThreshold());
        taskService.update(taskDO, TaskStatus.RUNNING.getName(), "");

        try {
            result = joinPoint.proceed(args);
        } catch (Throwable e) {
            e.printStackTrace();
            taskService.update(taskDO, TaskStatus.FAILED.getName(), "Error:" + e.getMessage());
        }

        taskService.finish(taskDO, TaskStatus.SUCCESS.getName(),"SUCCESS");
        return result;
    }

}
