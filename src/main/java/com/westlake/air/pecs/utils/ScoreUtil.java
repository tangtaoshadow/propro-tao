package com.westlake.air.pecs.utils;

import com.westlake.air.pecs.domain.bean.score.IntegrateWindowMzIntensity;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.feature.FeatureFinder;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-15 19:38
 */
public class ScoreUtil {

    /**
     * invert y = kx + b to x = 1/k y + -b/k;
     * @param slopeIntercept k & b input
     * @return 1/k -b/k
     */
    public static SlopeIntercept trafoInverter(SlopeIntercept slopeIntercept){
        double slope = slopeIntercept.getSlope();
        double intercept = slopeIntercept.getIntercept();
        SlopeIntercept slopeInterceptInvert = new SlopeIntercept();

        if(slope == 0d){
            slope = 0.000001d;
        }
        slopeInterceptInvert.setSlope(1 / slope);
        slopeInterceptInvert.setIntercept(- intercept / slope);

        return slopeInterceptInvert;
    }

    /**
     * apply kx + b
     * @param slopeIntercept k & b (may be inverted)
     * @param value x
     * @return y
     */
    public static double trafoApplier(SlopeIntercept slopeIntercept, Double value) {
        if (slopeIntercept.getSlope() == 0) {
            return value;
        } else {
            return value * slopeIntercept.getSlope() + slopeIntercept.getIntercept();
        }
    }

    /**
     * 1) get sum of list
     * 2) divide elements in list by sum
     * @param libraryIntensity input intensity list
     * @return output normalized intensity list
     */
    public static double[] normalizeSumDouble(List<Double> libraryIntensity){
        double[] normalizedLibraryIntensity = new double[libraryIntensity.size()];
        double sum = 0d;
        for(Double intensity: libraryIntensity){
            sum += intensity;
        }

        if(sum == 0d){
            sum += 0.000001;
        }

        for(int i = 0; i<libraryIntensity.size(); i++){
            normalizedLibraryIntensity[i] = (libraryIntensity.get(i) / sum);
        }
        return normalizedLibraryIntensity;
    }

    /**
     * 1) get left and right index corresponding to spectrum
     * 2) get interval intensity sum to intensity
     * 3) get interval average mz by intensity(as weight)
     * @param spectrumMzArray spectrum
     * @param spectrumIntArray spectrum
     * @param left left mz
     * @param right right mz
     * @return float mz,intensity boolean signalFound
     */
    public static IntegrateWindowMzIntensity integrateWindow(List<Float> spectrumMzArray, List<Float> spectrumIntArray, double left, double right){
        IntegrateWindowMzIntensity mzIntensity = new IntegrateWindowMzIntensity();

        double mz = 0d, intensity = 0d;
        int leftIndex = MathUtil.bisection(spectrumMzArray, left).getHigh();
        int rightIndex = MathUtil.bisection(spectrumMzArray, right).getLow();

        for(int index = leftIndex; index <=rightIndex; index ++){
            intensity += spectrumIntArray.get(index);
            mz += spectrumMzArray.get(index) * spectrumIntArray.get(index);
        }
        if(intensity > 0f) {
            mz /= intensity;
            mzIntensity.setSignalFound(true);
        }else {
            mz = -1;
            intensity = 0;
            mzIntensity.setSignalFound(false);
        }
        mzIntensity.setMz(mz);
        mzIntensity.setIntensity(intensity);

        return mzIntensity;
    }
}
