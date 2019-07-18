package com.westlake.air.propro.algorithm.parser;

import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.TaskDO;
import com.westlake.air.propro.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.HashSet;

/**
 * 对于TraML文件的高速解析引擎
 */
@Component("fastTraMLParser")
public class FastTraMLParser extends BaseLibraryParser {

    @Autowired
    TaskService taskService;

    @Override
    public ResultDO parseAndInsert(InputStream in, LibraryDO library, TaskDO taskDO) {
        return null;
    }

    @Override
    public ResultDO selectiveParseAndInsert(InputStream in, LibraryDO library, HashSet<String> selectedPepSet, boolean selectBySequence, TaskDO taskDO) {
        return null;
    }

    private void seekForProteinList(){

    }
}
