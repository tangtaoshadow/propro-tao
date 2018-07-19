package com.westlake.air.swathplatform.controller;

import com.westlake.air.swathplatform.domain.ResultDO;
import com.westlake.air.swathplatform.domain.db.AnalyseDataDO;
import com.westlake.air.swathplatform.domain.db.AnalyseOverviewDO;
import com.westlake.air.swathplatform.domain.db.ExperimentDO;
import com.westlake.air.swathplatform.domain.query.AnalyseDataQuery;
import com.westlake.air.swathplatform.domain.query.AnalyseOverviewQuery;
import com.westlake.air.swathplatform.domain.query.ExperimentQuery;
import com.westlake.air.swathplatform.service.AnalyseDataService;
import com.westlake.air.swathplatform.service.AnalyseOverviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 16:50
 */
@Controller
@RequestMapping("analyse")
public class AnalyseController extends BaseController {

    @Autowired
    AnalyseOverviewService analyseOverviewService;

    @Autowired
    AnalyseDataService analyseDataService;

    @RequestMapping(value = "/overview/list")
    String overviewList(Model model,
                  @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                  @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {

        model.addAttribute("pageSize", pageSize);
        AnalyseOverviewQuery query = new AnalyseOverviewQuery();
        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        ResultDO<List<AnalyseOverviewDO>> resultDO = analyseOverviewService.getList(query);

        model.addAttribute("overviews", resultDO.getModel());
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);

        return "/analyse/overview/list";
    }

    @RequestMapping(value = "/data/list")
    String dataList(Model model,
                        @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                        @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {

        model.addAttribute("pageSize", pageSize);

        AnalyseDataQuery query = new AnalyseDataQuery();
        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        ResultDO<List<AnalyseDataDO>> resultDO = analyseDataService.getList(query);

        model.addAttribute("datas", resultDO.getModel());
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);

        return "/analyse/data/list";
    }
}
