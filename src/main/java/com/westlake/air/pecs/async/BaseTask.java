package com.westlake.air.pecs.async;

import com.westlake.air.pecs.domain.params.LumsParams;
import com.westlake.air.pecs.domain.bean.analyse.WindowRang;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.domain.query.AnalyseDataQuery;
import com.westlake.air.pecs.service.AnalyseDataService;
import com.westlake.air.pecs.service.ScanIndexService;
import com.westlake.air.pecs.service.ScoresService;
import com.westlake.air.pecs.service.TaskService;
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
    ScoresService scoresService;
    @Autowired
    ScanIndexService scanIndexService;

    public void score(String overviewId, LumsParams lumsParams, TaskDO taskDO) {

        List<WindowRang> rangs = lumsParams.getExperimentDO().getWindowRangs();
        HashMap<Float, ScanIndexDO> swathMap = scanIndexService.getSwathIndexList(lumsParams.getExperimentDO().getId());
        AnalyseDataQuery query = new AnalyseDataQuery(overviewId);
        query.setIsHit(true);

        for (int i = 0; i < rangs.size(); i++) {
            long start = System.currentTimeMillis();
            query.setMzStart(rangs.get(i).getMzStart());
            query.setMzEnd(rangs.get(i).getMzEnd());
            List<AnalyseDataDO> datas = analyseDataService.getAll(query);
            if (datas == null || datas.size() == 0) {
                continue;
            }
            taskDO.addLog("第" + (i+1) + "批打分开始,总共有" + rangs.size() + "批,共" + datas.size() + "个,读取卷积数据耗时:" + (System.currentTimeMillis() - start) + "毫秒");
            logger.info("第" + (i+1) + "批打分开始,总共有" + rangs.size() + "批,共" + datas.size() + "个,读取卷积数据耗时:" + (System.currentTimeMillis() - start) + "毫秒");
            taskService.update(taskDO);
            start = System.currentTimeMillis();
            lumsParams.setUsedDIAScores(true);

            scoresService.scoreForAll(datas, rangs.get(i), swathMap.get(rangs.get(i).getMzStart()), lumsParams);
            taskDO.addLog("第" + (i+1) + "批打分结束,本批次打分总计耗时(不包含读取数据库时间):" + (System.currentTimeMillis() - start) + "毫秒");
            logger.info("第" + (i+1) + "批打分结束,本批次打分总计耗时(不包含读取数据库时间):" + (System.currentTimeMillis() - start) + "毫秒");
            taskService.update(taskDO);
        }
    }

}
