package com.westlake.air.pecs.domain.db;

import com.westlake.air.pecs.domain.BaseDO;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 16:20
 */
@Data
@Document(collection = "analyseOverview")
public class AnalyseOverviewDO extends BaseDO {

    @Id
    String id;

    String expId;

    String expName;

    String name;

    /**
     * 标准库ID
     */
    String libraryId;

    String libraryName;

    /**
     * 校准库ID
     */
    String iRtLibraryId;

    String iRtLibraryName;

    //MS1是全光谱扫描的.没有RTwindow的参数,MS2有
    Float rtExtractWindow;

    //MS1和MS2的mzWindow是共用同一个的
    Float mzExtractWindow;

    String creator = "Admin";

    Date createDate;

    Date lastModifiedDate;

}
