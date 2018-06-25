package com.westlake.air.swathplatform.decoy.generator;

import com.westlake.air.swathplatform.decoy.BaseGenerator;
import com.westlake.air.swathplatform.domain.db.TransitionDO;
import com.westlake.air.swathplatform.parser.model.traml.Peptide;
import org.springframework.stereotype.Component;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-07 10:23
 */
@Component("pseudoReverseGenerator")
public class PseudoReverseGenerator extends BaseGenerator {
    @Override
    protected Peptide generate(Peptide peptide) {
        return null;
    }

    @Override
    protected TransitionDO generate(TransitionDO transitionDO) {
        return null;
    }
}
