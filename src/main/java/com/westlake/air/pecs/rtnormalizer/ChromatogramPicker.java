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
//        int maxPeakSize = maxPeakPairs.getRtArray().length;
//        int[][] leftRight = new int[maxPeakSize][2];
//        float[][] intensityLeftrtRightrt = new float[maxPeakSize][3];
//        int leftIndex, rightIndex;
//
//        int closestPeakIndex;
//        for (int i = 0; i < maxPeakSize; i++) {
//            closestPeakIndex = findClosestPeak(rtIntensity, maxPeaks.get(i)[0]);
//
//            //to the left
//            leftIndex = closestPeakIndex;
//            while(leftIndex > 0 &&
//                    rtIntensity.get(leftIndex - 1)[1] < rtIntensity.get(leftIndex)[1] &&
//                    signalToNoise[leftIndex] >= signalToNoiseLimit){
//                leftIndex--;
//            }
//
//            //to the right
//            rightIndex = closestPeakIndex;
//            while(rightIndex < rtIntensity.size() - 1 &&
//                    rtIntensity.get(rightIndex + 1)[1] < rtIntensity.get(rightIndex)[1] &&
//                    signalToNoise[rightIndex] >= signalToNoiseLimit){
//                rightIndex++;
//            }
//
//            leftRight[i][0] = leftIndex;
//            leftRight[i][1] = rightIndex;
//            intensityLeftrtRightrt[i][1] = rtIntensity.get(leftIndex)[0];
//            intensityLeftrtRightrt[i][2] = rtIntensity.get(rightIndex)[0];
//
//        }
//
//        intensityLeftrtRightrt = integratePeaks(rtIntensity, leftRight, intensityLeftrtRightrt);
//
//        return intensityLeftrtRightrt;
        return null;
    }

    private int findClosestPeak(List<float[]> rtIntensity, float rt) {

        //bisection
        int low = MathUtil.bisection(rtIntensity, rt);
        int high = rtIntensity.size() - 1;


        if( Math.abs(rtIntensity.get(low)[0] - rt) < Math.abs(rtIntensity.get(high)[0] - rt)){
            return low;
        }else {
            return high;
        }
    }

    private float[][] integratePeaks(List<float[]> rtIntensity, int[][] leftRight, float[][] intensityLeftRight){
        int leftIndex, rightIndex, size;
        for(int i = 0; i< leftRight.length; i++){
            leftIndex = leftRight[i][0];
            rightIndex = leftRight[i][1];
            size = rightIndex - leftIndex;
            for(int j=leftIndex; j<= leftIndex + size; j++){
                intensityLeftRight[i][0] += rtIntensity.get(j)[1];
            }
        }
        return intensityLeftRight;
    }
}
