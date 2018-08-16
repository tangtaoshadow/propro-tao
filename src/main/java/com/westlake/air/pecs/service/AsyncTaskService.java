package com.westlake.air.pecs.service;

import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.domain.db.TaskDO;

import java.io.File;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-16 15:03
 */
public interface AsyncTaskService {

    void addExperimentTask(ExperimentDO experimentDO, TaskDO taskDO, File file);

    void addLibraryTask(LibraryDO libraryDO, TaskDO taskDO, File file);
}
