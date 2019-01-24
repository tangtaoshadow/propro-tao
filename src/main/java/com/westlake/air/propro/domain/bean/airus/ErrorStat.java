package com.westlake.air.propro.domain.bean.airus;

import com.westlake.air.propro.domain.bean.score.SimpleFeatureScores;
import lombok.Data;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-13 21:34
 */

@Data
public class ErrorStat {

    List<SimpleFeatureScores> bestFeatureScoresList;

    StatMetrics statMetrics;

    Pi0Est pi0Est;

    @Deprecated
    Double[] cutoff;
    @Deprecated
    Double[] pvalue;
    @Deprecated
    Double[] qvalue;


}

