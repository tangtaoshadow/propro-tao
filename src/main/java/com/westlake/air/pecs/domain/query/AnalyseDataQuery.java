package com.westlake.air.pecs.domain.query;

import com.westlake.air.pecs.domain.db.simple.IntensityGroup;
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

    public AnalyseDataQuery(){}

    public AnalyseDataQuery(String overviewId, Integer msLevel){
        this.overviewId = overviewId;
        this.msLevel = msLevel;
    }
}
