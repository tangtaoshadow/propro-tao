package com.westlake.air.pecs.controller;

import com.westlake.air.pecs.async.ScoreTask;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.constants.TaskTemplate;
import com.westlake.air.pecs.dao.ConfigDAO;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.AnalyseOverviewDO;
import com.westlake.air.pecs.domain.db.ConfigDO;
import com.westlake.air.pecs.domain.db.ScoresDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.domain.query.ScoresQuery;
import com.westlake.air.pecs.service.AnalyseOverviewService;
import com.westlake.air.pecs.service.ScoresService;
import com.westlake.air.pecs.utils.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
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

    @RequestMapping(value = "/list")
    String list(Model model,
                @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize,
                @RequestParam(value = "overviewId", required = true) String overviewId,
                @RequestParam(value = "peptideRef", required = false) String peptideRef) {
        model.addAttribute("overviewId", overviewId);
        model.addAttribute("peptideRef", peptideRef);
        model.addAttribute("pageSize", pageSize);
        ScoresQuery query = new ScoresQuery();
        if (peptideRef != null && !peptideRef.isEmpty()) {
            query.setPeptideRef(peptideRef);
        }
        query.setOverviewId(overviewId);
        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        ResultDO<List<ScoresDO>> resultDO = scoresService.getList(query);

        model.addAttribute("scores", resultDO.getModel());
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalNum", resultDO.getTotalNum());
        return "scores/list";
    }

    @RequestMapping(value = "/detail")
    String detail(Model model,
                @RequestParam(value = "overviewId", required = true) String overviewId,
                @RequestParam(value = "scoreType", required = true) String scoreType) {
        model.addAttribute("overviewId", overviewId);
        model.addAttribute("scoreType", scoreType);

        List<ScoresDO> scores = scoresService.getAllByOverviewId(overviewId);

        model.addAttribute("scores", scores);

        return "scores/detail";
    }

    @RequestMapping(value = "/export/{overviewId}")
    String export(Model model, @PathVariable("overviewId") String overviewId) {

        TaskDO taskDO = new TaskDO(TaskTemplate.EXPORT_SUBSCORES_TSV_FILE_FOR_PYPROPHET, "OverviewId" + ":" + overviewId);
        taskService.insert(taskDO);
        scoreTask.exportForPyProphet(overviewId, taskDO);

        return "redirect:/task/detail/" + taskDO.getId();
    }
}
