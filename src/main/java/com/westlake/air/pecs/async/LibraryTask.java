package com.westlake.air.pecs.async;

import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.service.LibraryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-17 10:40
 */
@Component("libraryTask")
public class LibraryTask {

    public final Logger logger = LoggerFactory.getLogger(LibraryTask.class);

    @Autowired
    LibraryService libraryService;

    @Async
    public void saveLibraryTask(LibraryDO library, InputStream in, String fileName, Boolean justReal, TaskDO taskDO) {
        libraryService.uploadFile(library, in, fileName, justReal, taskDO);
    }
}