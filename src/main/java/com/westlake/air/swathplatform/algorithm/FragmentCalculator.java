package com.westlake.air.swathplatform.algorithm;

import com.westlake.air.swathplatform.domain.bean.Annotation;
import com.westlake.air.swathplatform.domain.db.TransitionDO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-12 10:32
 */
@Component
public class FragmentCalculator {

    public TransitionDO cal(TransitionDO transitionDO){
        String sequence = transitionDO.getPeptideSequence();
        List<Annotation> annotationList = transitionDO.getAnnotations();

        return transitionDO;
    }


}
