package com.westlake.air.pecs.rtnormalizer;

import com.westlake.air.pecs.utils.MathUtil;

import java.util.List;


/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-01 15：27
 */
public class PeakSpline {
    private float[] a, b, c, d, x;

    public float derivatives(float value){
        int i = MathUtil.bisection(x, value);
        float xx = value - x[i];
        return b[i] + 2 * c[i] * xx + 3 * d[i] * xx * xx;
    }

    public float eval(float value){
        int i = MathUtil.bisection(x, value);
        float xx = value - x[i];
        return ((d[i] * xx + c[i]) * xx + b[i]) * xx + a[i];
    }

    public void init(List<float[]> rtIntensity, int leftBoundary, int rightBoundary){
        int maxIndex = rightBoundary - leftBoundary;
        x = new float[maxIndex + 1];
        a = new float[maxIndex + 1];
        b = new float[maxIndex];
        d = new float[maxIndex];
        c = new float[maxIndex + 1]; //c[maxIndex] = 0;
        float[] h = new float[maxIndex];
        float[] mu = new float[maxIndex];
        float[] z = new float[maxIndex];

        float l;
        for(int i = 0; i<= maxIndex; i++){
            x[i] = rtIntensity.get(leftBoundary + i)[0];
            a[i] = rtIntensity.get(leftBoundary + i)[1];
        }

        // do the 0'th element manually
        h[0] = x[1] - x[0];

        for(int i=1; i<maxIndex; i++){
            h[i] = x[i+1] - x[i];
            l = 2 * (x[i+1] - x[i-1]) - h[i - 1] * mu[i - 1];
            mu[i] = h[i] / l;
            z[i] = 3 *((a[i + 1] * h[i - 1] - a[i] * (x[i + 1] - x[i - 1]) + a[i - 1] * h[i]) / (h[i - 1] * h[i]) - h[i - 1] * z[i - 1])/l;
        }

        for(int j = maxIndex - 1; j>=0;j--){
            c[j] = z[j] - mu[j] * c[j + 1];
            b[j] = (a[j + 1] - a[j]) / h[j] - h[j] * (c[j + 1] + 2 * c[j]) / 3;
            d[j] = (c[j + 1] - c[j]) / (3 * h[j]);
        }
    }


}
