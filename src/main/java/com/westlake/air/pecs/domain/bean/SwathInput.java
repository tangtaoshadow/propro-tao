package com.westlake.air.pecs.domain.bean;

import com.westlake.air.pecs.domain.db.ExperimentDO;
import lombok.Data;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-29 13:55
 */
@Data
public class SwathInput {

    ExperimentDO experimentDO;

    String iRtLibraryId;

    String libraryId;

    Float mzExtractWindow;

    Float rtExtractWindow;

    /**
     * 默认为30
     */
    Float sigma;

    /**
     * 默认为
     */
    Float space;

    /**
     * 流程的创建人
     */
    String creator;

    /**
     * 必填,默认为2
     * 0代表同时卷积MS1和MS2, 1代表卷积MS1, 2代表卷积MS2
     */
    Integer buildType;

}
