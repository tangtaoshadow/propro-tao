package com.westlake.air.pecs.async;

import com.westlake.air.pecs.domain.bean.SwathInput;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.service.ExperimentService;
import com.westlake.air.pecs.service.ScoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-17 10:40
 */
@Component("experimentTask")
public class ExperimentTask extends BaseTask{
    @Autowired
    ExperimentService experimentService;
    @Autowired
    ScoreService scoreService;

    @Async
    public void saveExperimentTask(ExperimentDO experimentDO, File file, TaskDO taskDO) {
        experimentService.uploadFile(experimentDO, file, taskDO);
    }

    /**
     * @param experimentDO
     * @param libraryId
     * @param slopeIntercept
     * @param creator
     * @param rtExtractWindow
     * @param mzExtractWindow
     * @param buildType       0:解压缩MS1和MS2; 1:解压缩MS1; 2:解压缩MS2
     * @return
     */
    @Async
    public void extract(ExperimentDO experimentDO, String libraryId, SlopeIntercept slopeIntercept, String creator, float rtExtractWindow, float mzExtractWindow, int buildType, TaskDO taskDO) {
        SwathInput input = new SwathInput();
        input.setExperimentDO(experimentDO);
        input.setLibraryId(libraryId);
        input.setSlopeIntercept(slopeIntercept);
        input.setCreator(creator);
        input.setRtExtractWindow(rtExtractWindow);
        input.setMzExtractWindow(mzExtractWindow);
        input.setBuildType(buildType);
        experimentService.extract(input);
    }

    public void swath(ExperimentDO experimentDO, String libraryId, String creator, float rtExtractWindow, float mzExtractWindow, int buildType){

    }
}
