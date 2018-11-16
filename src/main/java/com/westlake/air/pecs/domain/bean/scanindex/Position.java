package com.westlake.air.pecs.domain.bean.scanindex;

import lombok.Data;

@Data
public class Position {

    Long start;

    Long delta;

    public Position(){}

    public Position(Long start, Long delta){
        this.start = start;
        this.delta = delta;
    }
}
