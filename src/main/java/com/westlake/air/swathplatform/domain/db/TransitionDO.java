package com.westlake.air.swathplatform.domain.db;

import com.westlake.air.swathplatform.domain.bean.Annotation;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

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
     * 前体的荷质比MZ
     */
    Double precursorMz;

    /**
     * 本体的荷质比MZ
     */
    Double productMz;

    /**
     * 归一化RT
     */
    Double normalizedRetentionTime;

    /**
     * 过渡态名称,如果是伪肽段,则为包含了原始肽段的属性
     */
    String transitionName;

    /**
     * 离子强度
     * library intensity
     */
    Double productIonIntensity;

    /**
     * 是否是伪肽段
     */
    Boolean isDecoy;

    /**
     * 对应肽段序列,如果是伪肽段,则为对应的伪肽段的序列
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
     * 完整版肽段名称(含修饰基团),如果是伪肽段则为原始的肽段的完整序列而不是伪肽段的完整序列
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
     * fragment详情列表
     */
    List<Annotation> annotations;


}
