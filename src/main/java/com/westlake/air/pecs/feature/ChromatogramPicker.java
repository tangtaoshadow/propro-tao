package com.westlake.air.pecs.feature;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.pecs.domain.bean.math.BisectionLowHigh;
import com.westlake.air.pecs.domain.bean.score.IntensityRtLeftRtRightPairs;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairs;
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
     * @param rtIntensityPairs origin rtIntensity pair
     * @param smoothedRtIntensityPairs rtIntensity pair after smooth
     * @param signalToNoise window length = 1000
     * @param maxPeakPairs picked max peak
     * @return 左右边界rt, chromatogram边界内intensity求和
     */
    public IntensityRtLeftRtRightPairs pickChromatogram(RtIntensityPairsDouble rtIntensityPairs, RtIntensityPairsDouble smoothedRtIntensityPairs, double[] signalToNoise, RtIntensityPairsDouble maxPeakPairs) {
        int maxPeakSize = maxPeakPairs.getRtArray().length;
        int[][] leftRight = new int[maxPeakSize][2];
        Double[] leftRt = new Double[maxPeakSize];
        Double[] rightRt = new Double[maxPeakSize];
        int leftIndex, rightIndex;

        RtIntensityPairsDouble chromatogram;
        if(Constants.CHROMATOGRAM_PICKER_METHOD == "legacy"){
            chromatogram = rtIntensityPairs;
        }else {
            chromatogram = smoothedRtIntensityPairs;
        }

        int closestPeakIndex;
        for (int i = 0; i < maxPeakSize; i++) {
            double centralPeakRt = maxPeakPairs.getRtArray()[i];
            closestPeakIndex = findClosestPeak(chromatogram, maxPeakPairs.getRtArray()[i]);

            //to the left
            leftIndex = closestPeakIndex - 1;
            while(leftIndex > 0 &&
                    chromatogram.getIntensityArray()[leftIndex - 1] < chromatogram.getIntensityArray()[leftIndex] &&
                    centralPeakRt - chromatogram.getRtArray()[leftIndex - 1] < Constants.PEAK_WIDTH &&
                    signalToNoise[leftIndex - 1] >= Constants.SIGNAL_TO_NOISE_LIMIT){
                leftIndex--;
            }

            //to the right
            rightIndex = closestPeakIndex + 1;
            while(rightIndex <chromatogram.getIntensityArray().length - 1 &&
                    chromatogram.getIntensityArray()[rightIndex + 1] < chromatogram.getIntensityArray()[rightIndex] &&
                    chromatogram.getRtArray()[rightIndex + 1] - centralPeakRt < Constants.PEAK_WIDTH &&
                    signalToNoise[rightIndex + 1] >= Constants.SIGNAL_TO_NOISE_LIMIT){
                rightIndex++;
            }

            leftRight[i][0] = leftIndex;
            leftRight[i][1] = rightIndex;
            leftRt[i] = chromatogram.getRtArray()[leftIndex];
            rightRt[i] = chromatogram.getRtArray()[rightIndex];

        }

        Double[] intensity = integratePeaks(rtIntensityPairs, leftRight);

        return new IntensityRtLeftRtRightPairs(intensity, leftRt, rightRt);
    }

    private int findClosestPeak(RtIntensityPairsDouble rtIntensityPairs, double rt) {

        //bisection
        BisectionLowHigh bisectionLowHigh = MathUtil.bisection(rtIntensityPairs.getRtArray(), rt);
        int low = bisectionLowHigh.getLow();
        int high = bisectionLowHigh.getHigh();

        if( Math.abs(rtIntensityPairs.getRtArray()[low] - rt) < Math.abs(rtIntensityPairs.getRtArray()[high] - rt)){
            return low;
        }else {
            return high;
        }
    }

    private Double[] integratePeaks(RtIntensityPairsDouble rtIntensityPairs, int[][] leftRight){
        int leftIndex, rightIndex;
        Double[] intensity = new Double[leftRight.length];
        for(int i = 0; i< leftRight.length; i++){
            intensity[i] = 0d;
            leftIndex = leftRight[i][0];
            rightIndex = leftRight[i][1];
            for(int j=leftIndex; j<= rightIndex; j++){
                intensity[i] += rtIntensityPairs.getIntensityArray()[j];
            }
        }
        return intensity;
    }
}
