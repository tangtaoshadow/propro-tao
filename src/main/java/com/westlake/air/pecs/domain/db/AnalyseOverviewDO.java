package com.westlake.air.pecs.domain.db;

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
public class AnalyseOverviewDO {

    @Id
    String id;

    String expId;

    String expName;

    String name;

    String sLibraryId;

    String sLibraryName;

    String vLibraryId;

    String vLibraryName;

    Double rtExtractWindowForMS1;

    Double rtExtractWindowForMS2;

    Double mzExtractWindowForMS1;

    Double mzExtractWindowForMS2;

    String creator;

    Date createDate;
}
