package com.westlake.air.pecs.service.impl;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.parser.MzMLParser;
import com.westlake.air.pecs.parser.MzXMLParser;
import com.westlake.air.pecs.service.AsyncTaskService;
import com.westlake.air.pecs.service.ExperimentService;
import com.westlake.air.pecs.service.ScanIndexService;
import com.westlake.air.pecs.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-16 15:02
 */
@Service("asyncTaskService")
public class AsyncTaskServiceImpl implements AsyncTaskService {

    @Autowired
    MzXMLParser mzXMLParser;
    @Autowired
    MzMLParser mzMLParser;
    @Autowired
    TaskService taskService;
    @Autowired
    ScanIndexService scanIndexService;
    @Autowired
    ExperimentService experimentService;

    @Async
    public void addExperimentTask(ExperimentDO experimentDO, TaskDO taskDO, File file){
        try {
            long start = System.currentTimeMillis();
            //建立索引
            List<ScanIndexDO> indexList = null;
            //传入不同的文件类型会调用不同的解析层
            if (experimentDO.getFileType().equals(Constants.EXP_SUFFIX_MZXML)) {
                indexList = mzXMLParser.index(file, experimentDO.getId(), taskDO);
            } else if (experimentDO.getFileType().equals(Constants.EXP_SUFFIX_MZML)) {
                indexList = mzMLParser.index(file, experimentDO.getId(), taskDO);
            }

            taskDO.setCurrentStep(2);
            taskDO.addLog("索引构建完毕,开始存储索引");
            taskService.update(taskDO);

            ResultDO resultDO = scanIndexService.insertAll(indexList, true);

            taskDO.setCurrentStep(3);
            taskService.update(taskDO);

            if (resultDO.isFailed()) {
                taskDO.setStatus(TaskDO.STATUS_FAILED);
                taskDO.addLog("索引存储失败" + resultDO.getMsgInfo());
                taskDO.finish();
                taskService.update(taskDO);
                experimentService.delete(experimentDO.getId());
                scanIndexService.deleteAllByExperimentId(experimentDO.getId());

            } else {
                taskDO.setStatus(TaskDO.STATUS_SUCCESS);
                taskDO.addLog("索引存储成功");
                taskDO.finish();
                taskService.update(taskDO);
            }

        } catch (Exception e) {
            taskDO.setStatus(TaskDO.STATUS_SUCCESS);
            taskDO.addLog("索引存储失败:"+e.getMessage());
            taskDO.finish();
            taskService.update(taskDO);
            e.printStackTrace();
        }
    }

    @Override
    public void addLibraryTask(LibraryDO libraryDO, TaskDO taskDO, File file) {

    }
}
