package com.westlake.air.swathplatform.service;

import com.westlake.air.swathplatform.domain.traml.Modification;
import com.westlake.air.swathplatform.domain.traml.Peptide;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

/**
 * Generate decoys from a TargetedExperiment
 * <p>
 * Will generate decoy peptides for each target peptide provided in exp and
 * write them into the decoy experiment.
 * <p>
 * Valid methods: shuffle, reverse, pseudo-reverse
 * <p>
 * If theoretical is true, the target transitions will be returned but their
 * masses will be adjusted to match the theoretical value of the fragment ion
 * that is the most likely explanation for the product.
 * <p>
 * mz_threshold is used for the matching of theoretical ion series to the observed one
 *
 * Created by James Lu MiaoShan
 * Time: 2018-05-31 09:53
 */
@Component
public class DecoyGeneratorService {

    /**
     * Reverse a peptide sequence (with its modifications)
     * 转置一个Sequence以及它的modification.如果存在C-,N-Terminal,由于是完全转置,所以C-Terminal和N-Terminal还是在两端不变,只是位置互换了一下
     * 例如肽段 C-A-P-M-K-N 这样一个肽段,修饰结构在两端,分别为C和N.因此Modification的位置为 C-0,N-5.其余的基团位置分别为A-1,P-2,M-3,K-4
     * 转换以后变为N-K-M-P-A-C. N依然修饰K,C依然修饰A
     * @param peptide
     * @return
     */
    public Peptide reverse(Peptide peptide) {
        String sequence = peptide.getSequence();
        sequence = StringUtils.reverse(sequence);
        peptide.setSequence(sequence);
        for (Modification modification : peptide.getModificationList()) {
            //转置以后,原来为i的位置会变为sequence.length()+ 1 -i
            modification.setLocation(sequence.length() + 1 - modification.getLocation());
        }
        return peptide;
    }

    /**
     * Pseudo-reverse a peptide sequence (with its modifications)
     * Pseudo reverses a peptide sequence, leaving the last AA constant
     *
     * @param peptide
     * @return
     */
    public Peptide pseudoReverse(Peptide peptide) {
        return null;
    }

    /**
     * Shuffle a peptide (with its modifications) sequence
     * This function will shuffle the given peptide sequences and its
     * modifications such that the resulting relative sequence identity is below
     * identity_threshold.
     *
     * @param peptide
     * @return
     */
    public Peptide shuffle(Peptide peptide) {
        return null;
    }

    /**
     * Compute relative identity (relative number of matches of amino acids at the same position) between two sequences
     *
     * @param sequence
     * @param decoy
     * @return
     */
    private float aaSequenceIdentify(String sequence, String decoy) {
        return 0;
    }

    /**
     * Find all K, R, P sites in a sequence to be set as fixed
     *
     * @param sequence
     * @return
     */
    private HashMap<Integer, String> findFixedResidues(String sequence) {
        return null;
    }

    /**
     * Find all K, R, P and C-/N-terminal sites in a sequence to be set as fixed
     * This method was adapted from the SpectraST decoy generator
     *
     * @param sequence
     * @return
     */
    private HashMap<Integer, String> findFixedAndTermResidues(String sequence) {
        return null;
    }

    /**
     * Check if a peptide has C or N terminal modifications
     *
     * @param peptide
     * @return
     */
    private boolean hasCNterminalMods(Peptide peptide) {
        List<Modification> modificationList = peptide.getModificationList();
        for (Modification modification : modificationList) {
            if (modification.getLocation() == 0 || modification.getLocation() == (peptide.getSequence().length() + 1)) {
                return true;
            }
        }
        return false;
    }
}
