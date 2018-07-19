package com.westlake.air.swathplatform.domain.db;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 16:20
 */
@Data
@Document(collection = "analyseRecord")
public class AnalyseRecordDO {

    @Id
    String id;

    String expId;

    String libraryId;

    Double rtExtractWindowForMS1;

    Double rtExtractWindowForMS2;

    Double mzExtractWindowForMS1;

    Double mzExtractWindowForMS2;

    Date createDate;
}
