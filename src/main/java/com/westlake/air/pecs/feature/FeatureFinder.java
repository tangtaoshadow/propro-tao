package com.westlake.air.pecs.feature;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.pecs.domain.bean.score.IntensityRtLeftRtRightPairs;
import com.westlake.air.pecs.domain.bean.score.ExperimentFeature;
import com.westlake.air.pecs.domain.bean.score.PeakGroup;
import com.westlake.air.pecs.utils.ConvolutionUtil;
import com.westlake.air.pecs.utils.MathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-01 22：42
 */
@Component("featureFinder")
public class FeatureFinder {

    public final Logger logger = LoggerFactory.getLogger(FeatureFinder.class);

    /**
     * 1）找出pickedChrom下的最高峰，得到对应rt和rtLeft、rtRight
     * 2）将pickedChrom最高峰intensity设为0
     * 3）将最高峰对应的chromatogram设为masterChromatogram
     * 4）对每个chromatogram映射到masterChromatogram，得到usedChromatogram
     * 5）对usedChromatogram求feature
     * 6）同时累计所有chromatogram（isDetecting）的intensity为totalXIC
     *
     * @param chromatograms      origin chromatogram
     * @param pickedChroms       maxPeaks picked and recalculated
     * @param intensityLeftRight left right borders
     *                           totalXic : intensity sum of all chromatogram of peptideRef(not rastered and all interval)
     *                           HullPoints : rt intensity pairs of rastered chromatogram between rtLeft, rtRight;
     *                           ExperimentFeature::intensity: intensity sum of hullPoints' intensity
     * @return list of mrmFeature (mrmFeature is list of chromatogram feature)
     */
    public List<List<ExperimentFeature>> findFeatures(List<RtIntensityPairsDouble> chromatograms, List<RtIntensityPairsDouble> pickedChroms, List<IntensityRtLeftRtRightPairs> intensityLeftRight) {

        int[] chrPeakIndex;

        List<List<ExperimentFeature>> experimentFeatures = new ArrayList<>();

        //totalXIC
        double totalXic = 0.0d;
        RtIntensityPairsDouble chromatogram;
        for (int i = 0; i < chromatograms.size(); i++) {
            chromatogram = chromatograms.get(i);
            for (double intensity : chromatogram.getIntensityArray()) {
                totalXic += intensity;
            }
        }

        //mrmFeature loop
        while (true) {
            chrPeakIndex = findLargestPeak(pickedChroms);
            if (chrPeakIndex[0] == -1 || chrPeakIndex[1] == -1) {
                break;
            }
            double peakApex = pickedChroms.get(chrPeakIndex[0]).getRtArray()[chrPeakIndex[1]];

            double bestLeft = intensityLeftRight.get(chrPeakIndex[0]).getRtLeftArray()[chrPeakIndex[1]];
            double bestRight = intensityLeftRight.get(chrPeakIndex[0]).getRtRightArray()[chrPeakIndex[1]];

            RtIntensityPairsDouble rtInt = pickedChroms.get(chrPeakIndex[0]);
            Double[] intensityArray = rtInt.getIntensityArray();
            intensityArray[chrPeakIndex[1]] = 0.0d;
            removeOverlappingFeatures(pickedChroms, bestLeft, bestRight, intensityLeftRight);

            RtIntensityPairsDouble masterChromatogram = new RtIntensityPairsDouble(chromatograms.get(chrPeakIndex[0]));

            PeakGroup peakGroup = new PeakGroup();
            peakGroup.setRt(peakApex);
            peakGroup.setTotalXic(totalXic);
            List<ExperimentFeature> mrmFeature = new ArrayList<>();
            double sum = 0.0d;
            for (int i = 0; i < chromatograms.size(); i++) {
                chromatogram = chromatograms.get(i);
                //best left and right is a constant value to a peptideRef
                RtIntensityPairsDouble usedChromatogram = raster(chromatogram, masterChromatogram, bestLeft, bestRight);
                ExperimentFeature feature = calculatePeakApexInt(usedChromatogram, bestLeft, bestRight, peakApex);
                sum += feature.getIntensity();
                mrmFeature.add(feature);
            }
            peakGroup.setIntensitySum(sum);
            for (ExperimentFeature feature : mrmFeature) {
                feature.setTotalXic(totalXic);
                feature.setIntensitySum(sum);
            }
            if (sum > 0) {
                experimentFeatures.add(mrmFeature);
            }
            if (sum > 0 && sum / totalXic < Constants.STOP_AFTER_INTENSITY_RATIO) {
                break;
            }
        }
        checkOverlappingFeatures(experimentFeatures);

        return experimentFeatures;
    }

    /**
     * 从maxPeak list中选取最大peak对应的index
     *
     * @param pickedChroms maxPeaks
     * @return list index, pairs index
     */
    private int[] findLargestPeak(List<RtIntensityPairsDouble> pickedChroms) {
        double largest = 0.0d;
        int[] chrPeakIndex = new int[2];
        chrPeakIndex[0] = -1;
        chrPeakIndex[1] = -1;
        for (int i = 0; i < pickedChroms.size(); i++) {
            for (int j = 0; j < pickedChroms.get(i).getRtArray().length; j++) {
                if (pickedChroms.get(i).getIntensityArray()[j] > largest) {
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
     * 将chromatogram中的intensity按照masterChromatogram的rt进行分布
     *
     * @param chromatogram       normal chromatogram
     * @param masterChromatogram chromatogram with max peak
     * @param leftBoundary       bestLeftRt rt of max peak(constant to peptideRef)
     * @param rightBoundary      bestRightRt rt of max peak(constant to peptideRef)
     * @return masterChromatogram with both rt and intensity
     */
    private RtIntensityPairsDouble raster(RtIntensityPairsDouble chromatogram, RtIntensityPairsDouble masterChromatogram, double leftBoundary, double rightBoundary) {
        int chromatogramLeft, chromatogramRight;
        int masterChromLeft, masterChromRight;
        chromatogramLeft = ConvolutionUtil.findIndex(chromatogram.getRtArray(), leftBoundary, true);
        chromatogramRight = ConvolutionUtil.findIndex(chromatogram.getRtArray(), rightBoundary, false);
        masterChromLeft = ConvolutionUtil.findIndex(masterChromatogram.getRtArray(), leftBoundary, true);
        masterChromRight = ConvolutionUtil.findIndex(masterChromatogram.getRtArray(), rightBoundary, false);
        int masterChromLeftStatic = masterChromLeft;

        Double[] rt = new Double[masterChromRight - masterChromLeft + 1];
        Double[] intensity = new Double[masterChromRight - masterChromLeft + 1];
        double distLeft, distRight;

        for (int i = 0; i < rt.length; i++) {
            rt[i] = 0d;
            intensity[i] = 0d;
        }

        //set rt
        for (int i = masterChromLeft; i <= masterChromRight; i++) {
            rt[i - masterChromLeftStatic] = masterChromatogram.getRtArray()[i];
        }

        //set intensity
        while (chromatogramLeft <= chromatogramRight && chromatogram.getRtArray()[chromatogramLeft] < masterChromatogram.getRtArray()[masterChromLeft]) {
            intensity[masterChromLeft - masterChromLeftStatic] += chromatogram.getIntensityArray()[chromatogramLeft];
            chromatogramLeft++;
        }
        while (chromatogramLeft <= chromatogramRight) {
            while (masterChromLeft <= masterChromRight && chromatogram.getRtArray()[chromatogramLeft] > masterChromatogram.getRtArray()[masterChromLeft]) {
                masterChromLeft++;
            }
            if (masterChromLeft != masterChromLeftStatic) {
                masterChromLeft--;
            }
            if (masterChromLeft == masterChromRight) {
                break;
            }
            distLeft = Math.abs(chromatogram.getRtArray()[chromatogramLeft] - masterChromatogram.getRtArray()[masterChromLeft]);
            distRight = Math.abs(chromatogram.getRtArray()[chromatogramLeft] - masterChromatogram.getRtArray()[masterChromLeft + 1]);

            intensity[masterChromLeft - masterChromLeftStatic] += chromatogram.getIntensityArray()[chromatogramLeft] * distRight / (distRight + distLeft);
            intensity[masterChromLeft - masterChromLeftStatic + 1] += chromatogram.getIntensityArray()[chromatogramLeft] * distLeft / (distRight + distLeft);

            chromatogramLeft++;
        }
        while (chromatogramLeft <= chromatogramRight) {
            intensity[masterChromLeft - masterChromLeftStatic] += chromatogram.getIntensityArray()[chromatogramLeft];
            chromatogramLeft++;
        }

        return new RtIntensityPairsDouble(rt, intensity);
    }

    /**
     * @param chromatogram masterChromatogram(same rt)
     * @param bestLeft     bestLeftRt rt of max peak
     * @param bestRight    bestRightRt rt of max peak
     * @param peakApexRt   rt of max peak
     * @return
     */
    private ExperimentFeature calculatePeakApexInt(RtIntensityPairsDouble chromatogram, double bestLeft, double bestRight, double peakApexRt) {
        Double[] rtArray = chromatogram.getRtArray();
        Double[] intArray = chromatogram.getIntensityArray();

        double peakApexDist = Math.abs(rtArray[0] - peakApexRt);
        double peakApexInt = 0.0d;
        List<Double> hullRt = new ArrayList<>();
        List<Double> hullInt = new ArrayList<>();
        double intSum = 0.0d;
        for (int i = 0; i < chromatogram.getRtArray().length; i++) {
            //TODO error in original code
            if (rtArray[i] > bestLeft && rtArray[i] < bestRight) {
                hullRt.add(rtArray[i]);
                hullInt.add(intArray[i]);
                if (Math.abs(rtArray[i] - peakApexRt) <= peakApexDist) {
                    peakApexDist = Math.abs(rtArray[i] - peakApexRt);
                    peakApexInt = intArray[i];
                }
                intSum += intArray[i];
            }
        }
        ExperimentFeature feature = new ExperimentFeature();
        feature.setRt(peakApexRt);
        feature.setIntensity(intSum);
        feature.setPeakApexInt(peakApexInt);
        feature.setBestLeftRt(bestLeft);
        feature.setBestRightRt(bestRight);
        feature.setHullRt(hullRt);
        feature.setHullInt(hullInt);

        return feature;
    }


    private float linearInterpolate(float x, float x0, float x1, float y0, float y1) {

        return y0 + (x - x0) * (y1 - y0) / (x1 - x0);
    }


    private void removeOverlappingFeatures(List<RtIntensityPairsDouble> pickedChroms, double bestLeft, double bestRight, List<IntensityRtLeftRtRightPairs> intensityLeftRight) {
        for (int i = 0; i < pickedChroms.size(); i++) {
            Double[] intensity = pickedChroms.get(i).getIntensityArray();
            Double[] rt = pickedChroms.get(i).getRtArray();
            for (int j = 0; j < intensity.length; j++) {
                if (intensity[j] <= 0d) {
                    continue;
                }
                if (rt[j] >= bestLeft && rt[j] <= bestRight) {
                    intensity[j] = 0d;
                }
                double left = intensityLeftRight.get(i).getRtLeftArray()[j];
                double right = intensityLeftRight.get(i).getRtRightArray()[j];
                if ((left > bestLeft && left < bestRight) || (right > bestLeft && right < bestRight)) {
                    intensity[j] = 0d;
                }
            }
        }
    }


    /**
     * 从feature中移除rt覆盖重复的feature
     *
     * @param experimentFeatures all mrmFeatures
     */
    private void checkOverlappingFeatures(List<List<ExperimentFeature>> experimentFeatures) {
        boolean skip;
        int i = 0;
        while (i < experimentFeatures.size()) {
            skip = false;
            for (int j = 0; j < i; j++) {
                if (experimentFeatures.get(i).get(0).getBestLeftRt() >= experimentFeatures.get(j).get(0).getBestLeftRt() &&
                        experimentFeatures.get(i).get(0).getBestRightRt() <= experimentFeatures.get(j).get(0).getBestRightRt()) {
                    skip = true;
                }
            }
            if (skip) {
                experimentFeatures.remove(i);
                i--;
            }
            i++;
        }
    }


}
