package com.westlake.air.pecs.domain.db.simple;

import com.westlake.air.pecs.domain.bean.scanindex.Position;
import lombok.Data;

import java.util.HashMap;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 20:46
 */
@Data
public class SimpleScanIndex {

    //原始谱图对应的rt时间
    Float rt;

    //key为位置的类型,详见PositionType类,value为对应的位置
    HashMap<String, Position> positionMap;

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

}
