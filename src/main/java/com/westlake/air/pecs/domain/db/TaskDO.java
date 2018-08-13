package com.westlake.air.pecs.domain.db;

import com.westlake.air.pecs.domain.BaseDO;
import com.westlake.air.pecs.domain.bean.task.Report;
import com.westlake.air.pecs.domain.bean.task.StepLog;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-13 21:34
 */
@Data
@Document(collection = "task")
public class TaskDO extends BaseDO {

    String id;

    String name;

    String creator;

    String experimentId;

    String sLibraryId;

    String sLibraryName;

    String vLibraryId;

    String vLibraryName;

    String overviewId;

    String currentStep;

    String workflow;

    Date createDate;

    Date lastModifiedDate;

    List<StepLog> stepLogs;

    List<Report> reports;
}
