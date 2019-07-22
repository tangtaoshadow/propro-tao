package com.westlake.air.propro.domain.db.simple;

import com.westlake.air.propro.domain.bean.score.FeatureScores;
import lombok.Data;

import java.util.List;

@Data
public class SimpleScores {

    //肽段名称_带电量,例如:SLMLSYN(UniMod:7)AITHLPAGIFR_3
    String peptideRef;
    //是否是伪肽段
    Boolean isDecoy = false;
    //所有峰组的打分情况
    List<FeatureScores> featureScoresList;
}
