package com.westlake.air.swathplatform.domain.bean;

import com.westlake.air.swathplatform.domain.db.TransitionDO;
import lombok.Data;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-12 20:27
 */
@Data
public class FragmentResult {

    TransitionDO transitionDO;

    double monoWeight;
    double averageWeight;

    public FragmentResult(TransitionDO transitionDO){
        this.transitionDO = transitionDO;
    }
}
