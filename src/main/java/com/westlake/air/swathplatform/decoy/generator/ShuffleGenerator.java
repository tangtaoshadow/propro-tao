package com.westlake.air.swathplatform.decoy.generator;

import com.westlake.air.swathplatform.decoy.BaseGenerator;
import com.westlake.air.swathplatform.domain.db.TransitionDO;
import com.westlake.air.swathplatform.parser.model.traml.Modification;
import com.westlake.air.swathplatform.parser.model.traml.Peptide;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-07 10:22
 */
@Component("shuffleGenerator")
public class ShuffleGenerator extends BaseGenerator {

    public final Logger logger = LoggerFactory.getLogger(ReverseGenerator.class);

    /**
     * Reverse a peptide sequence (with its modifications)
     * 转置一个Sequence以及它的modification.如果存在C-,N-Terminal,由于是完全转置,所以C-Terminal和N-Terminal还是在两端不变,只是位置互换了一下
     * 例如肽段 C-A-P-M-K-N 这样一个肽段,修饰结构在两端,分别为C和N.因此Modification的位置为 C-0,N-5.其余的基团位置分别为A-1,P-2,M-3,K-4
     * 转换以后变为N-K-M-P-A-C. N依然修饰K,C依然修饰A
     * @param peptide
     * @return
     */
    @Override
    protected Peptide generate(Peptide peptide) {
        return null;
    }

    @Override
    protected TransitionDO generate(TransitionDO transitionDO) {
        if(transitionDO.getIsDecoy()){
            logger.warn("this is already a decoy!!!");
            return transitionDO;
        }

        String sequence = transitionDO.getPeptideSequence();
        char[] array = sequence.toCharArray();
        List<char[]> list = Arrays.asList(array);

        Collections.shuffle(Arrays.asList(array));

        return null;
    }
}
