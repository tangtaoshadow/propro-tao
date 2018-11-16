package com.westlake.air.pecs.domain.db;

import com.westlake.air.pecs.domain.BaseDO;
import com.westlake.air.pecs.domain.bean.analyse.WindowRang;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

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
    String filePath;

    //转换压缩后的aird的文件名称
    String airdPath;

    String airdIndexPath;

    Boolean hasAirusFile;

    String description;

    String creator = "Admin";

    Date createDate;

    Date lastModifiedDate;

    Float overlap;

    String iRtLibraryId;

    Double slope;

    Double intercept;

    //新增的三个字段,用以支持最新的数据格式,仅支持MzXML格式的文件
    String compressionType;

    String precision;

    List<WindowRang> windowRangs;

}
