package com.westlake.air.pecs.domain.db;

import lombok.Data;

@Data
public class ScoreDistribution {

    String scoreType;

    String[] ranges;

    Integer[] numbers;
}
