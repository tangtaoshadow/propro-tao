package com.westlake.air.pecs.rtnormalizer;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairs;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-07-31 19-16
 */
@Component("peakPicker")
public class PeakPicker {

    /**
     * 1）选取最高峰
     * 2）对最高峰进行样条插值
     * 3）根据插值判断maxPeak的rt位置
     * 4）用插值与rt计算intensity
     *
     * @param rtIntensityPairs smoothed rtIntensityPairs
     * @param signalToNoise window width = 200
     * @return maxPeaks
     */
    public RtIntensityPairs pickMaxPeak(RtIntensityPairs rtIntensityPairs, float[] signalToNoise){
        if(rtIntensityPairs.getRtArray().length < 5) {
            return null;
        }
        List<float[]> maxPeaks = new ArrayList<>();
        float centralPeakRt, leftNeighborRt, rightNeighborRt;
        float centralPeakInt, leftBoundaryInt, rightBoundaryInt;
        float stnLeft, stnMiddle, stnRight;
        int leftBoundary, rightBoundary;
        int missing;
        float maxPeakRt, maxPeakInt;
        float leftHand, rightHand;
        float mid, midDerivVal;


        for(int i = 2; i<rtIntensityPairs.getRtArray().length - 2; i++){
            leftNeighborRt = rtIntensityPairs.getRtArray()[i - 1];
            centralPeakRt = rtIntensityPairs.getRtArray()[i];
            rightNeighborRt = rtIntensityPairs.getRtArray()[i + 1];

            leftBoundaryInt = rtIntensityPairs.getIntensityArray()[i - 1];
            centralPeakInt = rtIntensityPairs.getIntensityArray()[i];
            rightBoundaryInt = rtIntensityPairs.getIntensityArray()[i + 1];

            if(rightBoundaryInt < 0.000001) continue;
            if(leftBoundaryInt < 0.000001) continue;

            stnLeft = signalToNoise[i-1];
            stnMiddle = signalToNoise[i];
            stnRight = signalToNoise[i+1];

            if(centralPeakInt > leftBoundaryInt &&
                    centralPeakInt > rightBoundaryInt &&
                    stnLeft >= Constants.SIGNAL_TO_NOISE_LIMIT &&
                    stnMiddle >= Constants.SIGNAL_TO_NOISE_LIMIT &&
                    stnRight >= Constants.SIGNAL_TO_NOISE_LIMIT){

                // find left boundary
                missing = 0;
                leftBoundary = i - 1;
                for(int left = 2; left < i+1; left++){

                    stnLeft = signalToNoise[i-left];

                    //zeroLeft
                    if(rtIntensityPairs.getIntensityArray()[i - left] == 0){
                        break;
                    }

                    if(rtIntensityPairs.getIntensityArray()[i - left] < leftBoundaryInt){
                        if(stnLeft >= Constants.SIGNAL_TO_NOISE_LIMIT){
                            leftBoundaryInt = rtIntensityPairs.getIntensityArray()[i - left];
                            leftBoundary = i - left;
                        }else {
                            missing ++;
                            if(missing <= Constants.MISSING_LIMIT){
                                leftBoundaryInt = rtIntensityPairs.getIntensityArray()[i - left];
                                leftBoundary = i - left;
                            }else {
                                break;
                            }
                        }
                    }else {
                        break;
                    }
                }

                // find right boundary
                missing = 0;
                rightBoundary = i + 1;
                for(int right = 2; right < rtIntensityPairs.getRtArray().length - i; right++){

                    stnRight = signalToNoise[i+right];

                    //zeroLeft
                    if(rtIntensityPairs.getIntensityArray()[i + right] == 0){
                        break;
                    }

                    if(rtIntensityPairs.getIntensityArray()[i + right] < rightBoundaryInt){
                        if(stnRight >= Constants.SIGNAL_TO_NOISE_LIMIT){
                            rightBoundaryInt = rtIntensityPairs.getIntensityArray()[i + right];
                            rightBoundary = i + right;
                        }else {
                            missing ++;
                            if(missing <= Constants.MISSING_LIMIT){
                                rightBoundaryInt = rtIntensityPairs.getIntensityArray()[i + right];
                                rightBoundary = i + right;
                            }else {
                                break;
                            }
                        }
                    }else {
                        break;
                    }
                }

                PeakSpline peakSpline = new PeakSpline();
                peakSpline.init(rtIntensityPairs, leftBoundary, rightBoundary);
//                maxPeakRt =  centralPeakRt;
//                maxPeakInt = centralPeakInt;
                leftHand = leftNeighborRt;
                rightHand = rightNeighborRt;

                while (rightHand - leftHand > Constants.THRESHOLD){
                    mid = (leftHand + rightHand) / 2.0f;
                    midDerivVal = peakSpline.derivatives(mid);
                    if(Math.abs(midDerivVal) < 0.0001){
                        break;
                    }
                    if(midDerivVal < 0.0f){
                        rightHand = mid;
                    }else {
                        leftHand = mid;
                    }
                }

                maxPeakRt = (leftHand + rightHand) /2.0f;
                maxPeakInt = peakSpline.eval(maxPeakRt);

                float[] peak = new float[2];
                peak[0] = maxPeakRt;
                peak[1] = maxPeakInt;
                maxPeaks.add(peak);
                i = rightBoundary + 2;
            }
        }
        Float[] rt = new Float[maxPeaks.size()];
        Float[] intensity = new Float[maxPeaks.size()];

        for(int i = 0; i< maxPeaks.size(); i++){
            rt[i] = maxPeaks.get(i)[0];
            intensity[i] = maxPeaks.get(i)[1];
        }
        return new RtIntensityPairs(rt, intensity);
    }

}
