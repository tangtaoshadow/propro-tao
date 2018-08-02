package com.westlake.air.pecs.domain.db;

import com.westlake.air.pecs.domain.bean.AminoAcid;
import com.westlake.air.pecs.domain.bean.Annotation;
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
@Document(collection = "transition")
public class TransitionDO {

    @Id
    String id;

    /**
     * 对应的标准库ID
     */
    @Indexed
    String libraryId;

    String libraryName;

    /**
     * 肽段的荷质比MZ
     */
    Double precursorMz;

    /**
     * 离子片段的荷质比MZ(Mono荷质比)
     */
    Double productMz;

    /**
     * 离子片段的带电量
     */
    Integer productCharge;
    /**
     * 归一化RT
     */
    Double rt;

    /**
     * 过渡态名称,如果是伪肽段,则为包含了原始肽段的属性
     */
    String name;

    /**
     * 离子强度
     * library intensity
     */
    Double intensity;

    /**
     * 是否是伪肽段
     */
    Boolean isDecoy;

    /**
     * 新字段,如果是伪肽段,则本字段为伪肽段对应的原始肽段的Sequence
     */
    String targetSequence;

    /**
     * 对应肽段序列,如果是伪肽段,则为对应的伪肽段的序列
     */
    String sequence;

    /**
     * 对应蛋白质名称
     */
    String proteinName;

    /**
     * 注释
     */
    String annotations;

    /**
     * 注释是否带有中括号,默认不带
     */
    boolean withBrackets = false;

    /**
     * 完整版肽段名称(含修饰基团),如果是伪肽段则为原始的肽段的完整序列而不是伪肽段的完整序列
     */
    String fullName;

    /**
     * 肽段带电量
     */
    Integer precursorCharge;

    /**
     * 新字段,仅记录第一个
     */
    Annotation annotation;

    String cutInfo;

    /**
     * 如果是伪肽段,则本字段代表的是伪肽段中unimod的位置
     * key为unimod在肽段中的位置,位置从0开始计数,value为unimod的Id(参见unimod.obo文件)
     */
    HashMap<Integer, String> unimodMap;

    /**
     * 新字段,原始肽段的序列列表,包含修饰符
     */
    List<AminoAcid> acidList = new ArrayList<>();

    Boolean detecting = true;
    Boolean identifying = false;
    Boolean quantifying = true;

    String features;



}
