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

    Float rt;

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
