package com.westlake.air.pecs.rtnormalizer;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-07-29 08-59
 */
public class RTNormalizer {

    private List<float[]> removeOutlierIterative(List<float[]> pairs,float confidenceInterval, float minRsq, float minCoverage, boolean useIterativeChauvenet, String outlierMethod){

        int pairsSize = pairs.size();
        if( pairsSize < 3){ return null;}

        //获取斜率和截距
        WeightedObservedPoints obs = new WeightedObservedPoints();
        for(float[] rtPair:pairs){
            obs.add(rtPair[0],rtPair[1]);
        }
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
        double[] coeff;
        float rsq = 0;
        while(pairs.size() >= pairsSize * minCoverage && rsq< minRsq) {
            coeff = fitter.fit(obs.toList());

            rsq = RTNormalizeUtil.getRsq(pairs);
            List<Float> residual = new ArrayList<Float>();
            if (rsq < minRsq) {
                // calculate residual and get max index
                float res = 0, max = 0;
                int maxIndex = 0;
                for (int i = 0; i < pairs.size(); i++) {
                    res = (float) (Math.abs(pairs.get(i)[1] - (coeff[0] + coeff[1] * pairs.get(i)[0])));
                    residual.add(res);
                    if (res > max) {
                        max = res;
                        maxIndex = i;
                    }
                }
                //remove outlier of pairs iteratively
                pairs.remove(maxIndex);
            }
        }
        return pairs;

    }

    /**
     * 判断是否对RT空间实现了正常密度的覆盖
     * @param rtRange getRTRange
     * @param pairsCorrected remove outlier之后的pairs
     * @param rtBins 需要分成的bin的数量
     * @param minPeptidesPerBin 每个bin最小分到的数量
     * @param minBinsFilled 需要满足↑条件的bin的数量
     * @return boolean 是否覆盖
     */
    private boolean computeBinnedCoverage(float[] rtRange, List<float[]> pairsCorrected, int rtBins, int minPeptidesPerBin, int minBinsFilled){
        int[] binCounter = new int[rtBins];
        float rtDistance = rtRange[1] - rtRange[0];

        //获得theorRt部分的分布
        for(float[] pair: pairsCorrected){
            float percent = (pair[1] - rtRange[0])/rtDistance;
            int bin = (int)(percent * rtBins);
            if(bin>=rtBins){
                bin = rtBins -1;
            }
            binCounter[bin] ++;
        }

        //判断分布是否覆盖
        int binFilled = 0;
        for(int binCount: binCounter){
            if(binCount >= minPeptidesPerBin) binFilled++;
        }
        return binFilled >= minBinsFilled;
    }

    /**
     * 最小二乘法线性拟合RTPairs
     * @param rtPairs <exp_rt, theor_rt>
     * @return 斜率和截距
     */
    private float[] fitRTPairs(List<float[]> rtPairs){
        WeightedObservedPoints obs = new WeightedObservedPoints();
        for(float[] rtPair:rtPairs){
            obs.add(rtPair[0],rtPair[1]);
        }
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
        double[] coeff = fitter.fit(obs.toList());
        float[] slopeIntercept = new float[2];
        slopeIntercept[0] = (float)coeff[1];
        slopeIntercept[1] = (float)coeff[0];
        return slopeIntercept;
    }

}
