package com.westlake.air.pecs.utils;

import com.westlake.air.pecs.domain.bean.transition.AminoAcid;
import com.westlake.air.pecs.domain.db.PeptideDO;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-27 14:41
 */
public class TransitionUtil {

    /**
     * TODO 没有解决首末位是Modification的情况
     *
     * @param acidList
     * @return
     */
    public static String toSequence(List<AminoAcid> acidList, boolean withUnimod) {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < acidList.size(); i++) {
            AminoAcid aa = acidList.get(i);
            sb.append(aa.getName());

            if (withUnimod && aa.getModId() != null && !aa.getModId().isEmpty()) {
                sb.append("(UniMod:").append(aa.getModId()).append(")");
            }
        }

        return sb.toString();
    }

    /**
     * 克隆一个Transition
     * 设置Sequence为空
     * 设置UnimodMap为空
     * 设置id为空
     * 设置ProductMz为空
     * 设置isDecoy为True
     *
     * @param peptideDO
     * @return
     */
    public static PeptideDO cloneForDecoy(PeptideDO peptideDO) {
        PeptideDO decoy = new PeptideDO();
        decoy.setTargetSequence(peptideDO.getSequence());
        decoy.setLibraryName(peptideDO.getLibraryName());
        decoy.setLibraryId(peptideDO.getLibraryId());
        decoy.setCharge(peptideDO.getCharge());
        decoy.setFullName(peptideDO.getFullName());
        decoy.setPeptideRef(peptideDO.getPeptideRef());
        decoy.setProteinName(peptideDO.getProteinName());
        decoy.setIsDecoy(true);
        decoy.setRt(peptideDO.getRt());
        decoy.setMz(peptideDO.getMz());

        return decoy;
    }

}
