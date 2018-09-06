package com.westlake.air.pecs.async;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.SwathInput;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.service.ExperimentService;
import com.westlake.air.pecs.service.ScoresService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-17 10:40
 */
@Component("experimentTask")
public class ExperimentTask extends BaseTask {
    @Autowired
    ExperimentService experimentService;
    @Autowired
    ScoresService scoresService;

    @Async
    public void saveExperimentTask(ExperimentDO experimentDO, File file, TaskDO taskDO) {
        experimentService.uploadFile(experimentDO, file, taskDO);
    }

    /**
     * @param experimentDO
     * @param libraryId
     * @param slopeIntercept
     * @param creator
     * @param rtExtractWindow
     * @param mzExtractWindow
     * @param buildType       0:解压缩MS1和MS2; 1:解压缩MS1; 2:解压缩MS2
     * @return
     */
    @Async
    public void extract(ExperimentDO experimentDO, String libraryId, SlopeIntercept slopeIntercept, String creator, float rtExtractWindow, float mzExtractWindow, int buildType, TaskDO taskDO) {
        SwathInput input = new SwathInput();
        input.setExperimentDO(experimentDO);
        input.setLibraryId(libraryId);
        input.setSlopeIntercept(slopeIntercept);
        input.setCreator(creator);
        input.setRtExtractWindow(rtExtractWindow);
        input.setMzExtractWindow(mzExtractWindow);
        input.setBuildType(buildType);

        taskDO.addLog("录入有斜率:"+slopeIntercept.getSlope()+"截距:"+slopeIntercept.getIntercept());
        taskDO.addLog("使用标准库ID:"+libraryId);
        taskDO.addLog("入参准备完毕,开始卷积,时间可能较长");
        taskService.update(taskDO);
        long start = System.currentTimeMillis();
        experimentService.extract(input);

        taskDO.addLog("卷积完毕,总耗时:"+(System.currentTimeMillis() - start));
        taskDO.finish(TaskDO.STATUS_SUCCESS);
        taskService.update(taskDO);
    }

    @Async
    public void swath(SwathInput input, TaskDO taskDO) {
        taskDO.addLog("开始卷积IRT校准库并且计算iRT值");
        taskService.update(taskDO);

        ResultDO<SlopeIntercept> resultDO = experimentService.convAndComputeIrt(input.getExperimentDO(), input.getIRtLibraryId(), input.getMzExtractWindow(), input.getSigmaSpacing());
        SlopeIntercept slopeIntercept = resultDO.getModel();

        taskDO.addLog("iRT计算完毕,斜率:" + slopeIntercept.getSlope() + ",截距:" + slopeIntercept.getIntercept() + "开始卷积原始数据");
        taskService.update(taskDO);

        long start = System.currentTimeMillis();
        //将irt的计算结果加入到下一个步骤的入参中
        input.setSlopeIntercept(slopeIntercept);
        ResultDO<List<AnalyseDataDO>> originDataListResult = experimentService.extractWithList(input);

        taskDO.addLog("卷积完毕,耗时:" + (System.currentTimeMillis() - start));
        taskService.update(taskDO);

        if(originDataListResult.isFailed() || originDataListResult.getModel() == null || originDataListResult.getModel().size() == 0){
            taskDO.addLog("卷积失败:"+originDataListResult.getMsgInfo());
            taskDO.finish(TaskDO.STATUS_FAILED);
            taskService.update(taskDO);
        }

        List<AnalyseDataDO> dataList = originDataListResult.getModel();

        scoresService.score(dataList, input);

        taskDO.finish(TaskDO.STATUS_SUCCESS);
        taskService.update(taskDO);
    }
}
