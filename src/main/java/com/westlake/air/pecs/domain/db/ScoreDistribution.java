package com.westlake.air.pecs.domain.db;

import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import lombok.Data;

@Data
public class ScoreDistribution {

    String scoreType;

    Boolean biggerIsBetter;

    String[] ranges;

    Integer[] targetCount;

    Integer[] decoyCount;

    public ScoreDistribution(String scoreType) {
        this.scoreType = scoreType;
        this.biggerIsBetter = FeatureScores.ScoreType.getBiggerIsBetter(scoreType);
    }

    public void buildData(String[] ranges, Integer[] targetCount, Integer[] decoyCount) {
        this.ranges = ranges;
        this.targetCount = targetCount;
        this.decoyCount = decoyCount;
    }
}
