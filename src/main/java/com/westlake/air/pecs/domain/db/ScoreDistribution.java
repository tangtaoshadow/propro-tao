package com.westlake.air.pecs.domain.db;

import lombok.Data;

import java.util.TreeMap;

@Data
public class ScoreDistribution {

    String scoreType;

    String[] ranges;

    Integer[] targetCount;

    Integer[] decoyCount;


    public ScoreDistribution(String scoreType) {
        this.scoreType = scoreType;
    }

    public void buildData(String[] ranges, Integer[] targetCount, Integer[] decoyCount) {
        this.ranges = ranges;
        this.targetCount = targetCount;
        this.decoyCount = decoyCount;
    }
}
