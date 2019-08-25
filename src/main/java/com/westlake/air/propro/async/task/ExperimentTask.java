package com.westlake.air.propro.async.task;

import com.westlake.air.propro.algorithm.extract.Extractor;
import com.westlake.air.propro.algorithm.irt.Irt;
import com.westlake.air.propro.algorithm.learner.SemiSupervise;
import com.westlake.air.propro.algorithm.formula.FragmentFactory;
import com.westlake.air.propro.constants.enums.TaskStatus;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.learner.LearningParams;
import com.westlake.air.propro.domain.bean.learner.FinalResult;
import com.westlake.air.propro.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.propro.domain.bean.irt.IrtResult;
import com.westlake.air.propro.domain.bean.score.SlopeIntercept;
import com.westlake.air.propro.domain.db.ExperimentDO;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.TaskDO;
import com.westlake.air.propro.domain.params.IrtParams;
import com.westlake.air.propro.domain.params.WorkflowParams;
import com.westlake.air.propro.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

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
    SemiSupervise semiSupervise;
    @Autowired
    PeptideService peptideService;
    @Autowired
    FragmentFactory fragmentFactory;
    @Autowired
    LibraryService libraryService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    Irt irt;
    @Autowired
    Extractor extractor;

    @Async(value = "uploadFileExecutor")
    public void uploadAird(List<ExperimentDO> exps, TaskDO taskDO) {
        try {
            taskDO.start();
            taskDO.setStatus(TaskStatus.RUNNING.getName());
            taskService.update(taskDO);
            for (ExperimentDO exp : exps) {
                experimentService.uploadAirdFile(exp, taskDO);
                experimentService.update(exp);
            }
            taskDO.finish(TaskStatus.SUCCESS.getName());
            taskService.update(taskDO);
        } catch (Exception e) {
            e.printStackTrace();
            taskDO.finish(TaskStatus.FAILED.getName());
            taskService.update(taskDO, "Error:" + e.getMessage());
        }

    }

    /**
     * WorkflowParams 包含
     * experimentDO
     * libraryId
     * slopeIntercept
     * ownerName
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
    public void extract(TaskDO taskDO, WorkflowParams workflowParams) {

        long start = System.currentTimeMillis();
        //如果还没有计算irt,先执行计算irt的步骤
        if (workflowParams.getIRtLibrary() != null) {
            taskService.update(taskDO, "开始提取IRT校准库数据并且计算iRT值");
            IrtParams irtParams = new IrtParams();
            irtParams.setLibrary(workflowParams.getIRtLibrary());
            irtParams.setMzExtractWindow(workflowParams.getExtractParams().getMzExtractWindow());
            irtParams.setSigmaSpacing(workflowParams.getSigmaSpacing());
            ResultDO<IrtResult> resultDO = irt.extractAndAlign(workflowParams.getExperimentDO(), irtParams);
            if (resultDO.isFailed()) {
                taskService.finish(taskDO, TaskStatus.FAILED.getName(), "iRT计算失败:" + resultDO.getMsgInfo() + ":" + resultDO.getMsgInfo());
                return;
            }
            SlopeIntercept si = resultDO.getModel().getSi();
            workflowParams.setSlopeIntercept(si);
            taskDO.addLog("iRT计算完毕,斜率:" + si.getSlope() + "截距:" + si.getIntercept());
            experimentService.update(workflowParams.getExperimentDO());
        } else {
            taskDO.addLog("斜率:" + workflowParams.getSlopeIntercept().getSlope() + "截距:" + workflowParams.getSlopeIntercept().getIntercept());
        }

        taskService.update(taskDO, "入参准备完毕,开始提取数据(打分)");
        workflowParams.setTaskDO(taskDO);
        ResultDO result = extractor.extract(workflowParams);
        if (result.isFailed()) {
            taskService.finish(taskDO, TaskStatus.FAILED.getName(), "任务执行失败:" + result.getMsgInfo());
            return;
        }
        taskService.update(taskDO, "处理完毕,提取数据(打分)总耗时:" + (System.currentTimeMillis() - start) + "毫秒,开始进行合并打分.....");
        LearningParams ap = new LearningParams();
        ap.setScoreTypes(workflowParams.getScoreTypes());
        ap.setFdr(workflowParams.getFdr());
        FinalResult finalResult = semiSupervise.doSemiSupervise(workflowParams.getOverviewId(), ap);
        taskDO.addLog("流程执行完毕,总耗时:" + (System.currentTimeMillis() - start) + ",最终识别的肽段数为" + finalResult.getMatchedPeptideCount()+"最终识别的蛋白数目为:"+finalResult.getMatchedProteinCount());
        taskDO.addLog("Peptide/Protein Rate:"+finalResult.getMatchedPeptideCount()/finalResult.getMatchedProteinCount());
    }

    @Async(value = "extractorExecutor")
    public void irt(TaskDO taskDO, List<ExperimentDO> exps, IrtParams irtParams) {

        LibraryDO library = irtParams.getLibrary();

        for (ExperimentDO exp : exps) {
            taskService.update(taskDO, "Processing " + exp.getName() + "-" + exp.getId());

            ResultDO<IrtResult> resultDO = irt.extractAndAlign(exp, irtParams);

            if (resultDO.isFailed()) {
                taskService.update(taskDO, "iRT计算失败:" + resultDO.getMsgInfo() + ":" + resultDO.getMsgInfo());
                continue;
            }
            SlopeIntercept slopeIntercept = resultDO.getModel().getSi();
            exp.setIRtLibraryId(library.getId());
            experimentService.update(exp);

            taskDO.addLog("iRT计算完毕,斜率:" + slopeIntercept.getSlope() + ",截距:" + slopeIntercept.getIntercept());
        }
    }
}
