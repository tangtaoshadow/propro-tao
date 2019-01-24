package com.westlake.air.propro.domain.query;

import lombok.Data;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-04 21:16
 */
@Data
public class ScanIndexQuery extends PageQuery {

    private static final long serialVersionUID = -3258829832460856645L;

    String id;

    String experimentId;

    Integer numStart;

    Integer numEnd;

    Integer msLevel;

    Double rtStart;

    Double rtEnd;

    String rtStr;

    Integer parentNum;

    //前体的荷质比窗口开始位置,已经经过ExperimentDO.overlap参数调整
    Float precursorMzStart;

    //前体的荷质比窗口结束位置,已经经过ExperimentDO.overlap参数调整
    Float precursorMzEnd;

    //根据目标的前体MZ获取相关的窗口
    Float targetPrecursorMz;

    //原始文件中前体的荷质比窗口开始位置,未经过ExperimentDO.overlap参数调整
    Float originalPrecursorMzStart;
    //原始文件中前体的荷质比窗口结束位置,未经过ExperimentDO.overlap参数调整
    Float originalPrecursorMzEnd;

    public ScanIndexQuery() {
    }

    public ScanIndexQuery(String experimentId, Integer msLevel) {
        this.experimentId = experimentId;
        this.msLevel = msLevel;
    }

    public ScanIndexQuery(String experimentId, Integer msLevel, Float precursorMzStart, Float precursorMzEnd) {
        this.experimentId = experimentId;
        this.msLevel = msLevel;
        this.precursorMzStart = precursorMzStart;
        this.precursorMzEnd = precursorMzEnd;
    }

    public ScanIndexQuery(int pageNo, int pageSize) {
        super(pageNo, pageSize);
    }
}
