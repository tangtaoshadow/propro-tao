package com.westlake.air.pecs.controller;

import com.westlake.air.pecs.constants.SuccessMsg;
import com.westlake.air.pecs.dao.ConfigDAO;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.ConfigDO;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.ScoresDO;
import com.westlake.air.pecs.domain.query.ExperimentQuery;
import com.westlake.air.pecs.domain.query.ScoresQuery;
import com.westlake.air.pecs.service.ScoresService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
}
