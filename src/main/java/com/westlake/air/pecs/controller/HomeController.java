package com.westlake.air.pecs.controller;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.AnalyseOverviewDO;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.domain.query.AnalyseOverviewQuery;
import com.westlake.air.pecs.domain.query.ExperimentQuery;
import com.westlake.air.pecs.domain.query.LibraryQuery;
import com.westlake.air.pecs.service.AnalyseOverviewService;
import com.westlake.air.pecs.service.ExperimentService;
import com.westlake.air.pecs.service.LibraryService;
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

    @RequestMapping("/")
    String home(Model model) {
        ResultDO<List<LibraryDO>> libraries = libraryService.getList(new LibraryQuery());
        ResultDO<List<ExperimentDO>> experiments = experimentService.getList(new ExperimentQuery());
        ResultDO<List<AnalyseOverviewDO>> overviews = analyseOverviewService.getList(new AnalyseOverviewQuery());

        model.addAttribute("libraries",libraries);
        model.addAttribute("experiments",experiments);
        model.addAttribute("overviews",overviews);
        return "home";
    }


}
