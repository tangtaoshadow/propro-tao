package com.westlake.air.pecs.rtnormalizer;

import com.westlake.air.pecs.domain.bean.RtIntensityPairs;
import org.apache.commons.math3.fitting.GaussianCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import java.security.Guard;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-07-31 16-28
 */
public class GaussFilter {

    private float sigma = 0.1f;
    private float spacing = 0.01f;

    public RtIntensityPairs gaussFilter(RtIntensityPairs pairs) {

        //coeffs: 以0为中心，sigma为标准差的正态分布参数
        float[] coeffs = getCoeffs(sigma, spacing, getRightNum(sigma, spacing));

        //startPosition & endPosition
        int middle = getRightNum(sigma, spacing);
        int listSize = pairs.getRtArray().length;
        float startPosition, endPosition;
        float minRt = pairs.getRtArray()[0];
        float maxRt = pairs.getRtArray()[listSize - 1];

        //begin integrate
        RtIntensityPairs pairsFiltered = new RtIntensityPairs();
        Float[] rtArray = pairs.getRtArray();
        Float[] intArray = pairs.getIntensityArray();
        pairsFiltered.setRtArray(rtArray);
        Float[] newIntArray = new Float[intArray.length];
        float distanceInGaussian;
        int leftPosition;
        int rightPosition;
        float residualPercent;
        float coeffRight;
        float coeffLeft;
        float norm = 0;
        float v = 0;

        for (int i = 0; i < listSize; i++) {

            //startPosition
            if ((rtArray[i] - middle * spacing) > minRt) {
                startPosition = rtArray[i] - middle * spacing;
            } else {
                startPosition = minRt;
            }

            //endPostion
            if ((rtArray[i] + middle * spacing) < maxRt) {
                endPosition = rtArray[i] + middle * spacing;
            } else {
                endPosition = maxRt;
            }

            //help index
            int j = i;

            // left side of i
            while (j != 0 && rtArray[j - 1] > startPosition) {

                distanceInGaussian = Math.abs(rtArray[i] - rtArray[j]);
                leftPosition = (int) (distanceInGaussian / spacing);
                rightPosition = leftPosition + 1;
                residualPercent = (Math.abs(leftPosition * spacing) - distanceInGaussian) / spacing;
                if (rightPosition < middle) {
                    coeffRight = (1 - residualPercent) * coeffs[leftPosition] + residualPercent * coeffs[rightPosition];
                } else {
                    coeffRight = coeffs[leftPosition];
                }

                distanceInGaussian = Math.abs(rtArray[i] - rtArray[j - 1]);
                leftPosition = (int) (distanceInGaussian / spacing);
                rightPosition = leftPosition + 1;
                residualPercent = (Math.abs(leftPosition * spacing) - distanceInGaussian) / spacing;
                if (rightPosition < middle) {
                    coeffLeft = (1 - residualPercent) * coeffs[leftPosition] + residualPercent * coeffs[rightPosition];
                } else {
                    coeffLeft = coeffs[leftPosition];
                }

                norm += Math.abs(rtArray[i - 1] - rtArray[i]) * (coeffRight + coeffLeft) / 2.0;
                v += Math.abs(rtArray[i - 1] - rtArray[i]) * (intArray[i - 1] * coeffLeft + intArray[i] * coeffRight) / 2.0;

                j--;

            }

            j = i;

            // right side of i
            while (j != listSize - 1 && rtArray[j + 1] < endPosition) {
                distanceInGaussian = Math.abs(rtArray[i] - rtArray[j]);
                leftPosition = (int) (distanceInGaussian / spacing);
                rightPosition = leftPosition + 1;
                residualPercent = (Math.abs(leftPosition * spacing) - distanceInGaussian) / spacing;
                if (rightPosition < middle) {
                    coeffLeft = (1 - residualPercent) * coeffs[leftPosition] + residualPercent * coeffs[rightPosition];
                } else {
                    coeffLeft = coeffs[leftPosition];
                }

                distanceInGaussian = Math.abs(rtArray[i] - rtArray[j + 1]);
                leftPosition = (int) (distanceInGaussian / spacing);
                rightPosition = leftPosition + 1;
                residualPercent = (Math.abs(leftPosition * spacing) - distanceInGaussian) / spacing;
                if (rightPosition < middle) {
                    coeffRight = (1 - residualPercent) * coeffs[leftPosition] + residualPercent * coeffs[rightPosition];
                } else {
                    coeffRight = coeffs[leftPosition];
                }

                norm += Math.abs(rtArray[i + 1] - rtArray[i]) * (coeffLeft + coeffRight) / 2.0;
                v += Math.abs(rtArray[i + 1] - rtArray[i]) * (intArray[i] * coeffLeft + intArray[i + 1] * coeffRight) / 2.0;

                j++;

            }

            if (v > 0) {
                newIntArray[i] = v / norm;
            } else {
                newIntArray[i] = 0f;
            }


        }
        pairsFiltered.setIntensityArray(newIntArray);
        return pairsFiltered;
    }


    private int getRightNum(float sigma, float spacing) {
        return (int) Math.ceil(4 * sigma / spacing) + 1;
    }

    private float[] getCoeffs(float sigma, float spacing, int coeffSize) {
        float[] coeffs = new float[coeffSize];
        for (int i = 0; i < coeffSize; i++) {
            //coeffs_[i] = 1.0 / (sigma_ * sqrt(2.0 * Constants::PI)) * exp(-((i * spacing_) * (i * spacing_)) / (2 * sigma_ * sigma_));
            coeffs[i] = (float) (1.0 / (sigma * Math.sqrt(2.0 * Math.PI)) * Math.exp(-((i * spacing) * (i * spacing)) / (2 * sigma * sigma)));
        }
        return coeffs;
    }

    public static void main(String[] args) {


//        WeightedObservedPoints obs = new WeightedObservedPoints();
//        for (int i = 0; i < 9; i++) {
//            if(i==3){
//                obs.add(500.0 + 0.03 * i, 1.0);
//            }else if(i==4){
//                obs.add(500.0 + 0.03 * i, 0.8);
//            }else if(i==5){
//                obs.add(500.0 + 0.03 * i, 1.2);
//            }else{
//                obs.add(500.0 + 0.03 * i, 0.0);
//            }
//        }
//
//        double[] result = GaussianCurveFitter.create().fit(obs.toList());
//        System.out.println("Norm:"+result[0]);
//        System.out.println("Mean:"+result[1]);
//        System.out.println("Sigma:"+result[2]);
//
//        GaussianCurveFitter.ParameterGuesser guesser = new GaussianCurveFitter.ParameterGuesser(obs.toList());
//        double[] guessRes = guesser.guess();
//        System.out.println("Norm:"+guessRes[0]);
//        System.out.println("Mean:"+guessRes[1]);
//        System.out.println("Sigma:"+guessRes[2]);

        Float[] rtArray = new Float[9];
        Float[] intArray = new Float[9];
        for (int i = 0; i < 9; i++) {
            rtArray[i] = 500.0f + 0.03f * i;
            if(i==3){
                intArray[i] = 1.0f;
            }else if(i==4){
                intArray[i] = 0.8f;
            }else if(i==5){
                intArray[i] = 1.2f;
            }else{
                intArray[i] = 0f;
            }
        }

        RtIntensityPairs pairs = new RtIntensityPairs(rtArray, intArray);
        GaussFilter filter = new GaussFilter();
        filter.sigma = 0.025f;
        filter.spacing = 0.01f;
        RtIntensityPairs resultPair = filter.gaussFilter(pairs);
        for(float f : resultPair.getIntensityArray()){
            System.out.println(f);
        }
    }

}
