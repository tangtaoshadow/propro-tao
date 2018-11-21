package com.westlake.air.pecs.utils;

import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.pecs.domain.bean.math.BisectionLowHigh;
import com.westlake.air.pecs.domain.bean.score.RtPair;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-07-28 21-30
 */
public class MathUtil {

//    public static float getRsq(List<RtPair> pairs){
//        //step1 compute mean
//        double sumX = 0, sumY = 0;
//        int length = pairs.size();
//        for(RtPair pair : pairs){
//            sumX += pair.getExpRt();
//            sumY += pair.getTheoRt();
//        }
//        double meanX = sumX / length;
//        double meanY = sumY / length;
//
//        //step2 compute variance
//        sumX = 0; sumY = 0;
//        for(RtPair pair : pairs){
//            sumX += Math.pow(pair.getExpRt() - meanX, 2);
//            sumY += Math.pow(pair.getTheoRt() - meanY, 2);
//        }
//        double varX = sumX / (length - 1);
//        double varY = sumY / (length - 1);
//
//        //step3 compute covariance
//        double sum = 0;
//        for(RtPair pair: pairs){
//            sum += (pair.getExpRt() - meanX) * (pair.getTheoRt() - meanY);
//        }
//        double covXY = sum / length;
//
//        //step4 calculate R^2
//        return (float) ((covXY * covXY) / (varX * varY));
//    }

    public static double getRsq(List<RtPair> pairs){
        double sigmaX = 0d;
        double sigmaY = 0d;
        double sigmaXSquare = 0d;
        double sigmaYSquare = 0d;
        double sigmaXY = 0d;
        double x,y;
        int n = pairs.size();
        for(int i=0; i<n; i++){
            x = pairs.get(i).getExpRt();
            y = pairs.get(i).getTheoRt();
            sigmaX += x;
            sigmaY += y;
            sigmaXY += x * y;
            sigmaXSquare += x * x;
            sigmaYSquare += y * y;
        }


        double r = (n * sigmaXY - sigmaX * sigmaY) / Math.sqrt((n * sigmaXSquare - sigmaX * sigmaX) * (n * sigmaYSquare - sigmaY * sigmaY));

        return r * r;

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
    public static BisectionLowHigh bisection(Double[] x ,double value){
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
    public static BisectionLowHigh bisectionBD(List<BigDecimal> x , double value){
        BisectionLowHigh bisectionLowHigh = new BisectionLowHigh();
        int high = x.size() -1;
        int low = 0;
        int mid;

        if(x.get(0).compareTo(new BigDecimal(Double.toString(value))) > 0){
            high = 0;
        }else if(x.get(x.size() - 1).compareTo(new BigDecimal(Double.toString(value))) < 0){
            low = x.size() - 1;
        }else {
            while (high - low != 1) {
                mid = low + (high - low + 1) / 2;
                if (x.get(mid).compareTo(new BigDecimal(Double.toString(value))) < 0) {
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
    public static BisectionLowHigh bisection(RtIntensityPairsDouble x , double value){
        BisectionLowHigh bisectionLowHigh = new BisectionLowHigh();
        int high = x.getRtArray().length -1;
        int low = 0;
        int mid;

        if(high != 0) {
            if (x.getRtArray()[0] > value) {
                high = 0;
            } else if (x.getRtArray()[x.getRtArray().length - 1] < value) {
                low = x.getRtArray().length - 1;
            } else {
                while (high - low != 1) {
                    mid = low + (high - low + 1) / 2;
                    if (x.getRtArray()[mid] < value) {
                        low = mid;
                    } else {
                        high = mid;
                    }
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
    public static double[] standardizeData(List<Double> data){
        int dataLength = data.size();

        //get mean
        double sum = 0d;
        for(double value: data){
            sum += value;
        }
        double mean = sum / dataLength;

        //get std
        sum = 0f;
        for(double value: data){
            sum += (value - mean) * (value - mean);
        }
        double std = Math.sqrt(sum / dataLength);

        //get standardized data
        double[] standardizedData = new double[dataLength];
        for(int i = 0; i< dataLength; i++) {
            if (std == 0) {
                standardizedData[i] = 0;
            } else {
                standardizedData[i] = (data.get(i) - mean) / std;
            }
        }
        return standardizedData;
    }

    public static int findMaxIndex(Double[] data){
        double max = data[0];
        int index = 0;
        for(int i = 0; i < data.length; i++){
            if(data[i] > max){
                max = data[i];
                index = i;
            }
        }
        return index;
    }
    public static int findMaxIndex(List<Double> data){
        double max = data.get(0);
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

    public static void renormalize(List<Double> doubleList){
        double sum = 0.0d;
        for(double value: doubleList){
            sum += value;
        }
        for(int i=0; i<doubleList.size(); i++) {
            doubleList.set(i, doubleList.get(i) / sum);
        }
    }

    public static double getAverage(Double[] valueArray){
        double sum = 0.0d;
        for(double value: valueArray){
            sum += value;
        }
        return sum / valueArray.length;
    }

    /**
     * 求出数组的平均值和方差
     *
     * @param arrays k,v
     * @return 0:mean 1:variance
     */
    public static double[] getMeanVariance(Double[] arrays) {

        double[] meanVariance = new double[2];

        //get mean
        double sum = 0;
        int count = 0;
        for (double array : arrays) {
            sum += array;
            count++;
        }
        meanVariance[0] = sum / count;

        //get variance
        sum = 0;
        for (double array : arrays) {
            sum += (meanVariance[0] - array) * (meanVariance[0] - array);
        }
        meanVariance[1] = sum / count;

        return meanVariance;
    }

}
