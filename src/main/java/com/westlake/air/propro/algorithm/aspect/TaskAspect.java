package com.westlake.air.propro.algorithm.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TaskAspect {

    public final Logger logger = LoggerFactory.getLogger(TaskAspect.class);

    @Around("execution(* com.westlake.air.propro.async.task.ExperimentTask.convAndIrt(..))")
    public Object countTime(ProceedingJoinPoint joinPoint) {
        Object obj = null;
        Object[] args = joinPoint.getArgs();
        long startTime = System.currentTimeMillis();
        logger.info("开始提取并且计算IRT");
        try {
            obj = joinPoint.proceed(args);
        } catch (Throwable e) {
            logger.error("统计某方法执行耗时环绕通知出错", e);
        }

        logger.info("IRT耗时：" + (System.currentTimeMillis() - startTime)/1000 + "秒");

        return obj;
    }

}
