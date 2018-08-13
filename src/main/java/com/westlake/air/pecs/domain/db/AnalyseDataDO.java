package com.westlake.air.pecs.domain.db;

import com.westlake.air.pecs.domain.BaseDO;
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
public class AnalyseDataDO extends BaseDO {

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

    //是否命中原始数据,如果原始数据中没有此Transition对应的数据则为false
    Boolean isHit = false;

    //排序后的rt
    Float[] rtArray;

    //一一映射rt的intensity数据
    Float[] intensityArray;
}
