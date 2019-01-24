package com.westlake.air.propro.domain.db.simple;

import com.westlake.air.propro.domain.bean.score.FeatureScores;
import lombok.Data;

import java.util.List;

@Data
public class SimpleScores {

//    String proteinName;
    //肽段名称_带电量,例如:SLMLSYN(UniMod:7)AITHLPAGIFR_3
    String peptideRef;
    //是否是伪肽段
    Boolean isDecoy = false;
    //最后算法确定的最佳RT
    Double bestRt;
    //最后得出的定量值
    Double intensitySum;
    //所有峰组的打分情况
    List<FeatureScores> featureScoresList;
}
