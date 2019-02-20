package com.westlake.air.propro.domain.bean.scanindex;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class Position {

    @JSONField(name = "s")
    Long start;

    @JSONField(name = "d")
    Long delta;

    public Position(){}

    public Position(Long start, Long delta){
        this.start = start;
        this.delta = delta;
    }
}
