package com.westlake.air.pecs.feature;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.pecs.domain.bean.math.BisectionLowHigh;
import com.westlake.air.pecs.domain.bean.score.IntensityRtLeftRtRightPairs;
import com.westlake.air.pecs.utils.MathUtil;
import org.springframework.stereotype.Component;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-01 20：26
 */
@Component("chromatogramPicker")
public class ChromatogramPicker {

    /**
     * 1）根据pickPicker选出的maxPeak的rt在smooth后的rtIntensity pairs中找到最接近的index
     * 2）选取出左右边界
     * 3）根据origin chromatogram 和左右边界对intensity求和
     *
     * @param intensityArray       origin rtIntensity pair
     * @param smoothIntensityArray rtIntensity pair after smooth
     * @param signalToNoise        window length = 1000
     * @param maxPeakPairs         picked max peak
     * @return 左右边界rt, chromatogram边界内intensity求和
     */
    public IntensityRtLeftRtRightPairs pickChromatogram(Double[] rtArray, Double[] intensityArray, Double[] smoothIntensityArray, double[] signalToNoise, RtIntensityPairsDouble maxPeakPairs) {
        int maxPeakSize = maxPeakPairs.getRtArray().length;
        int[][] leftRight = new int[maxPeakSize][2];
        Double[] leftRt = new Double[maxPeakSize];
        Double[] rightRt = new Double[maxPeakSize];
        int leftIndex, rightIndex;

        Double[] chromatogram;
        if (Constants.CHROMATOGRAM_PICKER_METHOD.equals("legacy")) {
            chromatogram = intensityArray;
        } else {
            chromatogram = smoothIntensityArray;
        }

        int closestPeakIndex;
        for (int i = 0; i < maxPeakSize; i++) {
            double centralPeakRt = maxPeakPairs.getRtArray()[i];
            closestPeakIndex = findClosestPeak(rtArray, maxPeakPairs.getRtArray()[i]);

            //to the left
            leftIndex = closestPeakIndex - 1;
            while (leftIndex > 0 &&
                    (chromatogram[leftIndex - 1] < chromatogram[leftIndex] || (
                            Constants.PEAK_WIDTH > 0 && centralPeakRt - rtArray[leftIndex - 1] < Constants.PEAK_WIDTH)) &&
                    signalToNoise[leftIndex - 1] >= Constants.SIGNAL_TO_NOISE_LIMIT) {
                leftIndex--;
            }

            //to the right
            rightIndex = closestPeakIndex + 1;
            while (rightIndex < chromatogram.length - 1 &&
                    (chromatogram[rightIndex + 1] < chromatogram[rightIndex] || (
                            Constants.PEAK_WIDTH > 0 && rtArray[rightIndex + 1] - centralPeakRt < Constants.PEAK_WIDTH)) &&
                    signalToNoise[rightIndex + 1] >= Constants.SIGNAL_TO_NOISE_LIMIT) {
                rightIndex++;
            }

            leftRight[i][0] = leftIndex;
            leftRight[i][1] = rightIndex;
            leftRt[i] = rtArray[leftIndex];
            rightRt[i] = rtArray[rightIndex];

        }

        Double[] intensity = integratePeaks(intensityArray, leftRight);

        return new IntensityRtLeftRtRightPairs(intensity, leftRt, rightRt);
    }

    private int findClosestPeak(Double[] rtArray, double rt) {
        BisectionLowHigh bisectionLowHigh = MathUtil.bisection(rtArray, rt);
        int low = bisectionLowHigh.getLow();
        int high = bisectionLowHigh.getHigh();

        if (Math.abs(rtArray[low] - rt) < Math.abs(rtArray[high] - rt)) {
            return low;
        } else {
            return high;
        }
    }

    private Double[] integratePeaks(Double[] intensityArray, int[][] leftRight) {
        int leftIndex, rightIndex;
        Double[] intensity = new Double[leftRight.length];
        for (int i = 0; i < leftRight.length; i++) {
            intensity[i] = 0d;
            leftIndex = leftRight[i][0];
            rightIndex = leftRight[i][1];
            for (int j = leftIndex; j <= rightIndex; j++) {
                intensity[i] += intensityArray[j];
            }
        }
        return intensity;
    }
}
