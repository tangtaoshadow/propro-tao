package com.westlake.air.swathplatform.domain.bean;

import com.westlake.air.swathplatform.dao.AminoAcidDAO;
import com.westlake.air.swathplatform.dao.ElementsDAO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-12 13:30
 */
public class PeptideFormula {

    @Autowired
    AminoAcidDAO aminoAcidDAO;

    @Autowired
    ElementsDAO elementsDAO;

    String acidFormula;

    public PeptideFormula(String formula) {
        this.acidFormula = formula;
    }

    public double getMonoWeight() {
        double monoWeight = 0;
        for (char acidCode : acidFormula.toCharArray()) {
            monoWeight += aminoAcidDAO.getAminoAcidByCode(String.valueOf(acidCode)).getMonoIsotopicMass();
        }
        return monoWeight;
    }

    public double getMonoWeightAsFull() {
        return getMonoWeight() + elementsDAO.getMonoWeight("H:2,O:1");
    }

    public double getMonoWeightAsNTerm() {
        return getMonoWeight() + elementsDAO.getMonoWeight("H:1");
    }

    public double getMonoWeightAsCTerm() {
        return getMonoWeight() + elementsDAO.getMonoWeight("O:1,H:1");
    }

    public double getMonoWeightAsAIon() {
        return getMonoWeightAsNTerm() - elementsDAO.getMonoWeight("C:1,H:1,O:1");
    }

    public double getMonoWeightAsBIon() {
        return getMonoWeightAsNTerm() - elementsDAO.getMonoWeight("H:1");
    }

    public double getMonoWeightAsCIon() {
        return getMonoWeightAsNTerm() - elementsDAO.getMonoWeight("N:1,H:2");
    }

    public double getMonoWeightAsXIon() {
        return getMonoWeightAsCTerm() + elementsDAO.getMonoWeight("C:1,O:1") - elementsDAO.getMonoWeight("H:1");
    }

    public double getMonoWeightAsYIon() {
        return getMonoWeightAsCTerm() + elementsDAO.getMonoWeight("H:1");
    }

    public double getMonoWeightAsZIon() {
        return getMonoWeightAsCTerm() + elementsDAO.getMonoWeight("N:1,H:2");
    }

    /**
     * 获取平均分子质量
     *
     * @return
     */
    public double getAverageWeight() {
        double averageWeight = 0;
        for (char acidCode : acidFormula.toCharArray()) {
            averageWeight += aminoAcidDAO.getAminoAcidByCode(String.valueOf(acidCode)).getAverageMass();
        }
        return averageWeight;
    }

    public double getAverageWeightAsFull() {
        return getAverageWeight() + elementsDAO.getAverageWeight("H:2,O:1");
    }

    public double getAverageWeightAsNTerm() {
        return getAverageWeight() + elementsDAO.getAverageWeight("H:1");
    }

    public double getAverageWeightAsCTerm() {
        return getAverageWeight() + elementsDAO.getAverageWeight("O:1,H:1");
    }

    public double getAverageWeightAsAIon() {
        return getAverageWeightAsNTerm() - elementsDAO.getAverageWeight("C:1,H:1,O:1");
    }

    public double getAverageWeightAsBIon() {
        return getAverageWeightAsNTerm() - elementsDAO.getAverageWeight("H:1");
    }

    public double getAverageWeightAsCIon() {
        return getAverageWeightAsNTerm() - elementsDAO.getAverageWeight("N:1,H:2");
    }

    public double getAverageWeightAsXIon() {
        return getAverageWeightAsCTerm() + elementsDAO.getAverageWeight("C:1,O:1") - elementsDAO.getAverageWeight("H:1");
    }

    public double getAverageWeightAsYIon() {
        return getAverageWeightAsCTerm() + elementsDAO.getAverageWeight("H:1");
    }

    public double getAverageWeightAsZIon() {
        return getAverageWeightAsCTerm() + elementsDAO.getAverageWeight("N:1,H:2");
    }

    public String getAcidFormula() {
        return acidFormula;
    }

    public void setAcidFormula(String acidFormula) {
        this.acidFormula = acidFormula;
    }
}
