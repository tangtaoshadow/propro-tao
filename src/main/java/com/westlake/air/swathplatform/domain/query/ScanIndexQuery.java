package com.westlake.air.swathplatform.domain.query;

import lombok.Data;

import java.util.Date;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-04 21:16
 */
@Data
public class ScanIndexQuery extends PageQuery {

    private static final long serialVersionUID = -3258829832460856645L;

    String id;

    String experimentId;

    Integer numStart;

    Integer numEnd;

    Integer msLevel;

    Double rtStart;

    Double rtEnd;
}
