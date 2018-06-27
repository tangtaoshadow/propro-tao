package com.westlake.air.swathplatform.decoy.generator;

import com.westlake.air.swathplatform.algorithm.FormulaCalculator;
import com.westlake.air.swathplatform.algorithm.FragmentCalculator;
import com.westlake.air.swathplatform.constants.Constants;
import com.westlake.air.swathplatform.decoy.BaseGenerator;
import com.westlake.air.swathplatform.domain.bean.AminoAcid;
import com.westlake.air.swathplatform.domain.bean.Annotation;
import com.westlake.air.swathplatform.domain.db.TransitionDO;
import com.westlake.air.swathplatform.parser.model.traml.Modification;
import com.westlake.air.swathplatform.parser.model.traml.Peptide;
import com.westlake.air.swathplatform.utils.TransitionUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-07 10:22
 */
@Component("shuffleGenerator")
public class ShuffleGenerator extends BaseGenerator {

    public final Logger logger = LoggerFactory.getLogger(ReverseGenerator.class);

    @Autowired
    FormulaCalculator formulaCalculator;

    @Autowired
    FragmentCalculator fragmentCalculator;

    public List<TransitionDO> generate(List<TransitionDO> list) {
        List<TransitionDO> decoys = new ArrayList<>();
        for (TransitionDO trans : list) {
            TransitionDO decoy = generate(trans);
            decoys.add(decoy);
        }
        return decoys;
    }

    public TransitionDO generate(TransitionDO transitionDO) {

        if (transitionDO.getIsDecoy()) {
            logger.warn("this is already a decoy!!!");
            return transitionDO;
        }

        String sequence = transitionDO.getSequence();
        HashMap<Integer, String> unimodMap = transitionDO.getUnimodMap();

        List<AminoAcid> aminoAcids = new ArrayList<>();

        //最后一位是K,P,R时保持最后一位氨基酸位置不变
        char lastAcidChar = sequence.toUpperCase().charAt(sequence.length() - 1);

        AminoAcid lastAcid = null;

        boolean removeLastAcid = false;
        if (lastAcidChar == 'K' || lastAcidChar == 'P' || lastAcidChar == 'R') {
            removeLastAcid = true;
            lastAcid = new AminoAcid();
            sequence = sequence.substring(0, sequence.length() - 1);
            lastAcid.setName(String.valueOf(lastAcidChar));
            if (unimodMap != null && unimodMap.get(sequence.length()) != null) {
                lastAcid.setModId(unimodMap.get(sequence.length()));
            }
        }

        char[] sequenceArray = sequence.toCharArray();

        for (int i = 0; i < sequenceArray.length; i++) {
            AminoAcid aa = new AminoAcid();
            aa.setName(String.valueOf(sequenceArray[i]));
            if (unimodMap != null) {
                aa.setModId(unimodMap.get(i));
            }
            aminoAcids.add(aa);
        }

        String bestDecoy = null;
        Double asi = null;
        HashMap<Integer, String> newUnimodMap = new HashMap<>();

        //生成十个随机打乱的数组,比对重复度
        for (int i = 0; i < Constants.DECOY_GENERATOR_TRY_TIMES; i++) {
            Collections.shuffle(aminoAcids);

            String newSequence = TransitionUtil.toSequence(aminoAcids, false);
            double tempAsi = aaSequenceIdentify(sequence, newSequence);
            if (asi == null || asi > tempAsi) {
                asi = tempAsi;
                bestDecoy = newSequence;
                //如果已经生成一个重复度为0的肽段则可以直接跳出循环
                if (asi == 0) {
                    break;
                }
            }
        }

        if (removeLastAcid) {
            aminoAcids.add(lastAcid);
        }

        for (int i = 0; i < aminoAcids.size(); i++) {
            if (aminoAcids.get(i).getModId() != null) {
                newUnimodMap.put(i, aminoAcids.get(i).getModId());
            }
        }

        TransitionDO decoy = TransitionUtil.cloneForDecoy(transitionDO);
        decoy.setSequence(bestDecoy + lastAcidChar);
        decoy.setUnimodMap(newUnimodMap);
        Annotation oneAnno = decoy.getAnnotations().get(0);

        List<String> unimodIds = new ArrayList<>();
        List<AminoAcid> acids = fragmentCalculator.getFragmentSequence(aminoAcids, oneAnno.getType(), oneAnno.getLocation());
        for (AminoAcid aminoAcid : acids) {
            if (aminoAcid.getModId() != null) {
                unimodIds.add(aminoAcid.getModId());
            }
        }

        double productMz = formulaCalculator.getMonoMz(
                TransitionUtil.toSequence(acids, false),
                oneAnno.getType(),
                oneAnno.getCharge(),
                oneAnno.getAdjust(),
                oneAnno.getDeviation(),
                oneAnno.isIsotope(),
                unimodIds
        );

        transitionDO.setProductMz(productMz);
        return transitionDO;
    }
}
