package com.westlake.air.pecs.domain.params;

import lombok.Data;

import java.util.List;

@Data
public class ExpVO {

    List<Exp> exps;

    public ExpVO(){}

    public ExpVO(List<Exp> exps){
        this.exps = exps;
    }
}
