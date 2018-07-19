package com.westlake.air.swathplatform.domain.bean;

import lombok.Data;

import java.util.HashMap;
import java.util.TreeMap;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-18 15:52
 */
@Data
public class ExperimentResult {

    String experimentId;

    String libraryId;

    double rtExtractWindow;

    double mzExtractWindow;

    HashMap<Double, TreeMap<Double, Double>> ms1Map;

    HashMap<Double, TreeMap<Double, Double>> ms2Map;
}
