package com.westlake.air.pecs.utils;

import com.westlake.air.pecs.domain.bean.RtIntensityPairs;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-07-28 21-30
 */
public class MathUtil {

    public static float getRsq(List<float[]> pairs){
        //step1 compute mean
        float sumX = 0, sumY = 0;
        int length = pairs.size();
        for(float[] pair : pairs){
            sumX += pair[0];
            sumY += pair[1];
        }
        float meanX = sumX / length;
        float meanY = sumY / length;

        //step2 compute variance
        sumX = 0; sumY = 0;
        for(float[] pair : pairs){
            sumX += Math.pow(pair[0] - meanX, 2);
            sumY += Math.pow(pair[1] - meanY, 2);
        }
        float varX = sumX / (length - 1);
        float varY = sumY / (length - 1);

        //step3 compute covariance
        float sum = 0;
        for(float[] pair: pairs){
            sum += (pair[0] - meanX) * (pair[1] - meanY);
        }
        float covXY = sum / length;

        //step4 calculate R^2
        return (covXY * covXY) / (varX * varY);
    }

    public static int bisection(float[] x ,float value){
        int high = x.length -1;
        int low = 0;
        int mid;
        while(high - low != 1){
            mid = low + (high - low + 1) / 2;
            if(x[mid] <= value){
                low = mid;
            }else {
                high = mid;
            }
        }
        return low;
    }
    public static int bisection(RtIntensityPairs x , float value){
        int high = x.getRtArray().length -1;
        int low = 0;
        int mid;
        while(high - low != 1){
            mid = low + (high - low + 1) / 2;
            if(x.getRtArray()[mid] <= value){
                low = mid;
            }else {
                high = mid;
            }
        }
        return low;
    }

}
