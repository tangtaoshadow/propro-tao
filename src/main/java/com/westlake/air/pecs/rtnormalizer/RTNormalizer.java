package com.westlake.air.pecs.rtnormalizer;

import com.westlake.air.pecs.domain.bean.RtPair;
import com.westlake.air.pecs.domain.bean.ScoreRtPair;
import com.westlake.air.pecs.domain.bean.SlopeIntercept;
import com.westlake.air.pecs.rtnormalizer.domain.ExperimentFeature;
import com.westlake.air.pecs.utils.MathUtil;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-07-29 08-59
 */
public class RTNormalizer {


    public static void rtNormalizer(){

        //get chromatogram from database


   }


    /**
     * get rt pairs for every peptideRef
     * @param scoresList peptideRef list of List<ScoreRtPair>
     * @param rt get from groupsResult.getModel()
     * @return rt pairs
     */
    private List<RtPair> simpleFindBestFeature(List<List<ScoreRtPair>> scoresList, List<Float> rt){
        float max = Float.MIN_VALUE;
        List<RtPair> pairs = new ArrayList<>();
        RtPair rtPair = new RtPair();
        for(int i=0; i<scoresList.size(); i++){
            List<ScoreRtPair> scores = scoresList.get(i);
            //find max score's rt
            for(int j=0; j<scores.size(); j++){
                if(scores.get(j).getScore() > max){
                    max = scores.get(j).getScore();
                    rtPair.setExpRt(scores.get(j).getRt());
                }
            }
            rtPair.setTheoRt(rt.get(i));
            pairs.add(rtPair);
        }
        return pairs;
    }

    /**
     * 先进行线性拟合，每次从pairs中选取一个residual最大的点丢弃，获得pairsCorrected
     * @param pairs RTPairs
     * @param minRsq goal of iteration
     * @param minCoverage limit of picking
     * @return pairsCorrected
     */
    private List<RtPair> removeOutlierIterative(List<RtPair> pairs, float minRsq, float minCoverage){

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
            for(RtPair rtPair:pairs){
                obs.add(rtPair.getExpRt(),rtPair.getTheoRt());
            }
            PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
            coEff = fitter.fit(obs.toList());

            rsq = MathUtil.getRsq(pairs);
            if (rsq < minRsq) {
                // calculate residual and get max index
                float res, max = 0;
                int maxIndex = 0;
                for (int i = 0; i < pairs.size(); i++) {
                    res = (float) (Math.abs(pairs.get(i).getTheoRt() - (coEff[0] + coEff[1] * pairs.get(i).getExpRt())));
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
    private boolean computeBinnedCoverage(float[] rtRange, List<RtPair> pairsCorrected, int rtBins, int minPeptidesPerBin, int minBinsFilled){
        int[] binCounter = new int[rtBins];
        float rtDistance = rtRange[1] - rtRange[0];

        //获得theorRt部分的分布
        for(RtPair pair: pairsCorrected){
            float percent = (pair.getTheoRt() - rtRange[0])/rtDistance;
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
    private SlopeIntercept fitRTPairs(List<RtPair> rtPairs){
        WeightedObservedPoints obs = new WeightedObservedPoints();
        for(RtPair rtPair:rtPairs){
            obs.add(rtPair.getExpRt(),rtPair.getTheoRt());
        }
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
        double[] coeff = fitter.fit(obs.toList());
        SlopeIntercept slopeIntercept = new SlopeIntercept();
        slopeIntercept.setSlope((float)coeff[1]);
        slopeIntercept.setIntercept((float)coeff[0]);
        return slopeIntercept;
    }

}
