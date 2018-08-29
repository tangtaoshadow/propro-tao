package com.westlake.air.pecs.domain.bean.score;

import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairs;
import lombok.Data;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-20 16:08
 */
@Data
public class FeatureByPep {
    boolean featureFound;

    List<RtIntensityPairs> rtIntensityPairsOriginList;

    List<List<ExperimentFeature>> experimentFeatures;

    List<Float> libraryIntensityList;

    List<double[]> noise1000List;
}
