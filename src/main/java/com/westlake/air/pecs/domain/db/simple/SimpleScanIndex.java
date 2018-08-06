package com.westlake.air.pecs.domain.db.simple;

import lombok.Data;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 20:46
 */
@Data
public class SimpleScanIndex {

    Float rt;

    Long start;

    Long end;

    public SimpleScanIndex(){

    }

    public SimpleScanIndex(Long start,Long end){
        this.start = start;
        this.end = end;
    }
}
