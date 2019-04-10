package com.westlake.air.propro.controller;

import com.westlake.air.propro.async.ExperimentTask;
import com.westlake.air.propro.domain.db.ExperimentDO;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.query.PageQuery;
import com.westlake.air.propro.service.ExperimentService;
import com.westlake.air.propro.service.LibraryService;
import com.westlake.air.propro.service.TaskService;
import com.westlake.air.propro.async.LibraryTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-05-31 09:53
 */
public class BaseController {

    @Autowired
    LibraryService libraryService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    TaskService taskService;
    @Autowired
    LibraryTask libraryTask;
    @Autowired
    ExperimentTask experimentTask;

    //0:标准库,1:irt校准库
    public List<LibraryDO> getLibraryList(Integer type){
        return libraryService.getSimpleAll(type);
    }

    public List<ExperimentDO> getExperimentList(){
        return experimentService.getSimpleAll();
    }

    public final Logger logger = LoggerFactory.getLogger(getClass());
    public static String ERROR_MSG = "error_msg";
    public static String SUCCESS_MSG = "success_msg";

    public void buildPageQuery(PageQuery query, Integer currentPage, Integer pageSize){
        if(currentPage != null){
            query.setPageNo(currentPage);
        }
        if(pageSize != null){
            query.setPageSize(pageSize);
        }
    }

}
