package com.westlake.air.swathplatform.domain.query;

import lombok.Data;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 15:55
 */
@Data
public class ConvolutionDataQuery extends PageQuery{

    private static final long serialVersionUID = -3258829839166834225L;

    String id;

    String expId;

    String transitionId;

    Integer msLevel;
}
