package com.westlake.air.pecs.domain.db.simple;

import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import lombok.Data;

import java.util.HashMap;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-05 15:57
 */
@Data
public class TransitionGroup {

    String proteinName;

    String peptideRef;

    Double rt;

    //key为cutinfo,例如b3^2,y7等等
    HashMap<String, AnalyseDataDO> dataMap;

    public TransitionGroup(){}

    public TransitionGroup(String proteinName, String peptideRef, Double rt){
        this.proteinName = proteinName;
        this.peptideRef = peptideRef;
        this.rt = rt;
    }

    public void addData(AnalyseDataDO dataDO){
        if (dataMap == null){
            dataMap = new HashMap<>();
        }
        dataMap.put(dataDO.getCutInfo(), dataDO);
    }
}
