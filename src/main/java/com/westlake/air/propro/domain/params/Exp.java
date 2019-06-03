package com.westlake.air.propro.domain.params;

import lombok.Data;

@Data
public class Exp {

    String name;

    String filePath;

    String description;

    public Exp(){}

    public Exp(String name, String filePath, String description){
        this.name = name;
        this.filePath = filePath;
        this.description = description;
    }


}
