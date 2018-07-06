package com.westlake.air.swathplatform.parser.model.mzxml;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-05 16:36
 */
@Data
public class ScanIndex {

    Long start;
    Long end;
    int id;
    int msLevel;

    List<ScanIndex> children;

    public ScanIndex() {
    }

    public ScanIndex(Long start, Long end) {
        this.start = start;
        this.end = end;
    }

    public ScanIndex(int id, Long start, Long end) {
        this.id = id;
        this.start = start;
        this.end = end;
    }

    public void add(ScanIndex index){
        if(children == null){
            children = new ArrayList<>();
        }

        children.add(index);
    }
}
