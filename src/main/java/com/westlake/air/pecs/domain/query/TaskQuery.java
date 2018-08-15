package com.westlake.air.pecs.domain.query;

import lombok.Data;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-15 10:06
 */
@Data
public class TaskQuery extends PageQuery{

    String id;

    String name;

    String creator;

    String expId;

    String libraryId;

    String overviewId;

    String currentStep;

    String taskTemplate;

    public TaskQuery(){}

    public TaskQuery(int pageNo,int pageSize){
        super(pageNo, pageSize);
    }
}
