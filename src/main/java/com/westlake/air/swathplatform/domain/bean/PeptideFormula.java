package com.westlake.air.swathplatform.domain.bean;

import com.westlake.air.swathplatform.constants.ResidueType;
import com.westlake.air.swathplatform.dao.AminoAcidDAO;
import com.westlake.air.swathplatform.dao.ElementsDAO;
import com.westlake.air.swathplatform.utils.SpringUtil;
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

    private String acidFormula;

    private int charge = 1;

    private String type;

    public PeptideFormula(String formula, int charge, String type) {
        aminoAcidDAO = (AminoAcidDAO) SpringUtil.getObject("aminoAcidDAO");
        elementsDAO = (ElementsDAO) SpringUtil.getObject("elementsDAO");

        this.acidFormula = formula;
        this.charge = charge;
        this.type = type;
    }

    public double getMonoWeight() {
        switch (type) {
            case ResidueType.Full:
                return (getMonoWeightAsFull() + getMonoHWeight()) / charge;
            case ResidueType.AIon:
                return (getMonoWeightAsAIon() + getMonoHWeight()) / charge;
            case ResidueType.BIon:
                return (getMonoWeightAsBIon() + getMonoHWeight()) / charge;
            case ResidueType.CIon:
                return (getMonoWeightAsCIon() + getMonoHWeight()) / charge;
            case ResidueType.XIon:
                return (getMonoWeightAsXIon() + getMonoHWeight()) / charge;
            case ResidueType.YIon:
                return (getMonoWeightAsYIon() + getMonoHWeight()) / charge;
            case ResidueType.ZIon:
                return (getMonoWeightAsZIon() + getMonoHWeight()) / charge;
            default:
                return 0;
        }
    }

    public double getAverageWeight() {
        switch (type) {
            case ResidueType.Full:
                return (getAverageWeightAsFull() + getAverageHWeight()) / charge;
            case ResidueType.AIon:
                return (getAverageWeightAsAIon() + getAverageHWeight()) / charge;
            case ResidueType.BIon:
                return (getAverageWeightAsBIon() + getAverageHWeight()) / charge;
            case ResidueType.CIon:
                return (getAverageWeightAsCIon() + getAverageHWeight()) / charge;
            case ResidueType.XIon:
                return (getAverageWeightAsXIon() + getAverageHWeight()) / charge;
            case ResidueType.YIon:
                return (getAverageWeightAsYIon() + getAverageHWeight()) / charge;
            case ResidueType.ZIon:
                return (getAverageWeightAsZIon() + getAverageHWeight()) / charge;
            default:
                return 0;
        }
    }

    public String getAcidFormula() {
        return acidFormula;
    }

    public void setAcidFormula(String acidFormula) {
        this.acidFormula = acidFormula;
    }

    public int getCharge() {
        return charge;
    }

    public void setCharge(int charge) {
        this.charge = charge;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    private double getMonoHWeight() {
        return elementsDAO.getMonoWeight("H:" + charge);
    }

    private double getAverageHWeight() {
        return elementsDAO.getAverageWeight("H:" + charge);
    }

    private double getAcidMonoWeight() {
        double monoWeight = 0;
        for (char acidCode : acidFormula.toCharArray()) {
            monoWeight += aminoAcidDAO.getAminoAcidByCode(String.valueOf(acidCode)).getMonoIsotopicMass();
        }
        return monoWeight;
    }

    private double getMonoWeightAsFull() {
        return getAcidMonoWeight() + elementsDAO.getMonoWeight("H:2,O:1");
    }

    private double getMonoWeightAsNTerm() {
        return getAcidMonoWeight() + elementsDAO.getMonoWeight("H:1");
    }

    private double getMonoWeightAsCTerm() {
        return getAcidMonoWeight() + elementsDAO.getMonoWeight("O:1,H:1");
    }

    private double getMonoWeightAsAIon() {
        return getMonoWeightAsNTerm() - elementsDAO.getMonoWeight("C:1,H:1,O:1");
    }

    private double getMonoWeightAsBIon() {
        return getMonoWeightAsNTerm() - elementsDAO.getMonoWeight("H:1");
    }

    private double getMonoWeightAsCIon() {
        return getMonoWeightAsNTerm() - elementsDAO.getMonoWeight("N:1,H:2");
    }

    private double getMonoWeightAsXIon() {
        return getMonoWeightAsCTerm() + elementsDAO.getMonoWeight("C:1,O:1") - elementsDAO.getMonoWeight("H:1");
    }

    private double getMonoWeightAsYIon() {
        return getMonoWeightAsCTerm() + elementsDAO.getMonoWeight("H:1");
    }

    private double getMonoWeightAsZIon() {
        return getMonoWeightAsCTerm() + elementsDAO.getMonoWeight("N:1,H:2");
    }

    /**
     * 获取平均分子质量
     *
     * @return
     */
    private double getAcidAverageWeight() {
        double averageWeight = 0;
        for (char acidCode : acidFormula.toCharArray()) {
            averageWeight += aminoAcidDAO.getAminoAcidByCode(String.valueOf(acidCode)).getAverageMass();
        }
        return averageWeight;
    }

    private double getAverageWeightAsFull() {
        return getAcidAverageWeight() + elementsDAO.getAverageWeight("H:2,O:1");
    }

    private double getAverageWeightAsNTerm() {
        return getAcidAverageWeight() + elementsDAO.getAverageWeight("H:1");
    }

    private double getAverageWeightAsCTerm() {
        return getAcidAverageWeight() + elementsDAO.getAverageWeight("O:1,H:1");
    }

    private double getAverageWeightAsAIon() {
        return getAverageWeightAsNTerm() - elementsDAO.getAverageWeight("C:1,H:1,O:1");
    }

    private double getAverageWeightAsBIon() {
        return getAverageWeightAsNTerm() - elementsDAO.getAverageWeight("H:1");
    }

    private double getAverageWeightAsCIon() {
        return getAverageWeightAsNTerm() - elementsDAO.getAverageWeight("N:1,H:2");
    }

    private double getAverageWeightAsXIon() {
        return getAverageWeightAsCTerm() + elementsDAO.getAverageWeight("C:1,O:1") - elementsDAO.getAverageWeight("H:1");
    }

    private double getAverageWeightAsYIon() {
        return getAverageWeightAsCTerm() + elementsDAO.getAverageWeight("H:1");
    }

    private double getAverageWeightAsZIon() {
        return getAverageWeightAsCTerm() + elementsDAO.getAverageWeight("N:1,H:2");
    }
}
