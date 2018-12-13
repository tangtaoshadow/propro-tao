package com.westlake.air.pecs.domain.db;

import com.westlake.air.pecs.domain.BaseDO;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 15:48
 */
@Data
@Document(collection = "analyseData")
public class AnalyseDataDO extends BaseDO {

    @Id
    String id;

    @Indexed
    String overviewId;

    @Indexed
    String proteinName;

    @Indexed
    String peptideRef;

    //该肽段片段的理论rt值,从标准库中冗余所得
    Float rt;

    //该肽段的前体mz,从标准库中冗余所得
    Float mz;

    //对应的标准库的peptideId
    String peptideId;

    String annotations;

    //排序后的rt
    Float[] rtArray;

    /**
     * key为cutInfo, value为对应的intensity值
     */
    HashMap<String, Float[]> intensityMap = new HashMap<>();

    /**
     * key为cutInfo, value为对应的mz
     */
    HashMap<String, Float> mzMap = new HashMap<>();

    //是否命中原始数据,如果原始数据中没有此Transition对应的数据则为false
    Boolean isHit = false;

    //是否是伪肽段
    Boolean isDecoy = false;

    //修饰基团的位置和基团编号
    HashMap<Integer, String> unimodMap;

    /**
     * 内存计算时使用的字段,对应rtArray的压缩版本
     */
    byte[] convRtArray;

    /**
     * 内存计算时使用的字段,对应intensityMap的压缩版本
     */
    HashMap<String, byte[]> convIntensityMap = new HashMap<>();
}
