package com.westlake.air.propro.algorithm.decoy.generator;

import com.westlake.air.propro.algorithm.formula.FormulaCalculator;
import com.westlake.air.propro.algorithm.formula.FragmentFactory;
import com.westlake.air.propro.constants.Constants;
import com.westlake.air.propro.algorithm.decoy.BaseGenerator;
import com.westlake.air.propro.domain.bean.peptide.Annotation;
import com.westlake.air.propro.domain.db.FragmentInfo;
import com.westlake.air.propro.domain.db.PeptideDO;
import com.westlake.air.propro.algorithm.parser.model.chemistry.AminoAcid;
import com.westlake.air.propro.utils.TransitionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-07 10:22
 */
@Component("shuffleGenerator")
public class ShuffleGenerator extends BaseGenerator {

    public final Logger logger = LoggerFactory.getLogger(ShuffleGenerator.class);

    @Autowired
    FormulaCalculator formulaCalculator;

    @Autowired
    FragmentFactory fragmentFactory;

    public List<PeptideDO> generate(List<PeptideDO> list) {
        List<PeptideDO> decoys = new ArrayList<>();
        for (PeptideDO trans : list) {
            PeptideDO decoy = generate(trans);
            decoy.setIsUnique(trans.getIsUnique());
            decoys.add(decoy);
        }
        logger.info("伪肽段生成完毕,总计:"+decoys.size()+"个");
        return decoys;
    }

    public PeptideDO generate(PeptideDO peptideDO) {

        String sequence = peptideDO.getSequence();
        HashMap<Integer, String> unimodMap = peptideDO.getUnimodMap();

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

        aminoAcids = fragmentFactory.parseAminoAcid(sequence, unimodMap);

        List<AminoAcid> bestDecoy = null;
        Double asi = null;
        HashMap<Integer, String> newUnimodMap = new HashMap<>();

        //生成十个随机打乱的数组,比对重复度
        for (int i = 0; i < Constants.DECOY_GENERATOR_TRY_TIMES; i++) {

            Collections.shuffle(aminoAcids);

            String newSequence = TransitionUtil.toSequence(aminoAcids, false);
            double tempAsi = aaSequenceIdentify(sequence, newSequence);
            if (asi == null || asi > tempAsi) {
                asi = tempAsi;
                bestDecoy = aminoAcids;
                //如果已经生成一个重复度为0的肽段则可以直接跳出循环
                if (asi == 0) {
                    break;
                }
            }
            aminoAcids = fragmentFactory.parseAminoAcid(sequence, unimodMap);
        }

        if (removeLastAcid) {
            bestDecoy.add(lastAcid);
        }

        for (int i = 0; i < bestDecoy.size(); i++) {
            if (bestDecoy.get(i).getModId() != null) {
                newUnimodMap.put(i, bestDecoy.get(i).getModId());
            }
        }

        PeptideDO decoy = TransitionUtil.cloneForDecoy(peptideDO);
        decoy.setSequence(TransitionUtil.toSequence(bestDecoy, false));
        decoy.setUnimodMap(newUnimodMap);
        decoy.setDecoyAcidList(bestDecoy);

        for (String cutInfo : peptideDO.getFragmentMap().keySet()) {
            FragmentInfo targetFi = peptideDO.getFragmentMap().get(cutInfo);
            FragmentInfo decoyFi = new FragmentInfo();
            decoyFi.setCutInfo(cutInfo);
            decoyFi.setIntensity(targetFi.getIntensity());
            decoyFi.setCharge(targetFi.getCharge());
            decoyFi.setAnnotation(targetFi.getAnnotation());
            decoyFi.setAnnotations(targetFi.getAnnotation().toAnnoInfo());
            Annotation oneAnno = targetFi.getAnnotation();
            List<String> unimodIds = new ArrayList<>();
            if (oneAnno.getType() == null){
                System.out.println("debug");
            }
            List<AminoAcid> acids = fragmentFactory.getFragmentSequence(bestDecoy, oneAnno.getType(), oneAnno.getLocation());
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

            decoyFi.setMz(productMz);
            decoy.putFragment(cutInfo, decoyFi);
        }

        return decoy;
    }
}