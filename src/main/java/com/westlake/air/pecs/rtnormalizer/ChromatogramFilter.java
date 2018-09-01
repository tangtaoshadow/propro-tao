package com.westlake.air.pecs.rtnormalizer;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairs;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.utils.MathUtil;
import com.westlake.air.pecs.utils.ScoreUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-12 07:04
 */
@Component("chromatogramFilter")
public class ChromatogramFilter {

    public RtIntensityPairsDouble pickChromatogramByRt(RtIntensityPairsDouble chromatogram, double pepRefRt, SlopeIntercept slopeIntercept){
        SlopeIntercept invertedSlopeIntercept = ScoreUtil.trafoInverter(slopeIntercept);
        double normalizedExperimentRt = ScoreUtil.trafoApplier(invertedSlopeIntercept, pepRefRt);
        double rtMax = normalizedExperimentRt + Constants.RT_EXTRACTION_WINDOW;
        double rtMin = normalizedExperimentRt - Constants.RT_EXTRACTION_WINDOW;
        Double[] rtArray = chromatogram.getRtArray();
        Double[] intArray = chromatogram.getIntensityArray();
        List<Double> rtListPicked = new ArrayList<>();
        List<Double> intListPicked = new ArrayList<>();
        for(int i=0; i<rtArray.length; i++){
            if(rtArray[i] >= rtMin && rtArray[i] <= rtMax){
                rtListPicked.add(rtArray[i]);
                intListPicked.add(intArray[i]);
            }
        }
        Double[] rtArrayPicked = rtListPicked.toArray(new Double[0]);
        Double[] intArrayPicked = intListPicked.toArray(new Double[0]);

        chromatogram.setRtArray(rtArrayPicked);
        chromatogram.setIntensityArray(intArrayPicked);
        return chromatogram;
    }
}
