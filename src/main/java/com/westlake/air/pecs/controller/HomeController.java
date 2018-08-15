package com.westlake.air.pecs.controller;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.AnalyseOverviewDO;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.domain.query.AnalyseOverviewQuery;
import com.westlake.air.pecs.domain.query.ExperimentQuery;
import com.westlake.air.pecs.domain.query.LibraryQuery;
import com.westlake.air.pecs.domain.query.TaskQuery;
import com.westlake.air.pecs.service.AnalyseOverviewService;
import com.westlake.air.pecs.service.ExperimentService;
import com.westlake.air.pecs.service.LibraryService;
import com.westlake.air.pecs.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-05-31 09:53
 */
@Controller
@RequestMapping("/")
public class HomeController {

    private Logger logger = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    LibraryService libraryService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    TaskService taskService;

    @RequestMapping("/")
    String home(Model model) {
        ResultDO<List<LibraryDO>> libRes = libraryService.getList(new LibraryQuery(1,5));
        ResultDO<List<ExperimentDO>> expRes = experimentService.getList(new ExperimentQuery(1,5));
        ResultDO<List<AnalyseOverviewDO>> overviewRes = analyseOverviewService.getList(new AnalyseOverviewQuery(1,5));
        ResultDO<List<TaskDO>> taskRes = taskService.getList(new TaskQuery(1,5));

        model.addAttribute("taskCount", taskRes.getTotalNum());
        model.addAttribute("libCount", libRes.getTotalNum());
        model.addAttribute("expCount", expRes.getTotalNum());
        model.addAttribute("overviewCount", overviewRes.getTotalNum());
        model.addAttribute("tasks", taskRes.getModel());
        model.addAttribute("libraries", libRes.getModel());
        model.addAttribute("experiments", expRes.getModel());
        model.addAttribute("overviews", overviewRes.getModel());
        return "/home";
    }


}
