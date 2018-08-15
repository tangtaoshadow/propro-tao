package com.westlake.air.pecs.domain.query;

import lombok.Data;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 16:34
 */
@Data
public class AnalyseOverviewQuery extends PageQuery {

    String id;

    String expId;

    String libraryId;

    public AnalyseOverviewQuery(){}

    public AnalyseOverviewQuery(int pageNo,int pageSize){
        super(pageNo, pageSize);
    }
}
