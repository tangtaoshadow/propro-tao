package com.westlake.air.pecs.async;

import com.westlake.air.pecs.algorithm.Airus;
import com.westlake.air.pecs.compressor.Compressor;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.SwathInput;
import com.westlake.air.pecs.domain.bean.airus.FinalResult;
import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.pecs.domain.bean.analyse.WindowRang;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.ScoresDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.service.ExperimentService;
import com.westlake.air.pecs.service.ScoresService;
import com.westlake.air.pecs.utils.AirusUtil;
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
    @Autowired
    Airus airus;
    @Autowired
    Compressor compressor;

    @Async
    public void saveExperimentTask(ExperimentDO experimentDO, File file, TaskDO taskDO) {
        experimentService.uploadFile(experimentDO, file, taskDO);
        List<WindowRang> rangs = experimentService.getWindows(experimentDO.getId());
        experimentDO.setWindowRangs(rangs);
        experimentService.update(experimentDO);
    }

    @Async
    public void compressionAndSort(ExperimentDO experimentDO, TaskDO taskDO) {
        long start = System.currentTimeMillis();
        compressor.doCompress(experimentDO);
        taskDO.addLog("转还完毕,总耗时:"+(System.currentTimeMillis() - start));
        taskDO.finish(TaskDO.STATUS_SUCCESS);
        taskService.update(taskDO);
    }

    /**
     * @param experimentDO
     * @param libraryId
     * @param slopeIntercept
     * @param creator
     * @param rtExtractWindow
     * @param mzExtractWindow
     * @return
     */
    @Async
    public void extract(ExperimentDO experimentDO, String libraryId, SlopeIntercept slopeIntercept, String creator, float rtExtractWindow, float mzExtractWindow, TaskDO taskDO) {
        SwathInput input = new SwathInput();
        input.setExperimentDO(experimentDO);
        input.setLibraryId(libraryId);
        input.setSlopeIntercept(slopeIntercept);
        input.setCreator(creator);
        input.setRtExtractWindow(rtExtractWindow);
        input.setMzExtractWindow(mzExtractWindow);

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
    public void convAndIrt(ExperimentDO experimentDO, String iRtLibraryId, Float mzExtractWindow, SigmaSpacing sigmaSpacing, TaskDO taskDO) {
        taskDO.addLog("开始卷积IRT校准库并且计算iRT值");
        taskService.update(taskDO);

        ResultDO<SlopeIntercept> resultDO = experimentService.convAndIrt(experimentDO, iRtLibraryId, mzExtractWindow, sigmaSpacing);
        SlopeIntercept slopeIntercept = resultDO.getModel();

        experimentDO.setSlope(slopeIntercept.getSlope());
        experimentDO.setIntercept(slopeIntercept.getIntercept());
        experimentService.update(experimentDO);

        taskDO.addLog("iRT计算完毕,斜率:" + slopeIntercept.getSlope() + ",截距:" + slopeIntercept.getIntercept());
        taskDO.finish(TaskDO.STATUS_SUCCESS);
        taskService.update(taskDO);
    }

    @Async
    public void swath(SwathInput input, TaskDO taskDO) {
        long startAll = System.currentTimeMillis();
        long start = System.currentTimeMillis();
        taskDO.addLog("开始卷积IRT校准库并且计算iRT值");
        taskService.update(taskDO);

        ExperimentDO experimentDO = input.getExperimentDO();
        ResultDO<SlopeIntercept> resultDO = experimentService.convAndIrt(experimentDO, input.getIRtLibraryId(), input.getMzExtractWindow(), input.getSigmaSpacing());
        SlopeIntercept slopeIntercept = resultDO.getModel();

        experimentDO.setSlope(slopeIntercept.getSlope());
        experimentDO.setIntercept(slopeIntercept.getIntercept());
        experimentService.update(experimentDO);

        taskDO.addLog("iRT计算完毕,耗时:"+(System.currentTimeMillis() - start)+"毫秒,斜率:" + slopeIntercept.getSlope() + ",截距:" + slopeIntercept.getIntercept() + ",开始卷积原始数据");
        taskService.update(taskDO);

        start = System.currentTimeMillis();
        //将irt的计算结果加入到下一个步骤的入参中
        input.setSlopeIntercept(slopeIntercept);
        ResultDO<List<AnalyseDataDO>> originDataListResult = experimentService.extract(input);

        if(originDataListResult.isFailed() || originDataListResult.getModel() == null || originDataListResult.getModel().size() == 0){
            taskDO.addLog("卷积失败:"+originDataListResult.getMsgInfo());
            taskDO.finish(TaskDO.STATUS_FAILED);
            taskService.update(taskDO);
        }

        taskDO.addLog("卷积完毕,耗时:" + (System.currentTimeMillis() - start) + ",开始打分");
        taskService.update(taskDO);

        start = System.currentTimeMillis();
        List<AnalyseDataDO> dataList = originDataListResult.getModel();
        List<ScoresDO> scores = scoresService.score(dataList, input);

        taskDO.addLog("子分数打分完毕,耗时:" + (System.currentTimeMillis() - start) + ",开始合并打分");
        taskService.update(taskDO);
        start = System.currentTimeMillis();
        FinalResult finalResult = airus.doAirus(scores);

        int count = AirusUtil.checkFdr(finalResult);
        taskDO.addLog("合并打分完毕,耗时:" + (System.currentTimeMillis() - start) + ",最终识别的肽段数为"+count);
        taskDO.addLog("Swath流程总计耗时:" + (System.currentTimeMillis() - startAll));
        taskService.update(taskDO);

        taskDO.finish(TaskDO.STATUS_SUCCESS);
        taskService.update(taskDO);
    }
}
