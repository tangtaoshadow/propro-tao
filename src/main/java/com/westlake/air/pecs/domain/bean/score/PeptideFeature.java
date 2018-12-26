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
public class PeptideFeature {
    boolean featureFound;

    PeptideSpectrum peptideSpectrum;

    List<PeakGroup> peakGroupList;

    List<Double> libraryIntensityList;

    HashMap<String, double[]> noise1000Map;

    public PeptideFeature() {
    }

    public PeptideFeature(boolean isFeatureFound) {
        this.featureFound = isFeatureFound;
    }
}
