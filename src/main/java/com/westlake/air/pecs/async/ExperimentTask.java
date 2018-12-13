package com.westlake.air.pecs.async;

import com.westlake.air.pecs.algorithm.Airus;
import com.westlake.air.pecs.algorithm.FragmentFactory;
import com.westlake.air.pecs.compressor.Compressor;
import com.westlake.air.pecs.constants.TaskStatus;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.SwathParams;
import com.westlake.air.pecs.domain.bean.airus.AirusParams;
import com.westlake.air.pecs.domain.bean.airus.FinalResult;
import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.pecs.domain.bean.analyse.WindowRang;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.*;
import com.westlake.air.pecs.domain.db.simple.MatchedPeptide;
import com.westlake.air.pecs.service.*;
import com.westlake.air.pecs.utils.AirusUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
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
    ScoresService scoresService;
    @Autowired
    Airus airus;
    @Autowired
    Compressor compressor;
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
        compressor.doCompress(experimentDO);
        taskDO.addLog("压缩转换完毕,总耗时:" + (System.currentTimeMillis() - start));
        taskDO.finish(TaskStatus.SUCCESS.getName());
        taskService.update(taskDO);
    }

    @Async(value = "compressFileExecutor")
    public void compressToLMS(ExperimentDO experimentDO, TaskDO taskDO) {
        taskDO.setStatus(TaskStatus.RUNNING.getName());
        taskService.update(taskDO);
        long start = System.currentTimeMillis();
        compressor.doCompressToLMS(experimentDO);
        taskDO.addLog("压缩转换完毕,总耗时:" + (System.currentTimeMillis() - start));
        taskDO.finish(TaskStatus.SUCCESS.getName());
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
    @Async(value = "extractorExecutor")
    public void extract(ExperimentDO experimentDO, String libraryId, SlopeIntercept slopeIntercept, String creator, float rtExtractWindow, float mzExtractWindow, TaskDO taskDO) {
        taskDO.setStatus(TaskStatus.RUNNING.getName());
        taskService.update(taskDO);
        SwathParams input = new SwathParams();
        input.setExperimentDO(experimentDO);
        input.setLibraryId(libraryId);
        input.setSlopeIntercept(slopeIntercept);
        input.setCreator(creator);
        input.setRtExtractWindow(rtExtractWindow);
        input.setMzExtractWindow(mzExtractWindow);

        taskDO.addLog("录入有斜率:" + slopeIntercept.getSlope() + "截距:" + slopeIntercept.getIntercept());
        taskDO.addLog("使用标准库ID:" + libraryId);
        taskDO.addLog("入参准备完毕,开始卷积,时间可能较长");
        taskService.update(taskDO);
        long start = System.currentTimeMillis();
        experimentService.extract(input);

        taskDO.addLog("卷积完毕,总耗时:" + (System.currentTimeMillis() - start));
        logger.info("卷积完毕,总耗时:" + (System.currentTimeMillis() - start));
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

    @Async(value = "extractorExecutor")
    public void swath(SwathParams swathParams, TaskDO taskDO) {
        long startAll = System.currentTimeMillis();
        long start = System.currentTimeMillis();
        ExperimentDO experimentDO = swathParams.getExperimentDO();

        taskDO.addLog("开始创建Aird压缩文件");
        taskDO.setStatus(TaskStatus.RUNNING.getName());
        if(experimentDO.getAirdPath() == null){
            taskService.update(taskDO);
            compressor.doCompress(experimentDO);
            taskDO.addLog("文件压缩完毕,耗时" + (System.currentTimeMillis() - start) + "开始卷积IRT校准库并且计算iRT值");
            taskService.update(taskDO);
        }else{
            taskDO.addLog("已有压缩文件");
            taskService.update(taskDO);
        }

        start = System.currentTimeMillis();

        ResultDO<SlopeIntercept> resultDO = experimentService.convAndIrt(experimentDO, swathParams.getIRtLibraryId(), swathParams.getMzExtractWindow(), swathParams.getSigmaSpacing());
        SlopeIntercept slopeIntercept = resultDO.getModel();

        //此步可以获取iRT的SlopeIntercept
        experimentDO.setSlope(slopeIntercept.getSlope());
        experimentDO.setIntercept(slopeIntercept.getIntercept());
        experimentService.update(experimentDO);

        taskDO.addLog("iRT计算完毕,耗时:" + (System.currentTimeMillis() - start) + "毫秒,斜率:" + slopeIntercept.getSlope() + ",截距:" + slopeIntercept.getIntercept() + ",开始卷积原始数据");
        taskService.update(taskDO);

        start = System.currentTimeMillis();
        //将irt的计算结果加入到下一个步骤的入参中
        swathParams.setSlopeIntercept(slopeIntercept);
        //此步可以获得overviewId,并且存储于swathParams中
        ResultDO<AnalyseOverviewDO> extractResult = experimentService.extract(swathParams);

        if (extractResult.isFailed()) {
            taskDO.addLog("卷积失败:" + extractResult.getMsgInfo());
            taskDO.finish(TaskStatus.FAILED.getName());
            taskService.update(taskDO);
        }

        taskDO.addLog("卷积完毕,耗时:" + (System.currentTimeMillis() - start) + ",开始打分,首先删除原有打分数据");
        taskService.update(taskDO);

        start = System.currentTimeMillis();
        AnalyseOverviewDO overviewDO = extractResult.getModel();

        score(overviewDO.getId(), swathParams, taskDO);


        taskDO.addLog("子分数打分完毕,耗时:" + (System.currentTimeMillis() - start) + ",开始合并打分");
        taskService.update(taskDO);

//        start = System.currentTimeMillis();
//        scoresService.buildScoreDistributions(swathParams.getOverviewId());
//        taskDO.addLog("生成子分数总览图完毕,耗时:" + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        FinalResult finalResult = airus.doAirus(swathParams.getOverviewId(), new AirusParams());

        int matchedPeptideCount = AirusUtil.checkFdr(finalResult);
        taskDO.addLog("合并打分完毕,耗时:" + (System.currentTimeMillis() - start) + ",最终识别的肽段数为" + matchedPeptideCount);
        taskDO.addLog("Swath流程总计耗时:" + (System.currentTimeMillis() - startAll));
        taskDO.finish(TaskStatus.SUCCESS.getName());
        taskService.update(taskDO);
    }

    @Async(value = "extractorExecutor")
    public void swathForReconv(String fatherOverviewId, TaskDO taskDO) {
        long startAll = System.currentTimeMillis();
        long start = System.currentTimeMillis();
        taskDO.addLog("开始执行重卷积");
        taskDO.setStatus(TaskStatus.RUNNING.getName());
        taskService.update(taskDO);

        ResultDO<AnalyseOverviewDO> fatherOverviewResult = analyseOverviewService.getById(fatherOverviewId);
        if (fatherOverviewResult.isFailed()) {
            return;
        }

        AnalyseOverviewDO fatherOverview = fatherOverviewResult.getModel();
        ResultDO<ExperimentDO> experimentResult = experimentService.getById(fatherOverview.getExpId());
        if (experimentResult.isFailed()) {
            return;
        }
        ExperimentDO exp = experimentResult.getModel();
        String fatherLibraryId = fatherOverview.getLibraryId();
        //Step1.搜索所有已经识别的肽段并且从标准库中去除这些已经识别的肽段
        List<MatchedPeptide> mps = scoresService.getAllMatchedPeptides(fatherOverviewId);
        List<String> matchedNames = new ArrayList<>();
        for (MatchedPeptide mp : mps) {
            matchedNames.add(mp.getPeptideRef());
        }

        List<PeptideDO> peptides = peptideService.getAllByLibraryId(fatherLibraryId);

        //TODO 这里最好需要再做一层基于卷积结果的过滤,如果父实验中完全没有卷积到信号的肽段可以不用考虑,以节省二次实验的成本
        //为剩余的未鉴定出的蛋白生成更为复杂的离子情况
        List<PeptideDO> nextPeptides = new ArrayList<>();
        for (PeptideDO peptide : peptides) {
            if (!matchedNames.contains(peptide.getPeptideRef())) {
                PeptideDO peptideDO = peptideService.getByLibraryIdAndPeptideRefAndIsDecoy("5c0a3669fc6f9e1d441ae71f", peptide.getPeptideRef(), peptide.getIsDecoy());
                if (peptideDO == null) {
                    HashMap<String, Double> bySeriesMap = fragmentFactory.getBYSeriesMap(peptide, 4);
                    for (String cutInfo : bySeriesMap.keySet()) {
                        if (peptide.getFragmentMap().get(cutInfo) == null) {
                            peptide.getFragmentMap().put(cutInfo, new FragmentInfo(cutInfo, bySeriesMap.get(cutInfo), 0d, peptide.getCharge()));
                        }
                    }
                    nextPeptides.add(peptide);
                } else {
                    peptideDO.setRt(peptide.getRt());
                    peptide.setFeatures("From Human Library");
                    nextPeptides.add(peptideDO);
                }
            } else {
                nextPeptides.add(peptide);
            }
        }

        LibraryDO tempLib = new LibraryDO();
        tempLib.setType(LibraryDO.TYPE_STANDARD);
        String libraryName = "FatherId:" + fatherLibraryId + "--" + System.currentTimeMillis();
        tempLib.setName(libraryName);
        tempLib.setFatherId(fatherLibraryId);
        libraryService.insert(tempLib);
        for (PeptideDO peptideDO : nextPeptides) {
            peptideDO.setLibraryId(tempLib.getId());
            peptideDO.setLibraryName(libraryName);
            peptideDO.setId(null);
        }
        peptideService.insertAll(nextPeptides, false);

        SwathParams sp = new SwathParams();
        sp.setRtExtractWindow(fatherOverview.getRtExtractWindow());
        sp.setMzExtractWindow(fatherOverview.getMzExtractWindow());
        sp.setSlopeIntercept(new SlopeIntercept(fatherOverview.getSlope(), fatherOverview.getIntercept()));
        sp.setExperimentDO(exp);
        sp.setLibraryId(tempLib.getId());

        //此步可以获得overviewId,并且存储于swathParams中
        ResultDO<AnalyseOverviewDO> extractResult = experimentService.extract(sp);

        if (extractResult.isFailed()) {
            taskDO.addLog("卷积失败:" + extractResult.getMsgInfo());
            taskDO.finish(TaskStatus.FAILED.getName());
            taskService.update(taskDO);
        }

        taskDO.addLog("卷积完毕,耗时:" + (System.currentTimeMillis() - start) + ",开始打分,首先删除原有打分数据");
        taskService.update(taskDO);

        start = System.currentTimeMillis();
        AnalyseOverviewDO overviewDO = extractResult.getModel();

        score(overviewDO.getId(), sp, taskDO);
        taskDO.addLog("子分数打分完毕,耗时:" + (System.currentTimeMillis() - start));
        taskService.update(taskDO);
        start = System.currentTimeMillis();
        FinalResult finalResult = airus.doAirus(sp.getOverviewId(), new AirusParams());

        int matchedPeptideCount = AirusUtil.checkFdr(finalResult);
        taskDO.addLog("合并打分完毕,耗时:" + (System.currentTimeMillis() - start) + ",最终识别的肽段数为" + matchedPeptideCount);
        taskDO.addLog("Swath流程总计耗时:" + (System.currentTimeMillis() - startAll));
        taskDO.finish(TaskStatus.SUCCESS.getName());
        taskService.update(taskDO);
    }
}
