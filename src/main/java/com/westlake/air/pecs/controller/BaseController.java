package com.westlake.air.pecs.controller;

import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.service.LibraryService;
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

    public List<LibraryDO> getLibraryList(){
        return libraryService.getSimpleAll();
    }

    public final Logger logger = LoggerFactory.getLogger(getClass());
    public static String ERROR_MSG = "error_msg";
    public static String SUCCESS_MSG = "success_msg";


}
