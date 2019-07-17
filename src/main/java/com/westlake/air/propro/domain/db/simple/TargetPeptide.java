package com.westlake.air.propro.domain.db.simple;

import com.westlake.air.propro.domain.db.FragmentInfo;
import com.westlake.air.propro.domain.db.PeptideDO;
import lombok.Data;

import java.util.HashMap;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-17 10:16
 * 具体的注释说明请参考PeptideDO类
 */
@Data
public class TargetPeptide {

    String id;

    String proteinName;

    String peptideRef;

    /**
     * 对应肽段序列,如果是伪肽段,则为对应的伪肽段的序列(不包含UniModId)
     */
    String sequence;

    /**
     * 对应的前体荷质比
     */
    float mz;

    HashMap<String, FragmentInfo> fragmentMap;

    Boolean isUnique;

    float rt;

    //rtStart和rtEnd是在计算时使用的,并不会存在数据库中
    float rtStart;
    float rtEnd;

    /**
     * 如果是伪肽段,则本字段代表的是伪肽段中unimod的位置
     * key为unimod在肽段中的位置,位置从0开始计数,value为unimod的Id(参见unimod.obo文件)
     */
    HashMap<Integer, String> unimodMap;

    /**
     * 伪肽段的信息
     */
    String decoySequence;
    String decoyFullName;
    HashMap<Integer, String> decoyUnimodMap;
    HashMap<String, FragmentInfo> decoyFragmentMap;

    public TargetPeptide(){}

    public TargetPeptide(PeptideDO peptide){
        this.id = peptide.getId();
        this.proteinName = peptide.getProteinName();
        this.peptideRef = peptide.getPeptideRef();
        this.mz = peptide.getMz().floatValue();
        this.fragmentMap = peptide.getFragmentMap();
        this.rt = peptide.getRt().floatValue();
        this.decoySequence = peptide.getDecoySequence();
        this.decoyFullName = peptide.getFullName();
        this.decoyUnimodMap = peptide.getDecoyUnimodMap();
        this.decoyFragmentMap = peptide.getDecoyFragmentMap();
    }

    //根据自身构建IntensityMap,key为cutInfo,value为对应的Intensity值
    public HashMap<String, Float> buildIntensityMap(){
        HashMap<String, Float> intensityMap = new HashMap<>();
        for(String cutInfo : fragmentMap.keySet()){
            intensityMap.put(cutInfo, fragmentMap.get(cutInfo).getIntensity().floatValue());
        }
        return intensityMap;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof TargetPeptide) {
            TargetPeptide target = (TargetPeptide) obj;
            if (this.getPeptideRef().equals(target.getPeptideRef())) {
                return true;
            }

            return false;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (peptideRef).hashCode();
    }
}
