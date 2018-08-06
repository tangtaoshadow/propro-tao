package com.westlake.air.pecs.domain.db.simple;

import lombok.Data;

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



    float rt;

    float rtStart;

    float rtEnd;

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
