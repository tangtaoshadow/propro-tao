package com.westlake.air.pecs.controller;

import com.westlake.air.pecs.constants.TaskStatus;
import com.westlake.air.pecs.dao.ConfigDAO;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.*;
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
import org.springframework.data.domain.Sort;
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
public class HomeController extends BaseController{

    private Logger logger = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    LibraryService libraryService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    TaskService taskService;
    @Autowired
    ConfigDAO configDAO;

    public static int SHOW_NUM = 5;

    @RequestMapping("/")
    String home(Model model) {
        LibraryQuery libraryQuery = new LibraryQuery(1, SHOW_NUM, Sort.Direction.DESC, "createDate");
        ResultDO<List<LibraryDO>> libRes = libraryService.getList(libraryQuery);
        ExperimentQuery experimentQuery = new ExperimentQuery(1, SHOW_NUM, Sort.Direction.DESC, "createDate");
        ResultDO<List<ExperimentDO>> expRes = experimentService.getList(experimentQuery);
        AnalyseOverviewQuery overviewQuery = new AnalyseOverviewQuery(1, SHOW_NUM, Sort.Direction.DESC, "createDate");
        ResultDO<List<AnalyseOverviewDO>> overviewRes = analyseOverviewService.getList(overviewQuery);

        TaskQuery query = new TaskQuery(1, SHOW_NUM, Sort.Direction.DESC, "createDate");
        ResultDO<List<TaskDO>> taskTotalRes = taskService.getList(query);
        query.setStatus(TaskStatus.RUNNING.getName());
        ResultDO<List<TaskDO>> taskRunningRes = taskService.getList(query);
        ConfigDO configDO = configDAO.getConfig();

        List<LibraryDO> slist = getLibraryList(0);
        List<LibraryDO> iRtlist = getLibraryList(1);
        model.addAttribute("libraries", slist);
        model.addAttribute("iRtLibraries", iRtlist);

        model.addAttribute("taskRunningCount", taskRunningRes.getTotalNum());
        model.addAttribute("taskTotalCount", taskTotalRes.getTotalNum());
        model.addAttribute("libCount", libRes.getTotalNum());
        model.addAttribute("expCount", expRes.getTotalNum());
        model.addAttribute("overviewCount", overviewRes.getTotalNum());
        model.addAttribute("runningTasks", taskRunningRes.getModel());
        model.addAttribute("tasks", taskTotalRes.getModel());
        model.addAttribute("libs", libRes.getModel());
        model.addAttribute("exps", expRes.getModel());
        model.addAttribute("overviews", overviewRes.getModel());
        model.addAttribute("config", configDO);
        return "/home";
    }

}
