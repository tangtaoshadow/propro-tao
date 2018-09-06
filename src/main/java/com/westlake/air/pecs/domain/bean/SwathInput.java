package com.westlake.air.pecs.domain.bean;

import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import lombok.Data;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-29 13:55
 */
@Data
public class SwathInput {

    ExperimentDO experimentDO;

    String overviewId;

    String iRtLibraryId;

    String libraryId;

    Float mzExtractWindow;

    Float rtExtractWindow;

    /**
     * iRT求得的斜率和截距
     */
    SlopeIntercept slopeIntercept;

    SigmaSpacing sigmaSpacing = SigmaSpacing.create();

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
