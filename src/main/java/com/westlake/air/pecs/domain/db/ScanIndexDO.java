package com.westlake.air.pecs.domain.db;

import com.westlake.air.pecs.domain.BaseDO;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-05 16:36
 */
@Data
@Document(collection = "scanIndex")
public class ScanIndexDO extends BaseDO {

    @Id
    String id;

    /**
     * 0 代表Swath Block Index
     */
    @Indexed
    Integer msLevel;

    @Indexed
    Float rt;

    @Indexed
    String experimentId;

    //在MzXML源文件中的起始位置
    Long start;

    //在MzXML源文件中的结束位置
    Long end;

    //在AirusData源文件中的起始位置
    Long start2;

    //在AirusData源文件中的结束位置
    Long end2;

    //在AirusData中使用,一个Swath块中所有MS2的rt时间列表
    List<Float> rts = new ArrayList<>();

    Integer num;

    String rtStr;

    Integer parentNum;

    //前体的荷质比
    Float precursorMz;

    //前体的荷质比窗口开始位置,已经经过ExperimentDO.overlap参数调整
    Float precursorMzStart;

    //前体的荷质比窗口结束位置,已经经过ExperimentDO.overlap参数调整
    Float precursorMzEnd;

    //原始文件中前体的荷质比窗口开始位置,未经过ExperimentDO.overlap参数调整
    Float originalPrecursorMzStart;
    //原始文件中前体的荷质比窗口结束位置,未经过ExperimentDO.overlap参数调整
    Float originalPrecursorMzEnd;

    //前体的荷质比窗口
    Float windowWideness;

    //原始文件中前体的窗口大小,未经过ExperimentDO.overlap参数调整
    Float originalWindowWideness;

    public ScanIndexDO() {}

    public ScanIndexDO(Long start, Long end) {
        this.start = start;
        this.end = end;
    }

    public ScanIndexDO(int num, Long start, Long end) {
        this.num = num;
        this.start = start;
        this.end = end;
    }

    public void setRtStr(String rtStr) {
        this.rtStr = rtStr;
        if (rtStr.startsWith("PT") && rtStr.endsWith("S")) {
            this.rt = Float.parseFloat(rtStr.substring(2, rtStr.length() - 1));
        }
    }
}
