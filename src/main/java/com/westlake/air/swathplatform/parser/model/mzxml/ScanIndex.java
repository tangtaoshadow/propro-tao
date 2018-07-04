package com.westlake.air.swathplatform.parser.model.mzxml;

import lombok.Data;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-04 19:45
 */

@Data
public class ScanIndex {

    public ScanIndex(int id, Long start, Long end) {
        this.id = id;
        this.start = start;
        this.end = end;
    }

    int id;
    Long start;
    Long end;
}
