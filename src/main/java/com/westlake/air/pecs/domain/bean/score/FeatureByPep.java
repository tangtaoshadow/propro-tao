package com.westlake.air.pecs.domain.bean.score;

import com.westlake.air.pecs.domain.bean.analyse.PeptideSpectrum;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import lombok.Data;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-20 16:08
 */
@Data
public class FeatureByPep {
    boolean featureFound;

    PeptideSpectrum peptideSpectrum;

    List<PeakGroup> peakGroupFeatureList;

    List<Double> libraryIntensityList;

    HashMap<String, double[]> noise1000Map;

    public FeatureByPep() {
    }

    public FeatureByPep(boolean isFeatureFound) {
        this.featureFound = isFeatureFound;
    }
}
