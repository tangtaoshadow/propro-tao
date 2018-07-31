package com.westlake.air.pecs.rtnormalizer;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-07-31 19-30
 */
public class SignalToNoiseEstimator {

    private float autoMaxStdevFactor = 3.0f;
    private float windowLength = 200.0f;
    private int binCount = 30;
    private int minRequiredElements = 10;
    private float noiseForEmptyWindow = (float) Math.pow(10.0,20);

    public float[] computeSTN(List<float[]> rtIntensity) {

        //final result
        float[] stnResults = new float[rtIntensity.size()-1];


        //get mean and variance
        float[] meanVariance = getMeanVariance(rtIntensity);

        //get max intensity
        float maxIntensity = meanVariance[0] + meanVariance[1] * autoMaxStdevFactor;

        //bin params
        float windowHalfSize = windowLength / 2.0f;
        float binSize = Math.max(1.0f, maxIntensity / binCount);
        float[] binValue = new float[binCount];
        for (int bin = 0; bin < binCount; bin++) {
            binValue[bin] = (bin + 0.5f) * binSize;
        }

        //params
        int[] histogram = new int[binCount];
        int toBin;// bin in which a datapoint would fall
        int medianBin;// index of bin where the median is located
        int elementIncCount = 0;// additive number of elements from left to x in histogram
        int elementsInWindow = 0;// tracks elements in current window, which may vary because of unevenly spaced data
        int windowCount = 0;// number of windows
        int elementsInWindowHalf;// number of elements where we find the median
        float noise;// noise value of a data point
        int windowsOverall = rtIntensity.size() - 1;// determine how many elements we need to estimate (for progress estimation)
        float sparseWindowPercent = 0;

        //Main loop
        int positionCenter = 0;
        while (positionCenter != windowsOverall) {

            //get left/right borders
            for (int left = positionCenter; left >= 0; left--) {
                if (rtIntensity.get(left)[0] >= rtIntensity.get(positionCenter)[0] - windowHalfSize) {
                    toBin = Math.max(Math.min((int) (rtIntensity.get(left)[0] / binSize), binCount - 1), 0);
                    histogram[toBin]++;
                    elementsInWindow++;
                    left--;
                } else {
                    break;
                }
            }
            for (int right = positionCenter + 1; right < windowsOverall; right++) {
                if (rtIntensity.get(right)[0] <= rtIntensity.get(positionCenter)[0] + windowHalfSize) {
                    toBin = Math.max(Math.min((int) (rtIntensity.get(right)[0] / binSize), binCount - 1), 0);
                    histogram[toBin]++;
                    elementsInWindow++;
                    right++;
                } else {
                    break;
                }
            }

            //noise
            if(elementsInWindow < minRequiredElements){
                noise = noiseForEmptyWindow;
                sparseWindowPercent++;
            }else {
                medianBin = -1;
                elementsInWindowHalf = (elementsInWindow + 1)/2;
                while(medianBin < binCount - 1 && elementIncCount < elementsInWindowHalf) {
                    ++ medianBin;
                    elementIncCount += histogram[medianBin];
                }
                noise = Math.max(1.0f, binValue[medianBin]);
            }
            stnResults[positionCenter] = rtIntensity.get(positionCenter)[1] / noise;
            positionCenter++;
            windowCount ++;
        }

        sparseWindowPercent = sparseWindowPercent * 100 / windowCount;
        if(sparseWindowPercent > 20){
            System.out.println("Warning in SignalToNoiseEstimator: "+ sparseWindowPercent +"% of windows were sparse.\nIncreasing windowLength or decreasing minRequiredElements");
        }

        return stnResults;

    }

    /**
     * 求出intensity的平均值和方差
     *
     * @param rtIntensity k,v
     * @return 0:mean 1:variance
     */
    private float[] getMeanVariance(List<float[]> rtIntensity) {

        float[] meanVariance = new float[2];

        //get mean
        float sum = 0;
        int count = 0;
        for (float[] rtInt : rtIntensity) {
            sum += rtInt[1];
            count++;
        }
        meanVariance[0] = sum / count;

        //get variance
        for (float[] rtInt : rtIntensity) {
            sum = 0;
            sum += Math.pow((meanVariance[0] - rtInt[1]), 2);
        }
        meanVariance[1] = sum / count;

        return meanVariance;
    }
}
