package com.westlake.air.pecs.async;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.SwathParams;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.domain.query.AnalyseDataQuery;
import com.westlake.air.pecs.service.AnalyseDataService;
import com.westlake.air.pecs.service.ScoresService;
import com.westlake.air.pecs.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-29 20:28
 */
public class BaseTask {

    public final Logger logger = LoggerFactory.getLogger(BaseTask.class);

    @Autowired
    TaskService taskService;
    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    ScoresService scoresService;

    public void score(String overviewId, SwathParams swathParams, TaskDO taskDO){
        AnalyseDataQuery query = new AnalyseDataQuery(overviewId);
        query.setIsHit(true);

        Long count = analyseDataService.count(query);
        long pages = count/10000 + 1;
        query.setPageSize(10000);
        for(int i = 0;i <= pages;i++){
            query.setPageNo(i);

            long start = System.currentTimeMillis();
            ResultDO<List<AnalyseDataDO>> datasResult = analyseDataService.getList(query);
            if(datasResult.isFailed() || datasResult.getModel() == null || datasResult.getModel().size() == 0){
                continue;
            }
            taskDO.addLog("第"+(i+1)+"批打分开始,总共有"+pages+"批,共"+count+"个,读取卷积数据耗时:"+(System.currentTimeMillis() - start)+"毫秒");
            logger.info("第"+(i+1)+"批打分开始,总共有"+pages+"批,共"+count+"个,读取卷积数据耗时:"+(System.currentTimeMillis() - start)+"毫秒");
            taskService.update(taskDO);
            start = System.currentTimeMillis();
            scoresService.score(datasResult.getModel(), swathParams);
            taskDO.addLog("第"+(i+1)+"批打分结束,本批次打分总计耗时(不包含读取数据库时间):"+(System.currentTimeMillis() - start)+"毫秒");
            logger.info("第"+(i+1)+"批打分结束,本批次打分总计耗时(不包含读取数据库时间):"+(System.currentTimeMillis() - start)+"毫秒");
            taskService.update(taskDO);
        }
    }

}
