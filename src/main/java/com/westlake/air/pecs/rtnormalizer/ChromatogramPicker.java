package com.westlake.air.pecs.rtnormalizer;

import com.westlake.air.pecs.domain.bean.IntensityRtLeftRtRightPairs;
import com.westlake.air.pecs.domain.bean.RtIntensityPairs;
import com.westlake.air.pecs.utils.MathUtil;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-01 20：26
 */
public class ChromatogramPicker {

    private float signalToNoiseLimit = 1.0f;

    public IntensityRtLeftRtRightPairs pickChromatogram(RtIntensityPairs rtIntensityPairs, float[] signalToNoise, RtIntensityPairs maxPeakPairs) {
        int maxPeakSize = maxPeakPairs.getRtArray().length;
        int[][] leftRight = new int[maxPeakSize][2];
        Float[] leftRt = new Float[maxPeakSize];
        Float[] rightRt = new Float[maxPeakSize];
        int leftIndex, rightIndex;

        int closestPeakIndex;
        for (int i = 0; i < maxPeakSize; i++) {
            closestPeakIndex = findClosestPeak(rtIntensityPairs, maxPeakPairs.getRtArray()[i]);

            //to the left
            leftIndex = closestPeakIndex;
            while(leftIndex > 0 &&
                    rtIntensityPairs.getIntensityArray()[leftIndex - 1] < rtIntensityPairs.getIntensityArray()[leftIndex] &&
                    signalToNoise[leftIndex] >= signalToNoiseLimit){
                leftIndex--;
            }

            //to the right
            rightIndex = closestPeakIndex;
            while(rightIndex < rtIntensityPairs.getIntensityArray().length - 1 &&
                    rtIntensityPairs.getIntensityArray()[rightIndex + 1] < rtIntensityPairs.getIntensityArray()[rightIndex] &&
                    signalToNoise[rightIndex] >= signalToNoiseLimit){
                rightIndex++;
            }

            leftRight[i][0] = leftIndex;
            leftRight[i][1] = rightIndex;
            leftRt[i] = rtIntensityPairs.getRtArray()[leftIndex];
            rightRt[i] = rtIntensityPairs.getRtArray()[rightIndex];

        }

        Float[] intensity = integratePeaks(rtIntensityPairs, leftRight);

        return new IntensityRtLeftRtRightPairs(intensity, leftRt, rightRt);
    }

    private int findClosestPeak(RtIntensityPairs rtIntensityPairs, float rt) {

        //bisection
        int low = MathUtil.bisection(rtIntensityPairs, rt);
        int high = rtIntensityPairs.getRtArray().length - 1;


        if( Math.abs(rtIntensityPairs.getRtArray()[low] - rt) < Math.abs(rtIntensityPairs.getRtArray()[high] - rt)){
            return low;
        }else {
            return high;
        }
    }

    private Float[] integratePeaks(RtIntensityPairs rtIntensityPairs, int[][] leftRight){
        int leftIndex, rightIndex, size;
        Float[] intensity = new Float[leftRight.length];
        for(int i = 0; i< leftRight.length; i++){
            leftIndex = leftRight[i][0];
            rightIndex = leftRight[i][1];
            size = rightIndex - leftIndex;
            for(int j=leftIndex; j<= leftIndex + size; j++){
                intensity[i] += rtIntensityPairs.getIntensityArray()[j];
            }
        }
        return intensity;
    }
}
