package com.westlake.air.pecs.domain.db;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.awt.datatransfer.FlavorListener;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-05 16:36
 */
@Data
@Document(collection = "scanIndex")
public class ScanIndexDO {

    @Id
    String id;

    @Indexed
    Integer msLevel;

    @Indexed
    Float rt;

    String experimentId;

    //在源文件中的起始位置
    Long start;

    //在源文件中的结束位置
    Long end;

    Integer num;

    String rtStr;

    Integer parentNum;

    //前体的荷质比
    Float precursorMz;

    //前体的荷质比窗口开始位置
    Float precursorMzStart;

    //前体的荷质比窗口结束位置
    Float precursorMzEnd;

    //前体的荷质比窗口
    Float windowWideness;

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
