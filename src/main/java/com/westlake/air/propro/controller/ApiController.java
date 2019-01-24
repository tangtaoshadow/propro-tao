package com.westlake.air.propro.controller;

import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.ExperimentDO;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.query.ExperimentQuery;
import com.westlake.air.propro.domain.query.LibraryQuery;
import com.westlake.air.propro.service.ExperimentService;
import com.westlake.air.propro.service.LibraryService;
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
    @Autowired
    LibraryService libraryService;

    /**
     * 根据ID获取实验对象
     * @param model
     * @param id
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "experiment/getById")
    public ResultDO<ExperimentDO> getExperimentById(Model model,
                                         @RequestParam(value = "id", required = true) String id) {
        ResultDO<ExperimentDO> resultDO = experimentService.getById(id);
        return resultDO;
    }

    /**
     * 根据条件批量分页获取实验列表
     * @param model
     * @param currentPage
     * @param pageSize
     * @param projectName
     * @param batchName
     * @param expName
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "experiment/getList")
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

    /**
     * 根据ID获取标准库对象(包含iRT库)
     * @param model
     * @param id
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "library/getById")
    public ResultDO<LibraryDO> getLibraryById(Model model,
                                              @RequestParam(value = "id", required = true) String id) {
        ResultDO<LibraryDO> resultDO = libraryService.getById(id);
        return resultDO;
    }

    /**
     * 根据ID获取标准库对象(包含iRT库)
     * @param model
     * @param currentPage
     * @param pageSize
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "library/getList")
    public ResultDO<List<LibraryDO>> getLibraryById(Model model,
                                                    @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                                                    @RequestParam(value = "pageSize", required = false, defaultValue = "50") Integer pageSize) {
        LibraryQuery libraryQuery = new LibraryQuery();
        ResultDO<List<LibraryDO>> resultDO = libraryService.getList(libraryQuery);
        return resultDO;
    }
}
