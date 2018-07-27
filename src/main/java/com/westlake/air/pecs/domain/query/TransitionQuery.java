package com.westlake.air.pecs.domain.query;

import lombok.Data;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-06 19:16
 */
@Data
public class TransitionQuery extends PageQuery {

    String id;

    String libraryId;

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
    Double rt;

    /**
     * 过渡态名称
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
     * 对应肽段序列
     */
    String sequence;

    /**
     * 对应蛋白质名称
     */
    String proteinName;

    /**
     * 完整版肽段名称(含修饰基团)
     */
    String fullName;

    Double precursorMzStart;

    Double precursorMzEnd;

    public TransitionQuery() {
    }

    public TransitionQuery(String libraryId) {
        this.libraryId = libraryId;
    }
}
