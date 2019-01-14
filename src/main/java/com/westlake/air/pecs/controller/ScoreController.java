package com.westlake.air.pecs.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.westlake.air.pecs.algorithm.learner.Learner;
import com.westlake.air.pecs.async.AirusTask;
import com.westlake.air.pecs.async.ScoreTask;
import com.westlake.air.pecs.constants.Classifier;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.constants.ScoreType;
import com.westlake.air.pecs.constants.TaskTemplate;
import com.westlake.air.pecs.dao.ConfigDAO;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.airus.AirusParams;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.AnalyseOverviewDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.domain.query.AnalyseDataQuery;
import com.westlake.air.pecs.service.AnalyseDataService;
import com.westlake.air.pecs.service.AnalyseOverviewService;
import com.westlake.air.pecs.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
