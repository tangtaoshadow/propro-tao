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

    String name;

    String fileLocation;

    //目前只支持MzML和MzXML
    String fileType;

    String libraryId;

    String libraryName;

    String iRtLibraryId;

    String iRtLibraryName;

    String description;

    String creator = "Admin";

    Date createDate;

    Date lastModifiedDate;

}
