package com.westlake.air.pecs.domain.query;

import lombok.Data;

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

    Integer parentNum;

    //前体的荷质比窗口开始位置
    Float precursorMzStart;

    //前体的荷质比窗口结束位置
    Float precursorMzEnd;

    public ScanIndexQuery() {
    }

    public ScanIndexQuery(String experimentId, Integer msLevel) {
        this.experimentId = experimentId;
        this.msLevel = msLevel;
    }
}
