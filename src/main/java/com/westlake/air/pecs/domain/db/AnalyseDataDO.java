package com.westlake.air.pecs.domain.db;

import com.westlake.air.pecs.domain.BaseDO;
import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 15:48
 */
@Data
@Document(collection = "analyseData")
public class AnalyseDataDO extends BaseDO {

    //鉴定成功
    public static Integer IDENTIFIED_STATUS_SUCCESS = 0;
    //不满足基础条件
    public static Integer IDENTIFIED_STATUS_NO_FIT = 1;
    //鉴定未成功
    public static Integer IDENTIFIED_STATUS_UNKNOWN = 2;
    //尚未鉴定
    public static Integer IDENTIFIED_STATUS_NOT_START = 3;
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

    //对应的标记
    String annotations;

    //排序后的rt
    Float[] rtArray;

    //key为cutInfo, value为对应的intensity值
    HashMap<String, Float[]> intensityMap = new HashMap<>();

    //key为cutInfo, value为对应的mz
    HashMap<String, Float> mzMap = new HashMap<>();

    //是否是伪肽段
    Boolean isDecoy = false;

    //压缩相关的字段
    /**
     * 是否处于压缩状态
     */
    boolean compressed = false;

    //内存计算时使用的字段,对应rtArray的压缩版本
    byte[] convRtArray;

    //内存计算时使用的字段,对应intensityMap的压缩版本
    HashMap<String, byte[]> convIntensityMap = new HashMap<>();

    //打分相关的字段
    @Indexed
    int identifiedStatus = IDENTIFIED_STATUS_NOT_START;

    @Indexed
    Double fdr;

    List<FeatureScores> featureScoresList;

    Double bestRt;


}
