package com.westlake.air.pecs.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.westlake.air.pecs.async.AirusTask;
import com.westlake.air.pecs.async.ScoreTask;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.constants.TaskTemplate;
import com.westlake.air.pecs.dao.ConfigDAO;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.airus.AirusParams;
import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import com.westlake.air.pecs.domain.db.AnalyseOverviewDO;
import com.westlake.air.pecs.domain.db.ScoreDistribution;
import com.westlake.air.pecs.domain.db.ScoresDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.domain.query.ScoresQuery;
import com.westlake.air.pecs.service.AnalyseOverviewService;
import com.westlake.air.pecs.service.ScoresService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-14 16:02
 */
@Controller
@RequestMapping("/scores")
public class ScoresController extends BaseController {

    @Autowired
    ScoresService scoresService;
    @Autowired
    ConfigDAO configDAO;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
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
                RedirectAttributes redirectAttributes) {
        model.addAttribute("overviewId", overviewId);
        model.addAttribute("peptideRef", peptideRef);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("fdrStart", fdrStart);
        model.addAttribute("fdrEnd", fdrEnd);
        model.addAttribute("isIdentified", isIdentified);
        ScoresQuery query = new ScoresQuery();
        if (peptideRef != null && !peptideRef.isEmpty()) {
            query.setPeptideRef(peptideRef);
        }
        if (isIdentified != null && isIdentified.equals("Yes")) {
            query.setIsIdentified(true);
            query.setIsDecoy(false);
        } else if (isIdentified != null && isIdentified.equals("No")) {
            query.setIsIdentified(false);
            query.setIsDecoy(false);
        }
        if(fdrStart != null){
            query.setFdrStart(fdrStart);
        }
        if(fdrEnd != null){
            query.setFdrEnd(fdrEnd);
        }
        if (overviewId == null) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.ANALYSE_OVERVIEW_ID_CAN_NOT_BE_EMPTY.getMessage());
            return "redirect:/analyse/overview/list";
        }
        query.setOverviewId(overviewId);
        query.setPageSize(30);
        query.setPageNo(currentPage);
        query.setIsDecoy(false);
        query.setSortColumn("fdr");
        ResultDO<List<ScoresDO>> resultDO = scoresService.getList(query);

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

    @RequestMapping(value = "/buildDistribution")
    String buildDistribution(Model model, @RequestParam(value = "overviewId", required = true) String overviewId, RedirectAttributes redirectAttributes) {
        model.addAttribute("overviewId", overviewId);
        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);
        if (overviewResult.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.ANALYSE_OVERVIEW_NOT_EXISTED.getMessage());
            return "redirect:/analyse/overview/list";
        }
        AnalyseOverviewDO overviewDO = overviewResult.getModel();

        TaskDO taskDO = TaskDO.create(TaskTemplate.BUILD_SCORE_DISTRIBUTE, overviewDO.getExpName() + "-" + overviewId);
        taskService.insert(taskDO);
        scoreTask.buildScoreDistributions(overviewId, taskDO);
        return "redirect:/task/detail/" + taskDO.getId();
    }

    @RequestMapping(value = "/detail")
    String detail(Model model, @RequestParam(value = "overviewId", required = true) String overviewId, RedirectAttributes redirectAttributes) {

        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);
        if (overviewResult.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.ANALYSE_OVERVIEW_NOT_EXISTED.getMessage());
            return "redirect:/analyse/overview/list";
        }
        model.addAttribute("scoreTypes", FeatureScores.ScoreType.getUsedTypes());
        model.addAttribute("scoreTypeArray", JSONArray.parseArray(JSON.toJSONString(FeatureScores.ScoreType.getUsedTypes())));
        model.addAttribute("overview", overviewResult.getModel());
        return "scores/detail";
    }

    @RequestMapping(value = "/distributions")
    @ResponseBody
    ResultDO<JSONArray> getDistributions(Model model, @RequestParam(value = "overviewId", required = false) String overviewId) {
        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);
        if (overviewResult.isFailed()) {
            ResultDO resultDO = new ResultDO();
            resultDO.setErrorResult(overviewResult.getMsgCode(), overviewResult.getMsgInfo());
            return resultDO;
        }
        AnalyseOverviewDO overviewDO = overviewResult.getModel();

        if (overviewDO.getScoreDistributions() == null || overviewDO.getScoreDistributions().size() == 0) {
            return ResultDO.buildError(ResultCode.SCORE_DISTRIBUTION_NOT_GENERATED_YET);
        }

        List<ScoreDistribution> distributions = overviewDO.getScoreDistributions();

        ResultDO<JSONArray> resultDO = new ResultDO(true);
        JSONArray array = JSON.parseArray(JSON.toJSONString(distributions));

        resultDO.setModel(array);
        return resultDO;
    }

    @RequestMapping(value = "/export/{overviewId}")
    String export(Model model, @PathVariable("overviewId") String overviewId) {

        TaskDO taskDO = new TaskDO(TaskTemplate.EXPORT_SUBSCORES_TSV_FILE_FOR_PYPROPHET, "OverviewId" + ":" + overviewId);
        taskService.insert(taskDO);
        scoreTask.exportForPyProphet(overviewId, taskDO);

        return "redirect:/task/detail/" + taskDO.getId();
    }

    @RequestMapping(value = "/airus/{overviewId}")
    String airus(Model model, @PathVariable("overviewId") String overviewId) {

        TaskDO taskDO = new TaskDO(TaskTemplate.AIRUS, overviewId);
        taskService.insert(taskDO);

        AirusParams airusParams = new AirusParams();
        airusParams.setMainScore(FeatureScores.ScoreType.MainScore.getTypeName());
        airusTask.airus(overviewId, airusParams, taskDO);

        return "redirect:/task/detail/" + taskDO.getId();
    }
}
