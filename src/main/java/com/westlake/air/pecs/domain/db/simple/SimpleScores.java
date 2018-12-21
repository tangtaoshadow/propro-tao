package com.westlake.air.pecs.domain.db.simple;

import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import lombok.Data;

import java.util.List;

@Data
public class SimpleScores {

//    String proteinName;

    String peptideRef;

    Boolean isDecoy = false;

    Double bestRt;

    List<FeatureScores> featureScoresList;
}
