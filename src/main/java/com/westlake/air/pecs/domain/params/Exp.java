package com.westlake.air.pecs.domain.params;

import lombok.Data;

@Data
public class Exp {

    String name;

    String filePath;

    Float overlap;

    String description;

    public Exp(){}

    public Exp(String name, String filePath, Float overlap, String description){
        this.name = name;
        this.filePath = filePath;
        this.overlap = overlap;
        this.description = description;
    }


}
