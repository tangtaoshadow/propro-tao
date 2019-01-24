package com.westlake.air.propro.domain.bean.compressor;

import com.westlake.air.propro.domain.db.AnalyseDataDO;
import com.westlake.air.propro.domain.db.AnalyseOverviewDO;
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
