package com.westlake.air.propro.domain.db;

import com.alibaba.fastjson.annotation.JSONField;
import com.westlake.air.propro.domain.BaseDO;
import com.westlake.air.propro.domain.bean.score.FeatureScores;
import io.swagger.annotations.ApiModel;
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
    @JSONField(serialize=false)
    String id;

    @Indexed
    @JSONField(serialize=false)
    String overviewId;

    @Indexed
    String projectId;

    String proteinName;

    @Indexed
    String peptideRef;

    Boolean isUnique;

    //该肽段片段的理论rt值,从标准库中冗余所得
    Float rt;

    //该肽段的前体mz,从标准库中冗余所得
    Float mz;

    //对应的标准库的peptideId
    @JSONField(serialize=false)
    String peptideId;

    //排序后的rt
    @JSONField(serialize=false)
    Float[] rtArray;

    //key为cutInfo, value为对应的intensity值
    @JSONField(serialize=false)
    HashMap<String, Float[]> intensityMap = new HashMap<>();

    //key为cutInfo, value为对应的mz
    HashMap<String, Float> mzMap = new HashMap<>();

    //是否是伪肽段
    Boolean isDecoy = false;

    /**
     * 压缩相关的字段
     */
    //是否处于压缩状态
    @JSONField(serialize=false)
    boolean compressed = false;

    //内存计算时使用的字段,对应rtArray的压缩版本
    @JSONField(serialize=false)
    byte[] convRtArray;

    //内存计算时使用的字段,对应intensityMap的压缩版本
    @JSONField(serialize=false)
    HashMap<String, byte[]> convIntensityMap = new HashMap<>();

    //本肽段在存储文件中的开始位置
    Long startPos;
    //存储在本地的位置块信息,顺序依次为convRtArray,mzMap.keySet()为顺序的各个离子片段的位置,长度为mzMap.size()+1
    List<Integer> posDeltaList;

    //打分相关的字段
    @Indexed
    @JSONField(serialize=false)
    int identifiedStatus = IDENTIFIED_STATUS_NOT_START;

    //最终给出的FDR打分
    @Indexed
    @JSONField(serialize=false)
    Double fdr;

    //最终给出的qValue
    @Indexed
    @JSONField(serialize=false)
    Double qValue;

    @JSONField(serialize=false)
    List<FeatureScores> featureScoresList;

    //最终选出的最佳峰
    @JSONField(serialize=false)
    Double bestRt;

    @JSONField(serialize=false)
    Double intensitySum;
    //最终的定量值

    @JSONField(serialize=false)
    HashMap<String, Double> fragIntMap;
}
