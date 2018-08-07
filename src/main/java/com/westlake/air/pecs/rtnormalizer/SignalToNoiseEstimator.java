package com.westlake.air.pecs.rtnormalizer;

import com.westlake.air.pecs.domain.bean.RtIntensityPairs;
import org.springframework.stereotype.Component;

import javax.naming.Name;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-07-31 19-30
 */
@Component("signalToNoiseEstimator")
public class SignalToNoiseEstimator {

    private static final float AUTO_MAX_STDEV_FACTOR = 3.0f;
    private static final int MIN_REQUIRED_ELEMENTS = 10;
    private static final float NOISE_FOR_EMPTY_WINDOW = (float) Math.pow(10.0,20);

    /**
     * 计算信噪比
     * @param rtIntensity
     * @param windowLength
     * @param binCount
     * @return
     */
    public float[] computeSTN(RtIntensityPairs rtIntensity, float windowLength, int binCount) {

        //final result
        float[] stnResults = new float[rtIntensity.getRtArray().length];

        //get mean and variance
        float[] meanVariance = getMeanVariance(rtIntensity);

        //get max intensity
        float maxIntensity = meanVariance[0] + meanVariance[1] * AUTO_MAX_STDEV_FACTOR;

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
        int windowsOverall = rtIntensity.getRtArray().length - 1;// determine how many elements we need to estimate (for progress estimation)
        float sparseWindowPercent = 0;

        //Main loop
        int positionCenter = 0;
        while (positionCenter <= windowsOverall) {

            //get left/right borders
            for (int left = positionCenter; left >= 0; left--) {
                if (rtIntensity.getRtArray()[left] >= rtIntensity.getRtArray()[positionCenter] - windowHalfSize) {
                    toBin = Math.max(Math.min((int) (rtIntensity.getRtArray()[left] / binSize), binCount - 1), 0);
                    histogram[toBin]++;
                    elementsInWindow++;
                    left--;
                } else {
                    break;
                }
            }
            for (int right = positionCenter + 1; right <= windowsOverall; right++) {
                if (rtIntensity.getRtArray()[right] <= rtIntensity.getRtArray()[positionCenter] + windowHalfSize) {
                    toBin = Math.max(Math.min((int) (rtIntensity.getRtArray()[right] / binSize), binCount - 1), 0);
                    histogram[toBin]++;
                    elementsInWindow++;
                    right++;
                } else {
                    break;
                }
            }

            //noise
            if(elementsInWindow < MIN_REQUIRED_ELEMENTS){
                noise = NOISE_FOR_EMPTY_WINDOW;
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
            stnResults[positionCenter] = rtIntensity.getIntensityArray()[positionCenter] / noise;
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
    private float[] getMeanVariance(RtIntensityPairs rtIntensity) {

        float[] meanVariance = new float[2];
        Float[] intensity = rtIntensity.getIntensityArray();

        //get mean
        float sum = 0;
        int count = 0;
        for (float intens : intensity) {
            sum += intens;
            count++;
        }
        meanVariance[0] = sum / count;

        //get variance
        for (float intens : intensity) {
            sum = 0;
            sum += Math.pow((meanVariance[0] - intens), 2);
        }
        meanVariance[1] = sum / count;

        return meanVariance;
    }
}
