package com.westlake.air.propro.async;

import com.westlake.air.propro.algorithm.learner.Airus;
import com.westlake.air.propro.algorithm.formula.FragmentFactory;
import com.westlake.air.propro.constants.Constants;
import com.westlake.air.propro.constants.TaskStatus;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.airus.AirusParams;
import com.westlake.air.propro.domain.bean.airus.FinalResult;
import com.westlake.air.propro.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.propro.domain.bean.analyse.WindowRange;
import com.westlake.air.propro.domain.bean.score.SlopeIntercept;
import com.westlake.air.propro.domain.db.ExperimentDO;
import com.westlake.air.propro.domain.db.TaskDO;
import com.westlake.air.propro.domain.params.LumsParams;
import com.westlake.air.propro.service.*;
import org.apache.commons.lang3.StringUtils;
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
    PeptideService peptideService;
    @Autowired
    FragmentFactory fragmentFactory;
    @Autowired
    LibraryService libraryService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;

    @Async(value = "uploadFileExecutor")
    public void saveExperimentTask(ExperimentDO experimentDO, File file, TaskDO taskDO) {
        taskDO.start();
        taskDO.setStatus(TaskStatus.RUNNING.getName());
        taskService.update(taskDO);
        experimentService.uploadFile(experimentDO, file, taskDO);
        List<WindowRange> rangs;
        if (experimentDO.getType().equals(Constants.EXP_TYPE_PRM)) {
            rangs = experimentService.getPrmWindows(experimentDO.getId());
        } else {
            rangs = experimentService.getWindows(experimentDO.getId());
        }
        experimentDO.setWindowRanges(rangs);
        experimentService.update(experimentDO);
    }

    @Async(value = "uploadFileExecutor")
    public void saveAirdTask(ExperimentDO experimentDO, String airdFilePath, TaskDO taskDO) {
        taskDO.start();
        taskDO.setStatus(TaskStatus.RUNNING.getName());
        taskService.update(taskDO);
        experimentService.uploadAirdFile(experimentDO, airdFilePath, taskDO);
        experimentService.update(experimentDO);
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
     *
     * @return
     */
    @Async(value = "extractorExecutor")
    public void extract(LumsParams lumsParams, TaskDO taskDO) {
        taskDO.start();
        taskDO.setStatus(TaskStatus.RUNNING.getName());
        taskService.update(taskDO);

        taskDO.addLog("mz卷积窗口:" + lumsParams.getMzExtractWindow() + ",RT卷积窗口:" + lumsParams.getRtExtractWindow());
        taskDO.addLog("Sigma:" + lumsParams.getSigmaSpacing().getSigma() + ",Spacing:" + lumsParams.getSigmaSpacing().getSpacing());
        taskDO.addLog("使用标准库ID:" + lumsParams.getLibraryId());
        taskDO.addLog("Note:" + lumsParams.getNote());
        taskDO.addLog("使用限制阈值Shape/ShapeWeight:" + lumsParams.getXcorrShapeThreshold() + "/" + lumsParams.getXcorrShapeWeightThreshold());

        long start = System.currentTimeMillis();
        if (StringUtils.isNotEmpty(lumsParams.getIRtLibraryId())) {
            taskDO.addLog("开始卷积IRT校准库并且计算iRT值");
            taskService.update(taskDO);
            ResultDO<SlopeIntercept> resultDO = experimentService.convAndIrt(lumsParams.getExperimentDO(), lumsParams.getIRtLibraryId(), lumsParams.getMzExtractWindow(), lumsParams.getSigmaSpacing());
            if(resultDO.isFailed()){
                taskDO.addLog("iRT计算失败:"+resultDO.getMsgInfo() + ":" + resultDO.getMsgInfo());
                taskDO.finish(TaskStatus.FAILED.getName());
                taskService.update(taskDO);
                return;
            }
            SlopeIntercept si = resultDO.getModel();
            lumsParams.setSlopeIntercept(si);
            taskDO.addLog("iRT计算完毕");
            taskDO.addLog("斜率:" + si.getSlope() + "截距:" + si.getIntercept());

        }else{
            taskDO.addLog("斜率:" + lumsParams.getSlopeIntercept().getSlope() + "截距:" + lumsParams.getSlopeIntercept().getIntercept());
        }

        taskDO.addLog("入参准备完毕,开始卷积(打分),时间可能较长");
        taskService.update(taskDO);
        experimentService.extract(lumsParams);
        taskDO.addLog("处理完毕,卷积(打分)总耗时:" + (System.currentTimeMillis() - start));
        taskDO.addLog("开始进行合并打分");
        taskService.update(taskDO);
        FinalResult finalResult = airus.doAirus(lumsParams.getOverviewId(), new AirusParams());
        int matchedPeptideCount = finalResult.getMatchedPeptideCount();

        taskDO.addLog("流程执行完毕,总耗时:" + (System.currentTimeMillis() - start) + ",最终识别的肽段数为" + matchedPeptideCount);
        taskDO.finish(TaskStatus.SUCCESS.getName());
        taskService.update(taskDO);
    }

    @Async(value = "extractorExecutor")
    public void convAndIrt(ExperimentDO experimentDO, String iRtLibraryId, Float mzExtractWindow, SigmaSpacing sigmaSpacing, TaskDO taskDO) {
        taskDO.start();
        taskDO.addLog("开始卷积IRT校准库并且计算iRT值,iRT Library ID:" + iRtLibraryId);
        taskDO.setStatus(TaskStatus.RUNNING.getName());
        taskService.update(taskDO);

        ResultDO<SlopeIntercept> resultDO = experimentService.convAndIrt(experimentDO, iRtLibraryId, mzExtractWindow, sigmaSpacing);
        if (resultDO.isFailed()) {
            taskDO.addLog("iRT计算失败:"+resultDO.getMsgInfo() + ":" + resultDO.getMsgInfo());
            taskDO.finish(TaskStatus.FAILED.getName());
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
