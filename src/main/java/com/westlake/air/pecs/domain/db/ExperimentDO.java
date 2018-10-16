package com.westlake.air.pecs.domain.db;

import com.westlake.air.pecs.domain.BaseDO;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-04 13:46
 */
@Data
@Document(collection = "experiment")
public class ExperimentDO extends BaseDO {

    private static final long serialVersionUID = -3258829839160856625L;

    @Id
    String id;

    //必填
    String name;

    //必填
    String fileLocation;

    String description;

    String creator = "Admin";

    Date createDate;

    Date lastModifiedDate;

    Float overlap;

    Double slope;

    Double intercept;

    //新增的三个字段,用以支持最新的数据格式,仅支持MzXML格式的文件
    String compressionType;

    String precision;

}
