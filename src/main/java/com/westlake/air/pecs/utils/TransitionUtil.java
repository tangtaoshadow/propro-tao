package com.westlake.air.pecs.utils;

import com.westlake.air.pecs.domain.bean.AminoAcid;
import com.westlake.air.pecs.domain.db.TransitionDO;

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
    public static String toSequence(List<AminoAcid> acidList,boolean withUnimod) {

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
     * @param transitionDO
     * @return
     */
    public static TransitionDO cloneForDecoy(TransitionDO transitionDO){
        TransitionDO decoy = new TransitionDO();
        decoy.setTargetSequence(transitionDO.getSequence());
        decoy.setLibraryName(transitionDO.getLibraryName());
        decoy.setWithBrackets(transitionDO.isWithBrackets());
        decoy.setAnnotations(transitionDO.getAnnotations());
        decoy.setAnnotation(transitionDO.getAnnotation());
        decoy.setLibraryId(transitionDO.getLibraryId());
        decoy.setPrecursorCharge(transitionDO.getPrecursorCharge());
        decoy.setFullName(transitionDO.getFullName());
        decoy.setProteinName(transitionDO.getProteinName());
        decoy.setIsDecoy(true);
        decoy.setName(transitionDO.getName());
        decoy.setRt(transitionDO.getRt());
        decoy.setIntensity(transitionDO.getIntensity());
        decoy.setPrecursorMz(transitionDO.getPrecursorMz());

        return decoy;
    }
}
