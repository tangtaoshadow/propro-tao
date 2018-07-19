package com.westlake.air.swathplatform.domain.query;

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

    Integer msLevel;
}
