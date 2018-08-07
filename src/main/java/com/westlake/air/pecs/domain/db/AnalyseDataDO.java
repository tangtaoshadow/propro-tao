package com.westlake.air.pecs.domain.db;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 15:48
 */
@Data
@Document(collection = "analyseData")
public class AnalyseDataDO {

    @Id
    String id;

    @Indexed
    String overviewId;

    @Indexed
    String proteinName;

    @Indexed
    String peptideRef;

    String transitionId;

    String annotations;

    String cutInfo;

    Integer msLevel;

    Float mz;

    //排序后的rt
    Float[] rtArray;

    //一一映射rt的intensity数据
    Float[] intensityArray;
}
