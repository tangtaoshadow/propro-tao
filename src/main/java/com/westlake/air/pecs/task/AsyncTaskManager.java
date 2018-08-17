package com.westlake.air.pecs.task;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.parser.MzMLParser;
import com.westlake.air.pecs.parser.MzXMLParser;
import com.westlake.air.pecs.service.ExperimentService;
import com.westlake.air.pecs.service.LibraryService;
import com.westlake.air.pecs.service.ScanIndexService;
import com.westlake.air.pecs.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-17 10:40
 */
@Component("asyncTaskManager")
public class AsyncTaskManager {

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
    @Autowired
    LibraryService libraryService;

    int errorListNumberLimit = 10;

    @Async
    public void saveExperimentTask(ExperimentDO experimentDO, File file, TaskDO taskDO){
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

            if (resultDO.isFailed()) {
                taskDO.addLog("索引存储失败" + resultDO.getMsgInfo());
                taskDO.finish(TaskDO.STATUS_FAILED);
                taskService.update(taskDO);
                experimentService.delete(experimentDO.getId());
                scanIndexService.deleteAllByExperimentId(experimentDO.getId());

            } else {
                taskDO.addLog("索引存储成功");
                taskDO.finish(TaskDO.STATUS_SUCCESS);
                taskService.update(taskDO);
            }

        } catch (Exception e) {
            taskDO.addLog("索引存储失败:"+e.getMessage());
            taskDO.finish(TaskDO.STATUS_FAILED);
            taskService.update(taskDO);
            e.printStackTrace();
        }
    }

    @Async
    public void saveLibraryTask(LibraryDO library, InputStream in, String fileName, Boolean justReal, TaskDO taskDO) {
        //先Parse文件,再作数据库的操作
        ResultDO result = libraryService.parseAndInsertTsv(library, in, fileName,  justReal, taskDO);
        if (result.getErrorList() != null) {
            if (result.getErrorList().size() > errorListNumberLimit) {
                taskDO.addLog("解析错误,错误的条数过多,这边只显示" + errorListNumberLimit + "条错误信息");
                taskDO.addLog(result.getErrorList().subList(0, errorListNumberLimit));
            } else {
                taskDO.addLog(result.getErrorList());
            }
        }

        if (result.isFailed()) {
            taskDO.addLog(result.getMsgInfo());
            taskDO.finish(TaskDO.STATUS_FAILED);
        }

        /**
         * 如果全部存储成功,开始统计蛋白质数目,肽段数目和Transition数目
         */
        taskDO.setCurrentStep(3);
        taskDO.addLog("开始统计蛋白质数目,肽段数目和Transition数目");
        taskService.update(taskDO);
        libraryService.countAndUpdateForLibrary(library);

        taskDO.addLog("统计完毕");
        taskDO.setCurrentStep(4);
        taskDO.finish(TaskDO.STATUS_SUCCESS);
        taskService.update(taskDO);
    }
}
