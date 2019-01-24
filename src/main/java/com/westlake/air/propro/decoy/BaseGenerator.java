package com.westlake.air.propro.decoy;

import com.westlake.air.propro.parser.model.traml.Modification;
import com.westlake.air.propro.parser.model.traml.Peptide;

import java.util.HashMap;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-07 10:20
 */
public class BaseGenerator {

    /**
     * Compute relative identity (relative number of matches of amino acids at the same position) between two sequences
     *
     * @param sequence
     * @param decoy
     * @return
     */
    protected double aaSequenceIdentify(String sequence, String decoy) {
        //sequence和decoy的长度必须一致
        if (sequence.length() != decoy.length()) {
            return 0;
        }

        double count = 0;
        for (int i = 0; i < sequence.length(); i++) {
            if(sequence.charAt(i) == decoy.charAt(i)){
                count++;
            }
        }

        return count/sequence.length();
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
