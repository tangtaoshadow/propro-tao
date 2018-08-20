package com.westlake.air.pecs.feature;

import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairs;
import com.westlake.air.pecs.utils.MathUtil;


/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-01 15：27
 */
public class PeakSpline {
    private double[] a, b, c, d, x;

    // TODO: 暂时只有1阶导数
    public float derivatives(float value){
        int i = MathUtil.bisection(x, value).getHigh();
        if(x[i] > value || x[x.length-1] == value){
            --i;
        }
        double xx = value - x[i];
        return (float)(b[i] + 2 * c[i] * xx + 3 * d[i] * xx * xx);
    }

    public float eval(float value){
        int i = MathUtil.bisection(x, value).getHigh();
        if(x[i] > value || x[x.length-1] == value){
            --i;
        }
        double xx = value - x[i];
        return (float) (((d[i] * xx + c[i]) * xx + b[i]) * xx + a[i]);
    }

    public void init(RtIntensityPairs rtIntensityPairs, int leftBoundary, int rightBoundary){
        int maxIndex = rightBoundary - leftBoundary;
        x = new double[maxIndex + 1];
        a = new double[maxIndex + 1];
        b = new double[maxIndex];
        d = new double[maxIndex];
        c = new double[maxIndex + 1]; //c[maxIndex] = 0;
        double[] h = new double[maxIndex];
        double[] mu = new double[maxIndex];
        double[] z = new double[maxIndex];

        double l;
        for(int i = 0; i<= maxIndex; i++){
            x[i] = rtIntensityPairs.getRtArray()[leftBoundary + i];
            a[i] = rtIntensityPairs.getIntensityArray()[leftBoundary + i];
        }

        // do the 0'th element manually
        h[0] = x[1] - x[0];

        for(int i=1; i<maxIndex; i++){
            h[i] = x[i+1] - x[i];
            l = 2 * (x[i+1] - x[i-1]) - h[i - 1] * mu[i - 1];
            mu[i] = h[i] / l;
            z[i] = (3 *(a[i + 1] * h[i - 1] - a[i] * (x[i + 1] - x[i - 1]) + a[i - 1] * h[i]) / (h[i - 1] * h[i]) - h[i - 1] * z[i - 1])/l;
        }

        for(int j = maxIndex - 1; j>=0;j--){
            c[j] = z[j] - mu[j] * c[j + 1];
            b[j] = (a[j + 1] - a[j]) / h[j] - h[j] * (c[j + 1] + 2 * c[j]) / 3;
            d[j] = (c[j + 1] - c[j]) / (3 * h[j]);
        }
    }
    public void init(Double[] rt, Double[] intensity, int leftBoundary, int rightBoundary){
        int maxIndex = rightBoundary - leftBoundary;
        x = new double[maxIndex + 1];
        a = new double[maxIndex + 1];
        b = new double[maxIndex];
        d = new double[maxIndex];
        c = new double[maxIndex + 1]; //c[maxIndex] = 0;
        double[] h = new double[maxIndex];
        double[] mu = new double[maxIndex];
        double[] z = new double[maxIndex];

        double l;
        for(int i = 0; i<= maxIndex; i++){
            x[i] = rt[leftBoundary + i];
            a[i] = intensity[leftBoundary + i];
        }

        // do the 0'th element manually
        h[0] = x[1] - x[0];

        for(int i=1; i<maxIndex; i++){
            h[i] = x[i+1] - x[i];
            l = 2 * (x[i+1] - x[i-1]) - h[i - 1] * mu[i - 1];
            mu[i] = h[i] / l;
            z[i] = (3 *(a[i + 1] * h[i - 1] - a[i] * (x[i + 1] - x[i - 1]) + a[i - 1] * h[i]) / (h[i - 1] * h[i]) - h[i - 1] * z[i - 1])/l;
        }

        for(int j = maxIndex - 1; j>=0;j--){
            c[j] = z[j] - mu[j] * c[j + 1];
            b[j] = (a[j + 1] - a[j]) / h[j] - h[j] * (c[j + 1] + 2 * c[j]) / 3;
            d[j] = (c[j + 1] - c[j]) / (3 * h[j]);
        }
    }


}
