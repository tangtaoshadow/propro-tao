package com.westlake.air.pecs.domain.db;

import com.westlake.air.pecs.domain.BaseDO;
import com.westlake.air.pecs.domain.bean.scanindex.Position;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
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
     * 0 代表Swath Block Index,包含了一个完整的Swath窗口中的所有谱图
     */
    @Indexed
    Integer msLevel;

    @Indexed
    Float rt;

    @Indexed
    String experimentId;

    //key为对应的文件类型, @see PositionType.class
    HashMap<String, Position> positionMap;

    //在AirusData中使用,一个Swath块中所有MS2的rt时间列表
    List<Float> rts = new ArrayList<>();

    //在mzxml中的序号
    Integer num;

    //RT(Time)S格式
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

    public ScanIndexDO(String fileType, Long start, Long end) {
        addPosition(fileType, new Position(start, end));
    }

    public ScanIndexDO(int num,String fileType, Long start, Long end) {
        this.num = num;
        addPosition(fileType, new Position(start, end));
    }

    public void setRtStr(String rtStr) {
        this.rtStr = rtStr;
        if (rtStr.startsWith("PT") && rtStr.endsWith("S")) {
            this.rt = Float.parseFloat(rtStr.substring(2, rtStr.length() - 1));
        }
    }

    public void addPosition(String key, Position position){
        if(positionMap == null){
            positionMap = new HashMap<>();
        }

        positionMap.put(key, position);
    }

    public void removePosition(String key){
        if(positionMap == null){
            return;
        }

        positionMap.remove(key);
    }

    public Long getPosStart(String key){
        if(positionMap != null){
            Position pos = positionMap.get(key);
            if(pos != null){
                return pos.getStart();
            }
        }
        return null;
    }

    public Long getPosEnd(String key){
        if(positionMap != null){
            Position pos = positionMap.get(key);
            if(pos != null){
                return pos.getEnd();
            }
        }
        return null;
    }

    public void setPosStart(String key, Long startValue){
        Position position = null;
        if(positionMap == null){
            positionMap = new HashMap<>();
            position = new Position(startValue, null);
            positionMap.put(key, position);
        }else{
            position = positionMap.get(key);
            position.setStart(startValue);
        }
    }

    public void setPosEnd(String key, Long endValue){
        Position position = null;
        if(positionMap == null){
            positionMap = new HashMap<>();
            position = new Position(null, endValue);
            positionMap.put(key, position);
        }else{
            position = positionMap.get(key);
            position.setEnd(endValue);
        }
    }
}
