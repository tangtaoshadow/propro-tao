package com.westlake.air.pecs.domain.db;

import com.westlake.air.pecs.domain.BaseDO;
import com.westlake.air.pecs.domain.bean.task.MachineInfo;
import com.westlake.air.pecs.domain.bean.task.TaskLog;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-13 21:34
 */
@Data
@Document(collection = "task")
public class TaskDO extends BaseDO {

    public static String STATUS_UNKNOWN = "UNKNOWN";
    public static String STATUS_RUNNING = "RUNNING";
    public static String STATUS_FAILED = "FAILED";
    public static String STATUS_SUCCESS = "SUCCESS";

    String id;

    String name;

    String creator = "Admin";

    String expId;

    String status;

    String libraryId;

    String libraryName;

    String iRtLibraryId;

    String iRtLibraryName;

    String overviewId;

    Integer currentStep;

    String taskTemplate;

    Date createDate;

    Date lastModifiedDate;

    MachineInfo machineInfo;

    List<TaskLog> logs;

    Long totalCost;

    public void addLog(String content) {
        if (logs == null) {
            logs = new ArrayList<>();
            logs.add(new TaskLog("Task Started"));
        }
        TaskLog taskLog = new TaskLog(content);

        logs.add(taskLog);
    }

    public void start(){
        if(logs == null || logs.size() == 0){
            logs = new ArrayList<>();
            logs.add(new TaskLog("Task Started"));
        }
    }

    public Long getStartTime(){
        if(logs == null || logs.size() == 0){
            return null;
        }
        TaskLog taskLog = logs.get(0);
        return taskLog.getTime().getTime();
    }

    public void finish(){
        addLog("Task Ended");
        totalCost = System.currentTimeMillis() - getStartTime();
    }
}
