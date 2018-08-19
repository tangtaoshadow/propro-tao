package com.westlake.air.pecs.async;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.service.LibraryService;
import com.westlake.air.pecs.service.TaskService;
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
    TaskService taskService;
    @Autowired
    LibraryService libraryService;

    int errorListNumberLimit = 10;

    @Async
    public void saveLibraryTask(LibraryDO library, InputStream in, String fileName, Boolean justReal, TaskDO taskDO) {
        //先Parse文件,再作数据库的操作
        ResultDO result = libraryService.parseAndInsertTsv(library, in, fileName, justReal, taskDO);
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
        taskDO.addLog("开始统计蛋白质数目,肽段数目和Transition数目");
        taskService.update(taskDO);
        libraryService.countAndUpdateForLibrary(library);

        taskDO.addLog("统计完毕");
        taskDO.finish(TaskDO.STATUS_SUCCESS);
        taskService.update(taskDO);
    }
}
