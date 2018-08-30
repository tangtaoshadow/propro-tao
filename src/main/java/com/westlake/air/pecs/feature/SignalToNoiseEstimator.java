package com.westlake.air.pecs.feature;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import org.springframework.stereotype.Component;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-07-31 19-30
 */
@Component("signalToNoiseEstimator")
public class SignalToNoiseEstimator {

    /**
     * 计算信噪比
     * 按位取窗口，窗口由小到大排序取中位数
     *
     * @param rtIntensity
     * @param windowLength
     * @param binCount
     * @return
     */
    public double[] computeSTN(RtIntensityPairsDouble rtIntensity, float windowLength, int binCount) {

        //final result
        double[] stnResults = new double[rtIntensity.getRtArray().length];

        //get mean and variance
        double[] meanVariance = getMeanVariance(rtIntensity);

        //get max intensity
        double maxIntensity = meanVariance[0] + Math.sqrt(meanVariance[1]) * Constants.AUTO_MAX_STDEV_FACTOR;

        //bin params
        float windowHalfSize = windowLength / 2.0f;
        double binSize = Math.max(1.0d, maxIntensity / binCount);
        double[] binValue = new double[binCount];
        for (int bin = 0; bin < binCount; bin++) {
            binValue[bin] = (bin + 0.5) * binSize;
        }

        //params
        int[] histogram = new int[binCount];
        int toBin;// bin in which a datapoint would fall
        int medianBin;// index of bin where the median is located
        int elementIncCount;// additive number of elements from left to x in histogram
        int elementsInWindow = 0;// tracks elements in current window, which may vary because of unevenly spaced data
        int windowCount = 0;// number of windows
        int elementsInWindowHalf;// number of elements where we find the median
        double noise;// noise value of a data point
        int windowsOverall = rtIntensity.getRtArray().length - 1;// determine how many elements we need to estimate (for progress estimation)
        float sparseWindowPercent = 0;

        //Main loop
        int positionCenter = 0;
        while (positionCenter <= windowsOverall) {

            //get left/right borders
            for (int left = positionCenter; left >= 0; left--) {
                if (rtIntensity.getRtArray()[left] >= rtIntensity.getRtArray()[positionCenter] - windowHalfSize) {
                    toBin = Math.max(Math.min((int) (rtIntensity.getIntensityArray()[left] / binSize), binCount - 1), 0);
                    histogram[toBin]++;
                    elementsInWindow++;
                } else {

                    break;
                }
            }
            for (int right = positionCenter + 1; right <= windowsOverall; right++) {
                if (rtIntensity.getRtArray()[right] <= rtIntensity.getRtArray()[positionCenter] + windowHalfSize) {
                    toBin = Math.max(Math.min((int) (rtIntensity.getIntensityArray()[right] / binSize), binCount - 1), 0);
                    histogram[toBin]++;
                    elementsInWindow++;
                } else {
                    break;
                }
            }

            //noise
            if (elementsInWindow < Constants.MIN_REQUIRED_ELEMENTS) {
                noise = Constants.NOISE_FOR_EMPTY_WINDOW;
                sparseWindowPercent++;
            } else {
                medianBin = -1;
                elementIncCount = 0;
                elementsInWindowHalf = (elementsInWindow + 1) / 2;
                while (medianBin < binCount - 1 && elementIncCount < elementsInWindowHalf) {
                    ++medianBin;
                    elementIncCount += histogram[medianBin];
                }
                noise = Math.max(1.0, binValue[medianBin]);
            }
            stnResults[positionCenter] = rtIntensity.getIntensityArray()[positionCenter] / noise;
            positionCenter++;
            windowCount++;
        }

        sparseWindowPercent = sparseWindowPercent * 100 / windowCount;
        if (sparseWindowPercent > 20) {
            System.out.println("Warning in SignalToNoiseEstimator: " + sparseWindowPercent + "% of windows were sparse.\nIncreasing windowLength or decreasing minRequiredElements");
        }

        return stnResults;

    }

    /**
     * 求出intensity的平均值和方差
     *
     * @param rtIntensity k,v
     * @return 0:mean 1:variance
     */
    private double[] getMeanVariance(RtIntensityPairsDouble rtIntensity) {

        double[] meanVariance = new double[2];
        Double[] intensity = rtIntensity.getIntensityArray();

        //get mean
        double sum = 0;
        int count = 0;
        for (double intens : intensity) {
            sum += intens;
            count++;
        }
        meanVariance[0] = sum / count;

        //get variance
        sum = 0;
        for (double intens : intensity) {
            sum += Math.pow((meanVariance[0] - intens), 2);
        }
        meanVariance[1] = sum / count;

        return meanVariance;
    }
}
