package com.westlake.air.swathplatform.domain.db;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

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
    Double rt;

    String experimentId;

    Long start;

    Long end;

    Integer num;

    String rtStr;

    Integer parentNum;

    public ScanIndexDO() {
    }

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
            this.rt = Double.parseDouble(rtStr.substring(2, rtStr.length() - 1));
        }
    }
}
