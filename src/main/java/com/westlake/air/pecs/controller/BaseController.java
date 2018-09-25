package com.westlake.air.pecs.controller;

import com.westlake.air.pecs.async.ExperimentTask;
import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.service.LibraryService;
import com.westlake.air.pecs.service.TaskService;
import com.westlake.air.pecs.async.LibraryTask;
import com.westlake.air.pecs.service.impl.AnalyseOverviewServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-05-31 09:53
 */
public class BaseController {

    @Autowired
    LibraryService libraryService;
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

    public final Logger logger = LoggerFactory.getLogger(getClass());
    public static String ERROR_MSG = "error_msg";
    public static String SUCCESS_MSG = "success_msg";


}
