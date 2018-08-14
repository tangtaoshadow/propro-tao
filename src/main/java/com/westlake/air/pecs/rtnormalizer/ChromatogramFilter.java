package com.westlake.air.pecs.rtnormalizer;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairs;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.utils.MathUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-12 07:04
 */
public class ChromatogramFilter {

    public List<RtIntensityPairs> pickChromatogramListByRt(List<RtIntensityPairs> chromatogramList, float pepRefRt, SlopeIntercept slopeIntercept){
        List<RtIntensityPairs> pickedChromatogramList = new ArrayList<>();
        for(RtIntensityPairs rtIntensityPairs: chromatogramList){
            pickedChromatogramList.add(pickChromatogramByRt(rtIntensityPairs, pepRefRt, slopeIntercept));
        }
        return pickedChromatogramList;
    }

    private RtIntensityPairs pickChromatogramByRt(RtIntensityPairs chromatogram, float pepRefRt, SlopeIntercept slopeIntercept){
        SlopeIntercept invertedSlopeIntercept = MathUtil.trafoInverter(slopeIntercept);
        float normalizedExperimentRt = MathUtil.trafoApplier(invertedSlopeIntercept, pepRefRt);
        float rtMax = normalizedExperimentRt + Constants.RT_EXTRACTION_WINDOW;
        float rtMin = normalizedExperimentRt - Constants.RT_EXTRACTION_WINDOW;
        Float[] rtArray = chromatogram.getRtArray();
        Float[] intArray = chromatogram.getIntensityArray();
        List<Float> rtListPicked = new ArrayList<>();
        List<Float> intListPicked = new ArrayList<>();
        for(int i=0; i<rtArray.length; i++){
            if(rtArray[i] >= rtMin && rtArray[i] <= rtMax){
                rtListPicked.add(rtArray[i]);
                intListPicked.add(intArray[i]);
            }
        }
        Float[] rtArrayPicked = rtListPicked.toArray(new Float[rtListPicked.size()]);
        Float[] intArrayPicked = intListPicked.toArray(new Float[intListPicked.size()]);

        chromatogram.setRtArray(rtArrayPicked);
        chromatogram.setIntensityArray(intArrayPicked);
        return chromatogram;
    }
}
