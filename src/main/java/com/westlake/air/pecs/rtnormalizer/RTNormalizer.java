package com.westlake.air.pecs.rtnormalizer;

import com.westlake.air.pecs.utils.MathUtil;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-07-29 08-59
 */
public class RTNormalizer {


    public static void rtNormalizer(){

        //get chromatogram from database


   }
//
//
//    private List<float[]> simpleFindBestFeature(List<ExperimentFeature> experimentFeatures, ){
//
//    }

    /**
     * 先进行线性拟合，每次从pairs中选取一个residual最大的点丢弃，获得pairsCorrected
     * @param pairs RTPairs
     * @param minRsq goal of iteration
     * @param minCoverage limit of picking
     * @return pairsCorrected
     */
    private List<float[]> removeOutlierIterative(List<float[]> pairs, float minRsq, float minCoverage){

        int pairsSize = pairs.size();
        if( pairsSize < 3){
            return null;
        }

        //获取斜率和截距
        float rsq = 0;
        double[] coEff;

        WeightedObservedPoints obs = new WeightedObservedPoints();
        while(pairs.size() >= pairsSize * minCoverage && rsq< minRsq) {
            obs.clear();
            for(float[] rtPair:pairs){
                obs.add(rtPair[0],rtPair[1]);
            }
            PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
            coEff = fitter.fit(obs.toList());

            rsq = MathUtil.getRsq(pairs);
            if (rsq < minRsq) {
                // calculate residual and get max index
                float res, max = 0;
                int maxIndex = 0;
                for (int i = 0; i < pairs.size(); i++) {
                    res = (float) (Math.abs(pairs.get(i)[1] - (coEff[0] + coEff[1] * pairs.get(i)[0])));
                    if (res > max) {
                        max = res;
                        maxIndex = i;
                    }
                }
                //remove outlier of pairs iteratively
                pairs.remove(maxIndex);
            }
        }
        if(rsq < minRsq){
            return null; //TODO: RTNormalizer: unable to perform outlier detection
        }else {
            return pairs;
        }
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
