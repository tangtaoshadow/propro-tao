package com.westlake.air.pecs.domain.bean.score;

import lombok.Data;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-20 21:44
 */
@Data
public class PecsScore {

    String peptideRef;

    List<FeatureScores> featureScoresList;
}
