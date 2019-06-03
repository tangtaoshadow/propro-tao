package com.westlake.air.propro.async;

import com.westlake.air.propro.domain.bean.aird.WindowRange;
import com.westlake.air.propro.domain.db.SwathIndexDO;
import com.westlake.air.propro.domain.params.LumsParams;
import com.westlake.air.propro.domain.db.AnalyseDataDO;
import com.westlake.air.propro.domain.db.ScanIndexDO;
import com.westlake.air.propro.domain.db.TaskDO;
import com.westlake.air.propro.domain.query.AnalyseDataQuery;
import com.westlake.air.propro.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
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
    ScoreService scoreService;
    @Autowired
    SwathIndexService swathIndexService;

    public void score(String overviewId, LumsParams lumsParams, TaskDO taskDO) {

        List<SwathIndexDO> swathIndexList = swathIndexService.getAllMS2ByExpId(lumsParams.getExperimentDO().getId());
        AnalyseDataQuery query = new AnalyseDataQuery(overviewId);
        for (int i = 0; i < swathIndexList.size(); i++) {
            long start = System.currentTimeMillis();
            query.setMzStart(swathIndexList.get(i).getRange().getStart());
            query.setMzEnd(swathIndexList.get(i).getRange().getEnd());
            List<AnalyseDataDO> datas = analyseDataService.getAll(query);
            if (datas == null || datas.size() == 0) {
                continue;
            }
            taskDO.addLog("第" + (i+1) + "批打分开始,总共有" + swathIndexList.size() + "批,共" + datas.size() + "个,读取卷积数据耗时:" + (System.currentTimeMillis() - start) + "毫秒");
            logger.info("第" + (i+1) + "批打分开始,总共有" + swathIndexList.size() + "批,共" + datas.size() + "个,读取卷积数据耗时:" + (System.currentTimeMillis() - start) + "毫秒");
            taskService.update(taskDO);
            start = System.currentTimeMillis();
            lumsParams.setUsedDIAScores(true);

            scoreService.scoreForAll(datas, swathIndexList.get(i), lumsParams);
            taskDO.addLog("第" + (i+1) + "批打分结束,本批次打分总计耗时(不包含读取数据库时间):" + (System.currentTimeMillis() - start) + "毫秒");
            logger.info("第" + (i+1) + "批打分结束,本批次打分总计耗时(不包含读取数据库时间):" + (System.currentTimeMillis() - start) + "毫秒");
            taskService.update(taskDO);
        }

    }

}
