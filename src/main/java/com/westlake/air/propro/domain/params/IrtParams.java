package com.westlake.air.propro.domain.params;

import com.westlake.air.propro.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.propro.domain.db.LibraryDO;
import lombok.Data;

@Data
public class IrtParams {

    Float mzExtractWindow;

    SigmaSpacing sigmaSpacing = SigmaSpacing.create();

    boolean useLibrary = false;

    LibraryDO library;
}
