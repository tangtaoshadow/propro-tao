package com.westlake.air.swathplatform.domain.db;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

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

    /**
     * 前体的荷质比MZ
     */
    String precursorMz;

    /**
     * 本体的荷质比MZ
     */
    String productMz;

    /**
     * 归一化RT
     */
    String normalizedRetentionTime;

    /**
     * 过渡态名称
     */
    String transitionName;

    /**
     * 离子强度
     * library intensity
     */
    String productIonIntensity;

    /**
     * 是否是伪肽段
     */
    Boolean isDecoy;

    /**
     * 对应肽段序列
     */
    String peptideSequence;

    /**
     * 对应蛋白质名称
     */
    String proteinName;

    /**
     * 注释
     */
    String annotation;

    /**
     * 完整版肽段名称(含修饰基团)
     */
    String fullUniModPeptideName;

    /**
     * 前体带电量
     */
    Integer precursorCharge;

    /**
     * 肽段组标签
     */
    String peptideGroupLabel;

    /**
     * 片段类型
     */
    String fragmentType;

    /**
     * 片段带电量
     */
    Integer fragmentCharge;

    /**
     * 片段序列号
     */
    Integer fragmentSeriesNumber;
}
