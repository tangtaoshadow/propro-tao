package com.westlake.air.pecs.rtnormalizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-07-31 16-28
 */
public class GaussFilter {

    private float sigma = 0.1f;
    private float spacing = 0.01f;

    public List<float[]> gaussFilter(List<float[]> rtIntensity){

        //coeffs: 以0为中心，sigma为标准差的正态分布参数
        float[] coeffs = getCoeffs(sigma, spacing, getRightNum(sigma, spacing));

        //startPosition & endPosition
        int middle = getRightNum(sigma, spacing);
        int listSize = rtIntensity.size();
        float startPosition, endPosition;
        float minRt = rtIntensity.get(0)[0];
        float maxRt = rtIntensity.get(listSize-1)[0];

        //begin integrate
        List<float[]> rtIntensityFiltered = new ArrayList<>();
        float[] rtInt = new float[2];
        float distanceInGaussian;
        int leftPosition;
        int rightPosition;
        float residualPercent;
        float coeffRight;
        float coeffLeft;
        float norm = 0;
        float v = 0;

        for(int i=0;i<listSize; i++){

            //startPosition
            if((rtIntensity.get(i)[0] - middle * spacing) > minRt){
                startPosition = rtIntensity.get(i)[0] - middle * spacing;
            }else {
                startPosition = minRt;
            }

            //endPostion
            if((rtIntensity.get(i)[0] + middle * spacing) < maxRt){
                endPosition = rtIntensity.get(i)[0] + middle * spacing;
            }else{
                endPosition = maxRt;
            }

            //help index
            int j = i;

            // left side of i
            while (j!=0 && rtIntensity.get(j-1)[0] > startPosition){

                distanceInGaussian = Math.abs(rtIntensity.get(i)[0] - rtIntensity.get(j)[0]);
                leftPosition = (int)(distanceInGaussian / spacing);
                rightPosition = leftPosition + 1;
                residualPercent = (Math.abs(leftPosition * spacing) - distanceInGaussian)/spacing;
                if(rightPosition < middle){
                    coeffRight = (1 - residualPercent) * coeffs[leftPosition] + residualPercent * coeffs[rightPosition];
                }else {
                    coeffRight = coeffs[leftPosition];
                }

                distanceInGaussian = Math.abs(rtIntensity.get(i)[0] - rtIntensity.get(j-1)[0]);
                leftPosition = (int)(distanceInGaussian / spacing);
                rightPosition = leftPosition + 1;
                residualPercent = (Math.abs(leftPosition * spacing) - distanceInGaussian) /spacing;
                if(rightPosition < middle){
                    coeffLeft = (1 - residualPercent) * coeffs[leftPosition] + residualPercent * coeffs[rightPosition];
                }else {
                    coeffLeft = coeffs[leftPosition];
                }

                norm += Math.abs(rtIntensity.get(i-1)[0] - rtIntensity.get(i)[0]) * (coeffRight + coeffLeft) / 2.0;
                v += Math.abs(rtIntensity.get(i-1)[0] - rtIntensity.get(i)[0]) * (rtIntensity.get(i-1)[1] * coeffLeft + rtIntensity.get(i)[1] * coeffRight) / 2.0;

                j--;

            }

            j = i;

            // right side of i
            while (j!= listSize -1 && rtIntensity.get(j+1)[0] < endPosition){
                distanceInGaussian = Math.abs(rtIntensity.get(i)[0] - rtIntensity.get(j)[0]);
                leftPosition = (int)(distanceInGaussian / spacing);
                rightPosition = leftPosition + 1;
                residualPercent = (Math.abs(leftPosition * spacing) - distanceInGaussian)/spacing;
                if(rightPosition < middle){
                    coeffLeft = (1 - residualPercent) * coeffs[leftPosition] + residualPercent * coeffs[rightPosition];
                }else {
                    coeffLeft = coeffs[leftPosition];
                }

                distanceInGaussian = Math.abs(rtIntensity.get(i)[0] - rtIntensity.get(j+1)[0]);
                leftPosition = (int)(distanceInGaussian / spacing);
                rightPosition = leftPosition + 1;
                residualPercent = (Math.abs(leftPosition * spacing) - distanceInGaussian) /spacing;
                if(rightPosition < middle){
                    coeffRight = (1 - residualPercent) * coeffs[leftPosition] + residualPercent * coeffs[rightPosition];
                }else {
                    coeffRight = coeffs[leftPosition];
                }

                norm += Math.abs(rtIntensity.get(i+1)[0] - rtIntensity.get(i)[0]) * (coeffLeft + coeffRight) / 2.0;
                v += Math.abs(rtIntensity.get(i+1)[0] - rtIntensity.get(i)[0]) * (rtIntensity.get(i)[1] * coeffLeft + rtIntensity.get(i+1)[1] * coeffRight) / 2.0;

                j++;

            }

            rtInt[0] = rtIntensity.get(i)[0];
            if(v>0){
                rtInt[1] = v/norm;
            }else {
                rtInt[1] = 0;
            }

            rtIntensityFiltered.set(i, rtInt);
        }
        return rtIntensityFiltered;
    }


    private int getRightNum(float sigma, float spacing){
        return (int)(4 * sigma / spacing) + 2;
    }

    private float[] getCoeffs(float sigma, float spacing, int coeffSize){
        float[] coeffs = new float[coeffSize];
        for(int i=0; i<coeffSize; i++){
            //coeffs_[i] = 1.0 / (sigma_ * sqrt(2.0 * Constants::PI)) * exp(-((i * spacing_) * (i * spacing_)) / (2 * sigma_ * sigma_));
            coeffs[i] = (float) (1.0 / (sigma * Math.sqrt(2.0 * Math.PI)) * Math.exp(-((i * spacing) * (i * spacing))/(2 * sigma * sigma)));
        }
        return coeffs;
    }

}
