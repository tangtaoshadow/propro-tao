package com.westlake.air.pecs.utils;

import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairs;
import com.westlake.air.pecs.domain.bean.math.BisectionLowHigh;
import com.westlake.air.pecs.domain.bean.score.RtPair;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-07-28 21-30
 */
public class MathUtil {

    public static float getRsq(List<RtPair> pairs){
        //step1 compute mean
        float sumX = 0, sumY = 0;
        int length = pairs.size();
        for(RtPair pair : pairs){
            sumX += pair.getExpRt();
            sumY += pair.getTheoRt();
        }
        float meanX = sumX / length;
        float meanY = sumY / length;

        //step2 compute variance
        sumX = 0; sumY = 0;
        for(RtPair pair : pairs){
            sumX += Math.pow(pair.getExpRt() - meanX, 2);
            sumY += Math.pow(pair.getTheoRt() - meanY, 2);
        }
        float varX = sumX / (length - 1);
        float varY = sumY / (length - 1);

        //step3 compute covariance
        float sum = 0;
        for(RtPair pair: pairs){
            sum += (pair.getExpRt() - meanX) * (pair.getTheoRt() - meanY);
        }
        float covXY = sum / length;

        //step4 calculate R^2
        return (covXY * covXY) / (varX * varY);
    }

    public static BisectionLowHigh bisection(double[] x ,double value){
        BisectionLowHigh bisectionLowHigh = new BisectionLowHigh();
        int high = x.length -1;
        int low = 0;
        int mid;

        if(value < x[0]){
            high = 0;
        }else if(value > x[x.length - 1]){
            low = x.length - 1;
        }else {
            while (high - low != 1) {
                mid = low + (high - low + 1) / 2;
                if (x[mid] < value) {
                    low = mid;
                } else {
                    high = mid;
                }
            }
        }
        bisectionLowHigh.setLow(low);
        bisectionLowHigh.setHigh(high);
        return bisectionLowHigh;
    }

    /**
     * 调用之前必须保证输入数据为升序
     * @param x 输入数据
     * @param value 目标数据
     * @return left and right index of x
     */
    public static BisectionLowHigh bisection(List<Float> x, double value){
        BisectionLowHigh bisectionLowHigh = new BisectionLowHigh();
        int high = x.size() -1;
        int low = 0;
        int mid;
        if(x.get(0)> value){
            high = 0;
        }else if(x.get(x.size()-1) < value){
            low = x.size() - 1;
        }else {
            while (high - low != 1) {
                mid = low + (high - low + 1) / 2;
                if (x.get(mid) < value) {
                    low = mid;
                } else {
                    high = mid;
                }
            }
        }
        bisectionLowHigh.setLow(low);
        bisectionLowHigh.setHigh(high);
        return bisectionLowHigh;
    }
    public static BisectionLowHigh bisection(RtIntensityPairs x , float value){
        BisectionLowHigh bisectionLowHigh = new BisectionLowHigh();
        int high = x.getRtArray().length -1;
        int low = 0;
        int mid;
        if(x.getRtArray()[0]>value){
            high = 0;
        }else if(x.getRtArray()[x.getRtArray().length-1]<value){
            low = x.getRtArray().length - 1;
        }else {
            while (high - low != 1) {
                mid = low + (high - low + 1) / 2;
                if (x.getRtArray()[mid] < value) {
                    low = mid;
                } else {
                    high = mid;
                }
            }
        }
        bisectionLowHigh.setLow(low);
        bisectionLowHigh.setHigh(high);
        return bisectionLowHigh;
    }

    /**
     * (data - mean) / std
     */
    public static float[] standardizeData(List<Float> data){
        int dataLength = data.size();

        //get mean
        float sum = 0f;
        for(float value: data){
            sum += value;
        }
        float mean = sum / dataLength;

        //get std
        sum = 0f;
        for(float value: data){
            sum += (value - mean) * (value - mean);
        }
        float std = (float) Math.sqrt(sum / dataLength);

        //get standardized data
        float[] standardizedData = new float[dataLength];
        for(int i = 0; i< dataLength; i++) {
            if (std == 0) {
                standardizedData[i] = 0;
            } else {
                standardizedData[i] = (data.get(i) - mean) / std;
            }
        }
        return standardizedData;
    }

    public static int findMaxIndex(Float[] data){
        float max = data[0];
        int index = 0;
        for(int i = 0; i < data.length; i++){
            if(data[i] > max){
                max = data[i];
                index = i;
            }
        }
        return index;
    }
    public static int findMaxIndex(List<Float> data){
        float max = data.get(0);
        int index = 0;
        for(int i = 0; i < data.size(); i++){
            if(data.get(i) > max){
                max = data.get(i);
                index = i;
            }
        }
        return index;
    }


    public static int getLog2n(int value){
        int log2n = 0;
        while (value > Math.pow(2,log2n)){
            log2n ++;
        }
        return log2n;
    }

    public static void renormalize(List<Float> floatList){
        float sum = 0.0f;
        for(float value: floatList){
            sum += value;
        }
        for(int i=0; i<floatList.size(); i++) {
            floatList.set(i, floatList.get(i) / sum);
        }
    }

    public static float getAverage(Float[] valueArray){
        float sum = 0.0f;
        for(float value: valueArray){
            sum += value;
        }
        return sum / valueArray.length;
    }



}
