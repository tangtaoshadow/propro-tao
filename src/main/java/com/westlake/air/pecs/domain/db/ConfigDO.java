package com.westlake.air.pecs.domain.db;

import com.westlake.air.pecs.domain.BaseDO;
import lombok.Data;

import java.util.Date;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-14 10:04
 */
@Data
public class ConfigDO extends BaseDO {

    String id;

    String experimentFilePath;

    String libraryFilePath;

    String suffixForCompressorFile = "_compressed";

    String oldExperimentFilePath;

    Date createDate;

    Date lastModifiedDate;
}
