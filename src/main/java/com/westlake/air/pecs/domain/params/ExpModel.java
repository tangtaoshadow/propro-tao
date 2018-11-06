package com.westlake.air.pecs.domain.params;

import lombok.Data;

import java.util.List;

@Data
public class ExpModel {

    List<Exp> exps;

    public ExpModel(){}

    public ExpModel(List<Exp> exps){
        this.exps = exps;
    }
}
