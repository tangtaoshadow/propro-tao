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

    //特定字段,在msLevel=0的时候使用,在Aird格式文件中使用,一个Swath块中所有MS2的rt时间列表
    List<Float> rts;
    //特定字段,在msLevel=0的时候使用,在压缩文件中存储mz数组的长度以及存储intensity数组的长度,mz长度及intensity长度交替存入
    List<Integer> blockSizes;

    public ScanIndexDO() {}

    public ScanIndexDO(int num,String fileType, Long start, Long delta) {
        this.num = num;
        addPosition(fileType, new Position(start, delta));
    }

    public void setRtStr(String rtStr) {
        if(rtStr == null){
            this.rtStr = null;
            return;
        }
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
                return pos.getStart() + pos.getDelta();
            }
        }
        return null;
    }

    public Long getPosDelta(String key){
        if(positionMap != null){
            Position pos = positionMap.get(key);
            if(pos != null){
                return pos.getDelta();
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
            if(position == null){
                position = new Position(startValue, null);
                positionMap.put(key, position);
            }else{
                position.setStart(startValue);
            }
        }
    }

    public void setPosDelta(String key, Long deltaValue){
        Position position = null;
        if(positionMap == null){
            positionMap = new HashMap<>();
            position = new Position(null, deltaValue);
            positionMap.put(key, position);
        }else{
            position = positionMap.get(key);
            position.setDelta(deltaValue);
        }
    }

    public void setPosEnd(String key, Long endValue){
        Position position = null;
        if(positionMap != null){
            position = positionMap.get(key);
            position.setDelta(endValue - position.getStart());
        }
    }
}
