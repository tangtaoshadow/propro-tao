package com.westlake.air.pecs.domain.bean.airus;

import lombok.Data;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-17 17:59
 */

@Data
public class LDALearn {
    Double[] topTestTargetScores;
    Double[] topTestDecoyScores;
    Double[] weights;
}
