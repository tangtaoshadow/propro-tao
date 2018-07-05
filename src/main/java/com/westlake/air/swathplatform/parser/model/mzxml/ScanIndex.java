package com.westlake.air.swathplatform.parser.model.mzxml;

import lombok.Data;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-05 16:36
 */
@Data
public class ScanIndex {

    Long start;
    Long end;
    int id;

    public ScanIndex(int id, Long start, Long end) {
        this.id = id;
        this.start = start;
        this.end = end;
    }

}
