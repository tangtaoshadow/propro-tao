package com.westlake.air.pecs.async;

import com.westlake.air.pecs.algorithm.Airus;
import com.westlake.air.pecs.algorithm.FragmentFactory;
import com.westlake.air.pecs.compressor.Compressor;
import com.westlake.air.pecs.constants.TaskStatus;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.params.LumsParams;
import com.westlake.air.pecs.domain.bean.airus.AirusParams;
import com.westlake.air.pecs.domain.bean.airus.FinalResult;
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

@Component("lumsTask")
public class LumsTask extends BaseTask{

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

    @Async(value = "extractorExecutor")
    public void lums(LumsParams lumsParams, TaskDO taskDO){
        taskDO.addLog("开始卷积,选峰和打分");
        taskDO.setStatus(TaskStatus.RUNNING.getName());
        lumsParams.setUseEpps(true);
        experimentService.extract(lumsParams);
    }

    @Async(value = "extractorExecutor")
    public void swath(LumsParams lumsParams, TaskDO taskDO) {
        long startAll = System.currentTimeMillis();
        long start = System.currentTimeMillis();
        ExperimentDO experimentDO = lumsParams.getExperimentDO();

        taskDO.addLog("开始创建Aird压缩文件");
        taskDO.setStatus(TaskStatus.RUNNING.getName());
        if(experimentDO.getAirdPath() == null || !(new File(experimentDO.getAirdPath()).exists())){
            taskService.update(taskDO);
            compressor.doCompress(experimentDO);
            taskDO.addLog("文件压缩完毕,耗时" + (System.currentTimeMillis() - start) + "开始卷积IRT校准库并且计算iRT值");
            taskService.update(taskDO);
        }else{
            taskDO.addLog("已有压缩文件");
            taskService.update(taskDO);
        }

        start = System.currentTimeMillis();

        ResultDO<SlopeIntercept> resultDO = experimentService.convAndIrt(experimentDO, lumsParams.getIRtLibraryId(), lumsParams.getMzExtractWindow(), lumsParams.getSigmaSpacing());
        SlopeIntercept slopeIntercept = resultDO.getModel();

        //此步可以获取iRT的SlopeIntercept
        experimentDO.setSlope(slopeIntercept.getSlope());
        experimentDO.setIntercept(slopeIntercept.getIntercept());
        experimentService.update(experimentDO);

        taskDO.addLog("iRT计算完毕,耗时:" + (System.currentTimeMillis() - start) + "毫秒,斜率:" + slopeIntercept.getSlope() + ",截距:" + slopeIntercept.getIntercept() + ",开始卷积原始数据");
        taskService.update(taskDO);

        start = System.currentTimeMillis();
        //将irt的计算结果加入到下一个步骤的入参中
        lumsParams.setSlopeIntercept(slopeIntercept);
        //此步可以获得overviewId,并且存储于swathParams中
        ResultDO<AnalyseOverviewDO> extractResult = experimentService.extract(lumsParams);

        if (extractResult.isFailed()) {
            taskDO.addLog("卷积失败:" + extractResult.getMsgInfo());
            taskDO.finish(TaskStatus.FAILED.getName());
            taskService.update(taskDO);
        }

        taskDO.addLog("卷积完毕,耗时:" + (System.currentTimeMillis() - start) + ",开始打分,首先删除原有打分数据");
        taskService.update(taskDO);

        start = System.currentTimeMillis();
        AnalyseOverviewDO overviewDO = extractResult.getModel();

        score(overviewDO.getId(), lumsParams, taskDO);

        taskDO.addLog("子分数打分完毕,耗时:" + (System.currentTimeMillis() - start) + ",开始合并打分");
        taskService.update(taskDO);

        start = System.currentTimeMillis();
        FinalResult finalResult = airus.doAirus(lumsParams.getOverviewId(), new AirusParams());

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
                //人体蛋白质库
                PeptideDO peptideDO = peptideService.getByLibraryIdAndPeptideRefAndIsDecoy("5c0a3669fc6f9e1d441ae71f", peptide.getPeptideRef(), peptide.getIsDecoy());
                if (peptideDO == null) {
                    HashMap<String, Double> bySeriesMap = fragmentFactory.getBYSeriesMap(peptide, 4, false, false);
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

        LumsParams sp = new LumsParams();
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
