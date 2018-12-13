package com.westlake.air.pecs.domain.query;

import lombok.Data;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 15:55
 */
@Data
public class AnalyseDataQuery extends PageQuery{

    private static final long serialVersionUID = -3258829839166834225L;

    String id;

    String overviewId;

    String transitionId;

    String libraryId;

    Integer msLevel;

    String peptideRef;

    Boolean isHit;

    Boolean isDecoy;

    Float mzStart;

    Float mzEnd;

    public AnalyseDataQuery(){}

    public AnalyseDataQuery(String overviewId){
        this.overviewId = overviewId;
    }

    public AnalyseDataQuery(int pageNo,int pageSize){
        super(pageNo, pageSize);
    }
}
