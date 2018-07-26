package com.westlake.air.pecs.domain.vo;

import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import lombok.Data;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-26 13:24
 */
@Data
public class AnalyseDataVO extends AnalyseDataDO {

    String fullName;

    String annotations;

    public AnalyseDataVO(){}

    public AnalyseDataVO(AnalyseDataDO analyseDataDO){
        this.setId(analyseDataDO.getId());
        this.setTransitionId(analyseDataDO.getTransitionId());
        this.setIntensityArray(analyseDataDO.getIntensityArray());
        this.setRtArray(analyseDataDO.getRtArray());
        this.setMsLevel(analyseDataDO.getMsLevel());
        this.setMz(analyseDataDO.getMz());
        this.setOverviewId(analyseDataDO.getOverviewId());
    }

}
