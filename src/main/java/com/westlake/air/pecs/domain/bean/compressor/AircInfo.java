package com.westlake.air.pecs.domain.bean.compressor;

import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.AnalyseOverviewDO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AircInfo {

    AnalyseOverviewDO overview;

    /**
     * the whole convolution data list
     */
    List<AnalyseDataDO> dataList = new ArrayList<>();
}
