package com.westlake.air.pecs.domain.bean.airus;

import lombok.Data;

import java.util.HashMap;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-19 13:24
 */

@Data
public class FinalResult {

    HashMap classifierTable;

    ErrorStat finalErrorTable;

    ErrorStat summaryErrorTable;

    ErrorStat allInfo;
}
