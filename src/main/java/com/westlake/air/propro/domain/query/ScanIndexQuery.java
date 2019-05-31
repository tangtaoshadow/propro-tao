package com.westlake.air.propro.domain.query;

import lombok.Data;

/**
 * Time: 2018-07-04 21:16
 * @author James Lu MiaoShan
 */
@Data
public class ScanIndexQuery extends PageQuery {

    private static final long serialVersionUID = -3258829832460856645L;

    String id;

    String expId;

    Integer numStart;

    Integer numEnd;

    Integer msLevel;

    Double rtStart;

    Double rtEnd;

    Float rt;

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

    public ScanIndexQuery(String expId, Integer msLevel) {
        this.expId = expId;
        this.msLevel = msLevel;
    }

    public ScanIndexQuery(String expId, Integer msLevel, Float precursorMzStart, Float precursorMzEnd) {
        this.expId = expId;
        this.msLevel = msLevel;
        this.precursorMzStart = precursorMzStart;
        this.precursorMzEnd = precursorMzEnd;
    }

    public ScanIndexQuery(int pageNo, int pageSize) {
        super(pageNo, pageSize);
    }
}
