package com.westlake.air.propro.domain.db;

import com.westlake.air.propro.domain.BaseDO;
import com.westlake.air.propro.domain.db.simple.TargetPeptide;
import com.westlake.air.propro.parser.model.chemistry.AminoAcid;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 19:16
 */
@Data
@Document(collection = "peptide")
public class PeptideDO extends BaseDO {

    @Id
    String id;

    /**
     * 对应的标准库ID
     */
    @Indexed
    String libraryId;

    /**
     * 标准库名称
     */
    String libraryName;

    /**
     * 对应蛋白质名称（包含Decoy信息）
     */
    String proteinName;

//    String uniProtName;

    /**
     * 肽段的唯一识别符,格式为 : fullName_precursorCharge,如果是伪肽段,则本字段为对应的真肽段的PeptideRef
     */
    @Indexed
    String peptideRef;

    Boolean isUnique = true;

    /**
     * 肽段的荷质比MZ
     */
    Double mz;

    /**
     * 肽段的归一化RT
     */
    Double rt;

    /**
     * 针对PRM提出，开始采集肽段的真实时间，单位：秒
     */
    Double PrmRtStart;

    /**
     * 针对PRM提出，peptidelist中预计肽段出现的时间，单位：秒
     */
    Double PrmRt;

    /**
     * 是否是伪肽段
     */
    Boolean isDecoy;

    /**
     * 对应肽段序列,如果是伪肽段,则本字段为伪肽段对应的原始真肽段的序列(不包含UniModId)
     */
    String targetSequence;

    /**
     * 对应肽段序列,如果是伪肽段,则为对应的伪肽段的序列(不包含UniModId)
     */
    String sequence;

    /**
     * 完整版肽段名称(含修饰基团),如果是伪肽段则为原始的肽段的完整序列而不是伪肽段的完整序列
     */
    String fullName;

    /**
     * 肽段带电量
     */
    Integer charge;

    /**
     * 如果是伪肽段,则本字段代表的是伪肽段中unimod的位置
     * key为unimod在肽段中的位置,位置从0开始计数,value为unimod的Id(参见unimod.obo文件)
     */
    HashMap<Integer, String> unimodMap;

    /**
     * 新字段,原始肽段的序列列表,包含修饰符
     */
    List<AminoAcid> acidList = new ArrayList<>();

    /**
     * 新字段,如果是伪肽段则为伪肽段的序列列表,包含修饰符
     */
    List<AminoAcid> decoyAcidList = new ArrayList<>();

    /**
     * 对应的肽段碎片的信息
     */
    HashMap<String, FragmentInfo> fragmentMap = new HashMap<>();

    String features;

    public void putFragment(String cutInfo, FragmentInfo fi){
        fragmentMap.put(cutInfo, fi);
    }

    public void removeFragment(String cutInfo){
        fragmentMap.remove(cutInfo);
    }

    public TargetPeptide toTargetPeptide(){
        TargetPeptide tp = new TargetPeptide();
        tp.setPeptideRef(getPeptideRef());
        tp.setRt(getRt().floatValue());
        tp.setFragmentMap(getFragmentMap());
        tp.setIsDecoy(getIsDecoy());
        tp.setMz(getMz().floatValue());
        tp.setProteinName(getProteinName());
        tp.setUnimodMap(getUnimodMap());
        return tp;
    }
}
