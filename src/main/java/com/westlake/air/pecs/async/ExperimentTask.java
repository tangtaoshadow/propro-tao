package com.westlake.air.pecs.async;

import com.westlake.air.pecs.algorithm.Airus;
import com.westlake.air.pecs.algorithm.FragmentFactory;
import com.westlake.air.pecs.compressor.AirdCompressor;
import com.westlake.air.pecs.constants.TaskStatus;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.pecs.domain.bean.analyse.WindowRang;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.domain.params.LumsParams;
import com.westlake.air.pecs.service.*;
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
    AnalyseDataService analyseDataService;
    @Autowired
    ScoreService scoreService;
    @Autowired
    Airus airus;
    @Autowired
    AirdCompressor airdCompressor;
    @Autowired
    PeptideService peptideService;
    @Autowired
    FragmentFactory fragmentFactory;
    @Autowired
    LibraryService libraryService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;

    @Async(value = "uploadFileExecutor")
    public void saveExperimentTask(ExperimentDO experimentDO, File file, TaskDO taskDO) {
        taskDO.setStatus(TaskStatus.RUNNING.getName());
        taskService.update(taskDO);
        experimentService.uploadFile(experimentDO, file, taskDO);
        List<WindowRang> rangs = experimentService.getWindows(experimentDO.getId());
        experimentDO.setWindowRangs(rangs);
        experimentService.update(experimentDO);
    }

    @Async(value = "compressFileExecutor")
    public void compress(ExperimentDO experimentDO, TaskDO taskDO) {
        taskDO.setStatus(TaskStatus.RUNNING.getName());
        taskService.update(taskDO);
        long start = System.currentTimeMillis();
        airdCompressor.compress(experimentDO);
        taskDO.addLog("压缩转换完毕,总耗时:" + (System.currentTimeMillis() - start));
        taskDO.finish(TaskStatus.SUCCESS.getName());
        taskService.update(taskDO);
    }

    /**
     * LumsParams 包含
     * experimentDO
     * libraryId
     * slopeIntercept
     * creator
     * rtExtractWindow
     * mzExtractWindow
     * useEpps
     * scoreTypes
     * sigmaSpacing
     * shapeScoreThreshold
     * shapeScoreWeightThreshold
     * @return
     */
    @Async(value = "extractorExecutor")
    public void extract(LumsParams lumsParams, TaskDO taskDO) {
        taskDO.setStatus(TaskStatus.RUNNING.getName());
        taskService.update(taskDO);

        taskDO.addLog("录入有斜率:" + lumsParams.getSlopeIntercept().getSlope() + "截距:" + lumsParams.getSlopeIntercept().getIntercept());
        taskDO.addLog("mz卷积窗口:" + lumsParams.getMzExtractWindow() + ",RT卷积窗口:" + lumsParams.getRtExtractWindow());
        taskDO.addLog("Sigma:" + lumsParams.getSigmaSpacing().getSigma() + ",Spacing:" + lumsParams.getSigmaSpacing().getSpacing());
        taskDO.addLog("使用标准库ID:" + lumsParams.getLibraryId());
        taskDO.addLog("入参准备完毕,开始卷积,时间可能较长");
        taskService.update(taskDO);
        long start = System.currentTimeMillis();
        experimentService.extract(lumsParams);

        taskDO.addLog("处理完毕,总耗时:" + (System.currentTimeMillis() - start));
        logger.info("处理完毕,总耗时:" + (System.currentTimeMillis() - start));
        taskDO.finish(TaskStatus.SUCCESS.getName());
        taskService.update(taskDO);
    }

    @Async(value = "extractorExecutor")
    public void convAndIrt(ExperimentDO experimentDO, String iRtLibraryId, Float mzExtractWindow, SigmaSpacing sigmaSpacing, TaskDO taskDO) {
        taskDO.addLog("开始卷积IRT校准库并且计算iRT值");
        taskDO.setStatus(TaskStatus.RUNNING.getName());
        taskService.update(taskDO);

        ResultDO<SlopeIntercept> resultDO = experimentService.convAndIrt(experimentDO, iRtLibraryId, mzExtractWindow, sigmaSpacing);
        if (resultDO.isFailed()) {
            taskDO.addLog(resultDO.getMsgInfo() + ":" + resultDO.getMsgInfo());
            taskDO.finish(TaskStatus.SUCCESS.getName());
            taskService.update(taskDO);
            return;
        }
        SlopeIntercept slopeIntercept = resultDO.getModel();

        experimentDO.setSlope(slopeIntercept.getSlope());
        experimentDO.setIntercept(slopeIntercept.getIntercept());
        experimentDO.setIRtLibraryId(iRtLibraryId);
        experimentService.update(experimentDO);

        taskDO.addLog("iRT计算完毕,斜率:" + slopeIntercept.getSlope() + ",截距:" + slopeIntercept.getIntercept());
        taskDO.finish(TaskStatus.SUCCESS.getName());
        taskService.update(taskDO);
    }
}
