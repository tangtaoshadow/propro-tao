package com.westlake.air.pecs.domain.bean;

import lombok.Data;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-17 10:16
 */
@Data
public class TargetTransition {

    //对应的transition的Id,如果是MS1的则为对应的第一条transition的Id(一个MS1会对应多条transition记录)
    String id;

    //对应的MS1荷质比
    float precursorMz;

    //对应的MS2荷质比
    float productMz;

    //对应的肽段全名(包含unimod)
    String fullName;

    Integer precursorCharge;

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
            if (this.fullName.equals(target.getFullName())) {
                return true;
            }

            return false;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (fullName).hashCode();
    }
}
