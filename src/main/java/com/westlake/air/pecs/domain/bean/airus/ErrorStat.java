package com.westlake.air.pecs.domain.bean.airus;

import lombok.Data;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-13 21:34
 */

@Data
public class ErrorStat {
    Double[] cutoff;
    Double[] pvalue;
    Double[] qvalue;
    StatMetrics statMetrics;
    Pi0Est pi0Est;
}

