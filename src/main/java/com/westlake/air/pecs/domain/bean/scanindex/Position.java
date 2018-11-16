package com.westlake.air.pecs.domain.bean.scanindex;

import lombok.Data;

@Data
public class Position {

    Long start;

    Long end;

    public Position() {
    }

    public Position(Long start, Long end) {
        this.start = start;
        this.end = end;
    }

    public long getDelta(){
        if(start != null && end != null){
            return end - start;
        }
        return 0L;
    }
}
