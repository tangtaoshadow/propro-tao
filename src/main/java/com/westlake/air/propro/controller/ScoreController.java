package com.westlake.air.propro.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.westlake.air.propro.async.AirusTask;
import com.westlake.air.propro.async.ScoreTask;
import com.westlake.air.propro.constants.Classifier;
import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.constants.ScoreType;
import com.westlake.air.propro.constants.TaskTemplate;
import com.westlake.air.propro.dao.ConfigDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.airus.AirusParams;
import com.westlake.air.propro.domain.db.AnalyseDataDO;
import com.westlake.air.propro.domain.db.AnalyseOverviewDO;
import com.westlake.air.propro.domain.db.TaskDO;
import com.westlake.air.propro.domain.query.AnalyseDataQuery;
import com.westlake.air.propro.service.AnalyseDataService;
import com.westlake.air.propro.service.AnalyseOverviewService;
import com.westlake.air.propro.service.ScoreService;
import com.westlake.air.propro.utils.AnalyseDataUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-14 16:02
 */
@Controller
@RequestMapping("/score")
public class ScoreController extends BaseController {

    @Autowired
    ScoreService scoreService;
    @Autowired
    ConfigDAO configDAO;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    ScoreTask scoreTask;
    @Autowired
    AirusTask airusTask;

    @RequestMapping(value = "/list")
    String list(Model model,
                @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
                @RequestParam(value = "overviewId", required = false) String overviewId,
                @RequestParam(value = "peptideRef", required = false) String peptideRef,
                @RequestParam(value = "fdrStart", required = false) Double fdrStart,
                @RequestParam(value = "fdrEnd", required = false) Double fdrEnd,
                @RequestParam(value = "isIdentified", required = false) String isIdentified,
                @RequestParam(value = "isDecoy", required = false) String isDecoy,
                RedirectAttributes redirectAttributes) {
        model.addAttribute("overviewId", overviewId);
        model.addAttribute("peptideRef", peptideRef);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("fdrStart", fdrStart);
        model.addAttribute("fdrEnd", fdrEnd);
        model.addAttribute("isIdentified", isIdentified);
        model.addAttribute("isDecoy", isDecoy);
        AnalyseDataQuery query = new AnalyseDataQuery();
        if (peptideRef != null && !peptideRef.isEmpty()) {
            query.setPeptideRef(peptideRef);
        }
        if (isIdentified != null && isIdentified.equals("Yes")) {
            query.addIndentifiedStatus(AnalyseDataDO.IDENTIFIED_STATUS_SUCCESS);
        } else if (isIdentified != null && isIdentified.equals("No")) {
            query.addIndentifiedStatus(AnalyseDataDO.IDENTIFIED_STATUS_NO_FIT);
            query.addIndentifiedStatus(AnalyseDataDO.IDENTIFIED_STATUS_UNKNOWN);
        }
        if (isDecoy != null && isDecoy.equals("Yes")) {
            query.setIsDecoy(true);
        } else if (isDecoy != null && isDecoy.equals("No")) {
            query.setIsDecoy(false);
        }
        if (fdrStart != null) {
            query.setFdrStart(fdrStart);
        }
        if (fdrEnd != null) {
            query.setFdrEnd(fdrEnd);
        }
        if (overviewId == null) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.ANALYSE_OVERVIEW_ID_CAN_NOT_BE_EMPTY.getMessage());
            return "redirect:/analyse/overview/list";
        }
        query.setOverviewId(overviewId);
        query.setPageSize(30);
        query.setPageNo(currentPage);
        query.setSortColumn("fdr");
        ResultDO<List<AnalyseDataDO>> resultDO = analyseDataService.getList(query);

        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);
        if (overviewResult.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.ANALYSE_OVERVIEW_NOT_EXISTED.getMessage());
            return "redirect:/analyse/overview/list";
        }
        model.addAttribute("overview", overviewResult.getModel());
        model.addAttribute("scores", resultDO.getModel());
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalNum", resultDO.getTotalNum());
        return "scores/list";
    }
    @RequestMapping(value = "/result/list")
    String resultList(Model model,
                @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                @RequestParam(value = "pageSize", required = false, defaultValue = "30") Integer pageSize,
                @RequestParam(value = "overviewId", required = true) String overviewId,
                @RequestParam(value = "peptideRef", required = false) String peptideRef,
                @RequestParam(value = "proteinName", required = false) String proteinName,
                @RequestParam(value = "isIdentified", required = false) String isIdentified,
                RedirectAttributes redirectAttributes) {

        model.addAttribute("overviewId", overviewId);
        model.addAttribute("proteinName", proteinName);
        model.addAttribute("peptideRef", peptideRef);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("isIdentified", isIdentified);
        AnalyseDataQuery query = new AnalyseDataQuery();
        query.setIsDecoy(false);
        if (peptideRef != null && !peptideRef.isEmpty()) {
            query.setPeptideRef(peptideRef);
        }
        if (proteinName != null && !proteinName.isEmpty()) {
            query.setProteinName(proteinName);
        }
        if (isIdentified != null && isIdentified.equals("Yes")) {
            query.addIndentifiedStatus(AnalyseDataDO.IDENTIFIED_STATUS_SUCCESS);
        } else if (isIdentified != null && isIdentified.equals("No")) {
            query.addIndentifiedStatus(AnalyseDataDO.IDENTIFIED_STATUS_NO_FIT);
            query.addIndentifiedStatus(AnalyseDataDO.IDENTIFIED_STATUS_UNKNOWN);
        }
        query.setOverviewId(overviewId);
        query.setPageSize(pageSize);
        query.setPageNo(currentPage);

        ResultDO<List<AnalyseDataDO>> resultDO = analyseDataService.getList(query);
        if (peptideRef != null){
            query.setPeptideRef(null);
            query.setProteinName(resultDO.getModel().get(0).getProteinName());
            resultDO = analyseDataService.getList(query);
        }
        List<AnalyseDataDO> dataDOList = resultDO.getModel();
        HashMap<String, List<AnalyseDataDO>> proteinMap = new HashMap<>();
        for (AnalyseDataDO dataDO: dataDOList){
            if (proteinMap.containsKey(dataDO.getProteinName())){
                proteinMap.get(dataDO.getProteinName()).add(dataDO);
            }else {
                List<AnalyseDataDO> newList = new ArrayList<>();
                newList.add(dataDO);
                proteinMap.put(dataDO.getProteinName(), newList);
            }
        }
//        JSONObject protMap = new JSONObject();
//        for (String protName: proteinMap.keySet()){
//            JSONArray peptideArray = new JSONArray();
//            Collections.addAll(peptideArray, proteinMap.get(protName));
//            protMap.put(protName, peptideArray);
//        }
        model.addAttribute("protMap", proteinMap);
        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);
        model.addAttribute("overview", overviewResult.getModel());
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalNum", resultDO.getTotalNum());
        return "scores/result/list";
    }

    @RequestMapping(value = "/report")
    String report(Model model,
                @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                @RequestParam(value = "pageSize", required = false, defaultValue = "100") Integer pageSize,
                @RequestParam(value = "overviewId", required = false) String overviewId,
                @RequestParam(value = "peptideRef", required = false) String peptideRef,
                @RequestParam(value = "fdrStart", required = false) Double fdrStart,
                @RequestParam(value = "fdrEnd", required = false) Double fdrEnd,
                RedirectAttributes redirectAttributes) {
        model.addAttribute("overviewId", overviewId);
        model.addAttribute("peptideRef", peptideRef);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("fdrStart", fdrStart);
        model.addAttribute("fdrEnd", fdrEnd);
        AnalyseDataQuery query = new AnalyseDataQuery();
        if (peptideRef != null && !peptideRef.isEmpty()) {
            query.setPeptideRef(peptideRef);
        }
        if (fdrStart != null) {
            query.setFdrStart(fdrStart);
        }
        if (fdrEnd != null) {
            query.setFdrEnd(fdrEnd);
        }
        query.setIsDecoy(false);
        if (overviewId == null) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.ANALYSE_OVERVIEW_ID_CAN_NOT_BE_EMPTY.getMessage());
            return "redirect:/analyse/overview/list";
        }
        query.setOverviewId(overviewId);
        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        ResultDO<List<AnalyseDataDO>> resultDO = analyseDataService.getListWithConvolutionData(query);

        for(AnalyseDataDO data : resultDO.getModel()){
            AnalyseDataUtil.decompress(data);

        }
        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);
        if (overviewResult.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.ANALYSE_OVERVIEW_NOT_EXISTED.getMessage());
            return "redirect:/analyse/overview/list";
        }
        model.addAttribute("overview", overviewResult.getModel());
        model.addAttribute("dataList", resultDO.getModel());
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalNum", resultDO.getTotalNum());
        return "scores/list";
    }

    @RequestMapping(value = "/detail")
    String detail(Model model, @RequestParam(value = "overviewId", required = true) String overviewId, RedirectAttributes redirectAttributes) {

        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);
        if (overviewResult.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.ANALYSE_OVERVIEW_NOT_EXISTED.getMessage());
            return "redirect:/analyse/overview/list";
        }
        model.addAttribute("scoreTypes", ScoreType.getUsedTypes());
        model.addAttribute("scoreTypeArray", JSONArray.parseArray(JSON.toJSONString(ScoreType.getUsedTypes())));
        model.addAttribute("overview", overviewResult.getModel());
        return "scores/detail";
    }

    @RequestMapping(value = "/airus")
    String airus(Model model,
                 @RequestParam(value = "overviewId", required = true) String overviewId,
                 @RequestParam(value = "classifier", required = true) String classifier,
                 RedirectAttributes redirectAttributes) {

        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);
        if (overviewResult.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.ANALYSE_OVERVIEW_NOT_EXISTED.getMessage());
            return "redirect:/analyse/overview/list";
        }
        TaskDO taskDO = new TaskDO(TaskTemplate.AIRUS, overviewResult.getModel().getName() + "(" + overviewResult.getModel().getId() + ")-classifier:" + classifier);
        taskService.insert(taskDO);
        AirusParams airusParams = new AirusParams();
        if (classifier.equals("xgboost")) {
            airusParams.setClassifier(Classifier.xgboost);
        } else {
            airusParams.setClassifier(Classifier.lda);
        }

        airusTask.airus(overviewId, airusParams, taskDO);

        return "redirect:/task/detail/" + taskDO.getId();
    }
}
