package com.westlake.air.propro.async.task;

import com.westlake.air.propro.algorithm.extract.Extractor;
import com.westlake.air.propro.algorithm.irt.Irt;
import com.westlake.air.propro.algorithm.learner.Airus;
import com.westlake.air.propro.algorithm.formula.FragmentFactory;
import com.westlake.air.propro.constants.TaskStatus;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.airus.AirusParams;
import com.westlake.air.propro.domain.bean.airus.FinalResult;
import com.westlake.air.propro.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.propro.domain.bean.irt.IrtResult;
import com.westlake.air.propro.domain.bean.score.SlopeIntercept;
import com.westlake.air.propro.domain.db.ExperimentDO;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.TaskDO;
import com.westlake.air.propro.domain.params.LumsParams;
import com.westlake.air.propro.service.*;
import com.westlake.air.propro.utils.ConvolutionUtil;
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
    Airus airus;
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
     * LumsParams 包含
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
    public void extract(TaskDO taskDO, LumsParams lumsParams) {

            long start = System.currentTimeMillis();
            //如果还没有计算irt,先执行计算irt的步骤
            if (lumsParams.getIRtLibrary() != null) {
                taskService.update(taskDO, "开始提取IRT校准库数据并且计算iRT值");
                ResultDO<IrtResult> resultDO = irt.extractAndAlign(lumsParams.getExperimentDO(), lumsParams.getIRtLibrary(), lumsParams.getMzExtractWindow(), lumsParams.getSigmaSpacing());
                if (resultDO.isFailed()) {
                    taskService.finish(taskDO, TaskStatus.FAILED.getName(), "iRT计算失败:" + resultDO.getMsgInfo() + ":" + resultDO.getMsgInfo());
                    return;
                }
                SlopeIntercept si = resultDO.getModel().getSi();
                lumsParams.setSlopeIntercept(si);
                taskDO.addLog("iRT计算完毕,斜率:" + si.getSlope() + "截距:" + si.getIntercept());
                experimentService.update(lumsParams.getExperimentDO());
            } else {
                taskDO.addLog("斜率:" + lumsParams.getSlopeIntercept().getSlope() + "截距:" + lumsParams.getSlopeIntercept().getIntercept());
            }

            taskService.update(taskDO, "入参准备完毕,开始提取数据(打分)");
            lumsParams.setTaskDO(taskDO);
            ResultDO result = extractor.extract(lumsParams);
            if (result.isFailed()) {
                taskService.finish(taskDO, TaskStatus.FAILED.getName(), "任务执行失败:" + result.getMsgInfo());
                return;
            }
            taskService.update(taskDO, "处理完毕,提取数据(打分)总耗时:" + (System.currentTimeMillis() - start) + "毫秒,开始进行合并打分.....");
            AirusParams ap = new AirusParams();
            ap.setScoreTypes(lumsParams.getScoreTypes());
            FinalResult finalResult = airus.doAirus(lumsParams.getOverviewId(), ap);
            taskDO.addLog("流程执行完毕,总耗时:" + (System.currentTimeMillis() - start) + ",最终识别的肽段数为" + finalResult.getMatchedPeptideCount());

    }

    @Async(value = "extractorExecutor")
    public void irt(TaskDO taskDO, LibraryDO library, List<ExperimentDO> exps, Float mzExtractWindow, SigmaSpacing sigmaSpacing) {

        for (ExperimentDO exp : exps) {
            taskService.update(taskDO, "Processing " + exp.getName() + "-" + exp.getId());

            ResultDO<IrtResult> resultDO = irt.extractAndAlign(exp, library, mzExtractWindow, sigmaSpacing);

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
