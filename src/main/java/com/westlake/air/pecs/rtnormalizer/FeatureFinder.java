package com.westlake.air.pecs.rtnormalizer;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.domain.bean.math.BisectionLowHigh;
import com.westlake.air.pecs.domain.bean.score.IntensityRtLeftRtRightPairs;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairs;
import com.westlake.air.pecs.domain.bean.score.ExperimentFeature;
import com.westlake.air.pecs.utils.MathUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-01 22：42
 */
@Component("featureFinder")
public class FeatureFinder {

    public List<List<ExperimentFeature>> findFeatures(List<RtIntensityPairs> chromatograms, List<RtIntensityPairs> pickedChroms, List<IntensityRtLeftRtRightPairs> intensityLeftRight){

        int[] chrPeakIndex;

        List<List<ExperimentFeature>> experimentFeatures = new ArrayList<>();
        while (true) {
            chrPeakIndex = findLargestPeak(pickedChroms);
            if (chrPeakIndex[0] == -1 || chrPeakIndex[1] == -1) {
                break;
            }

            float bestLeft = intensityLeftRight.get(chrPeakIndex[0]).getRtLeftArray()[chrPeakIndex[1]];
            float bestRight = intensityLeftRight.get(chrPeakIndex[0]).getRtRightArray()[chrPeakIndex[1]];
            float peakApex = pickedChroms.get(chrPeakIndex[0]).getRtArray()[chrPeakIndex[1]];

            RtIntensityPairs rtInt = pickedChroms.get(chrPeakIndex[0]);
            Float[] intensityArray = rtInt.getIntensityArray();
            intensityArray[chrPeakIndex[1]] = 0.0f;
            rtInt.setIntensityArray(intensityArray);
            pickedChroms.set(chrPeakIndex[0], rtInt);


            RtIntensityPairs chromatogram;
            RtIntensityPairs masterChromatogram = new RtIntensityPairs(chromatograms.get(chrPeakIndex[0]));

            float totalXic = 0.0f;

            List<ExperimentFeature> mrmFeature = new ArrayList<>();
            for (int i = 0; i < chromatograms.size(); i++) {
                chromatogram = chromatograms.get(i);
                for (float intensity : chromatogram.getIntensityArray()) {
                    totalXic += intensity;
                }
                RtIntensityPairs usedChromatogram = raster(chromatogram, masterChromatogram, bestLeft, bestRight);
                ExperimentFeature feature = calculatePeakApexInt(usedChromatogram, bestLeft, bestRight, peakApex);
                mrmFeature.add(feature);
            }
            float sum = 0.0f;
            for(ExperimentFeature feature: mrmFeature){
                sum += feature.getIntensity();
                feature.setTotalXic(totalXic);
            }
            if(sum > 0){
                experimentFeatures.add(mrmFeature);
            }
            if(sum > 0 && sum / totalXic < Constants.STOP_AFTER_INTENSITY_RATIO){
                break;
            }
        }
        checkOverlappingFeatures(experimentFeatures);

        return experimentFeatures;
    }

    private int[] findLargestPeak(List<RtIntensityPairs> pickedChroms){
        float largest = 0.0f;
        int[] chrPeakIndex = new int[2];
        chrPeakIndex[0] = -1;
        chrPeakIndex[1] = -1;
        for(int i = 0; i < pickedChroms.size(); i++){
            for(int j = 0; j < pickedChroms.get(0).getRtArray().length; j++){
                if(pickedChroms.get(i).getIntensityArray()[j] > largest){
                    largest = pickedChroms.get(i).getIntensityArray()[j];
                    chrPeakIndex[0] = i;
                    chrPeakIndex[1] = j;
                }
            }
        }
        return chrPeakIndex;
    }

    /**
     * resampleChromatogram
     * 从chromatogram中得到intensity存进masterChromatogram
     * @param chromatogram normal chromatogram
     * @param masterChromatogram chromatogram with max peak
     * @param leftBoundary bestLeft rt of max peak
     * @param rightBoundary bestRight rt of max peak
     * @return masterChromatogram with both rt and intensity
     */
    private RtIntensityPairs raster(RtIntensityPairs chromatogram, RtIntensityPairs masterChromatogram, float leftBoundary, float rightBoundary){
        int chromatogramLeft, chromatogramRight;
        int masterChromLeft, masterChromRight;
        chromatogramLeft = MathUtil.bisection(chromatogram, leftBoundary).getLow();
        chromatogramRight = MathUtil.bisection(chromatogram, rightBoundary).getHigh();
        masterChromLeft = MathUtil.bisection(masterChromatogram, leftBoundary).getLow();
        masterChromRight = MathUtil.bisection(masterChromatogram, rightBoundary).getHigh();
        int masterChromLeftStatic = masterChromLeft;

        Float[] rt = new Float[masterChromRight - masterChromLeft + 1];
        Float[] intensity = new Float[masterChromRight - masterChromLeft + 1];
        float distLeft, distRight;

        for(int i=0; i<rt.length; i++){
            rt[i] = 0f;
            intensity[i] = 0f;
        }

        //set rt
        for(int i=masterChromLeft; i<=masterChromRight; i++){
            rt[i - masterChromLeftStatic] = masterChromatogram.getRtArray()[i];
        }

        //set intensity
        while(chromatogramLeft <= chromatogramRight && chromatogram.getRtArray()[chromatogramLeft] < masterChromatogram.getRtArray()[masterChromLeft]){
            intensity[masterChromLeft-masterChromLeftStatic] += chromatogram.getIntensityArray()[chromatogramLeft];
            chromatogramLeft ++;
        }
        while (chromatogramLeft <= chromatogramRight){
            while (masterChromLeft <= masterChromRight && chromatogram.getRtArray()[chromatogramLeft] > masterChromatogram.getRtArray()[masterChromLeft]){
                masterChromLeft ++;
            }
            if(masterChromLeft != masterChromLeftStatic){
                masterChromLeft--;
            }
            if(masterChromLeft == masterChromRight){
                break;
            }
            distLeft = Math.abs(chromatogram.getRtArray()[chromatogramLeft] - masterChromatogram.getRtArray()[masterChromLeft]);
            distRight = Math.abs(chromatogram.getRtArray()[chromatogramLeft] - masterChromatogram.getRtArray()[masterChromLeft + 1]);

            intensity[masterChromLeft - masterChromLeftStatic] += chromatogram.getIntensityArray()[chromatogramLeft] * distRight / (distRight + distLeft);
            intensity[masterChromLeft - masterChromLeftStatic + 1] += chromatogram.getIntensityArray()[chromatogramLeft] * distLeft / (distRight + distLeft);

            chromatogramLeft ++;
        }
        while (chromatogramLeft <= chromatogramRight){
            intensity[masterChromLeft - masterChromLeftStatic + 1] += chromatogram.getIntensityArray()[chromatogramLeft];
            chromatogramLeft ++;
        }

        return new RtIntensityPairs(rt, intensity);
    }

    /**
     *
     * @param chromatogram masterChromatogram(same rt)
     * @param bestLeft bestLeft rt of max peak
     * @param bestRight bestRight rt of max peak
     * @param peakApexRt rt of max peak
     * @return
     */
    private ExperimentFeature calculatePeakApexInt(RtIntensityPairs chromatogram, float bestLeft, float bestRight, float peakApexRt){
        Float[] rtArray = chromatogram.getRtArray();
        Float[] intArray = chromatogram.getIntensityArray();

        int peakNum = 0;
        float deltaRt, interpolIntensity;
//        float intensityIntegral = 0.0f;
        float peakApexDist = Math.abs(rtArray[0] - peakApexRt);
        float peakApexInt = 0.0f;
        List<Float> hullRt = new ArrayList<>();
        List<Float> hullInt = new ArrayList<>();
        float rtSum = 0.0f, intSum = 0.0f;
        for(int i = 0; i<chromatogram.getRtArray().length; i++){
            if(rtArray[i] > bestLeft && rtArray[i] < bestRight){
//                if(peakNum == 0 && i != 0){
//                    deltaRt = rtArray[i] - bestLeft;
//                    interpolIntensity = linearInterpolate(bestLeft, rtArray[i-1], rtArray[i], intArray[i-1], intArray[i]);
//                    intensityIntegral += (interpolIntensity + intArray[i]) / 2.0f * deltaRt;
//                }
//                if(peakNum > 0){
//                    deltaRt = rtArray[i] - rtArray[i-1];
//                    intensityIntegral += (intArray[i-1] + intArray[i]) / 2.0f * deltaRt;
//                }
                hullRt.add(rtArray[i]);
                hullInt.add(intArray[i]);
                if(Math.abs(rtArray[i] - peakApexRt) <= peakApexDist){
                    peakApexDist = Math.abs(rtArray[i] - peakApexRt);
                    peakApexInt = intArray[i];
                }
                rtSum += rtArray[i];
                intSum += intArray[i];

                peakNum ++;
//            }else if(peakNum > 0){
//                deltaRt = bestRight - rtArray[i-1];
//                interpolIntensity = linearInterpolate(bestRight, rtArray[i-1], rtArray[i], intArray[i-1], intArray[i]);
//                intensityIntegral += (intArray[i-1] + interpolIntensity)/2.0 * deltaRt;
//                break;
//            }
            }
        }
        ExperimentFeature feature = new ExperimentFeature();
        feature.setRt(peakApexRt);
        feature.setIntensity(intSum);
        feature.setPeakApexInt(peakApexInt);
        feature.setBestLeft(bestLeft);
        feature.setBestRight(bestRight);
        feature.setHullRt(hullRt);
        feature.setHullInt(hullInt);

        return feature;
    }


    private float linearInterpolate(float x, float x0, float x1, float y0, float y1){

        return y0 + (x - x0) * (y1 - y0) / (x1 - x0);
    }

    private void checkOverlappingFeatures(List<List<ExperimentFeature>> experimentFeatures){
        boolean skip;
        int i = 0;
        while (i < experimentFeatures.size()){
            skip = false;
            for(int j=0; j<i; j++){
                if(experimentFeatures.get(i).get(0).getBestLeft() >= experimentFeatures.get(j).get(0).getBestLeft() &&
                        experimentFeatures.get(i).get(0).getBestRight() <= experimentFeatures.get(j).get(0).getBestRight()){
                    skip = true;
                }
            }
            if(skip){
                experimentFeatures.remove(i);
                i--;
            }
            i++;
        }
    }


}
