package com.westlake.air.pecs.domain.db.simple;

import lombok.Data;

import java.util.HashMap;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-17 10:16
 */
@Data
public class TargetTransition {

    //对应的transition的Id,如果是MS1的则为对应的第一条transition的Id(一个MS1会对应多条transition记录)
    String id;

    String proteinName;

    String peptideRef;

    //对应的MS1荷质比
    float precursorMz;

    //对应的MS2荷质比
    float productMz;

    String annotations;

    String cutInfo;

    Boolean isDecoy;

    float rt;

    float rtStart;

    float rtEnd;

    /**
     * 如果是伪肽段,则本字段代表的是伪肽段中unimod的位置
     * key为unimod在肽段中的位置,位置从0开始计数,value为unimod的Id(参见unimod.obo文件)
     */
    HashMap<Integer, String> unimodMap;

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof TargetTransition) {
            TargetTransition target = (TargetTransition) obj;
            if (this.getPeptideRef().equals(target.getPeptideRef())) {
                return true;
            }

            return false;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (peptideRef).hashCode();
    }
}
