package com.westlake.air.pecs.domain.query;

import lombok.Data;

import java.util.Date;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-04 21:16
 */
@Data
public class ExperimentQuery extends PageQuery {

    private static final long serialVersionUID = -3258829839160856645L;

    String id;

    String name;

    Date createDate;

    Date lastModifiedDate;
}
