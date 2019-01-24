package com.westlake.air.propro.controller;

import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.ExperimentDO;
import com.westlake.air.propro.domain.query.ExperimentQuery;
import com.westlake.air.propro.service.ExperimentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping(value = "api")
public class ApiController {

    @Autowired
    ExperimentService experimentService;

    @ResponseBody
    @RequestMapping(value = "getExperimentById")
    public ResultDO<ExperimentDO> getExperimentById(Model model,
                                         @RequestParam(value = "id", required = true) String id) {
        ResultDO<ExperimentDO> resultDO = experimentService.getById(id);
        return resultDO;
    }

    @ResponseBody
    @RequestMapping(value = "getExperimentList")
    public ResultDO<List<ExperimentDO>> getExperimentList(Model model,
                    @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                    @RequestParam(value = "pageSize", required = false, defaultValue = "50") Integer pageSize,
                    @RequestParam(value = "projectName", required = false) String projectName,
                    @RequestParam(value = "batchName", required = false) String batchName,
                    @RequestParam(value = "expName", required = false) String expName) {
        ExperimentQuery query = new ExperimentQuery();
        if (expName != null && !expName.isEmpty()) {
            query.setName(expName);
        }
        if(projectName != null && !projectName.isEmpty()){
            query.setProjectName(projectName);
        }
        if(batchName != null && !batchName.isEmpty()){
            query.setBatchName(batchName);
        }
        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        ResultDO<List<ExperimentDO>> resultDO = experimentService.getList(query);

        return resultDO;
    }
}
