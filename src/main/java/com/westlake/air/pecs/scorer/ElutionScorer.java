package com.westlake.air.pecs.scorer;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairs;
import com.westlake.air.pecs.domain.bean.score.EmgModelParams;
import com.westlake.air.pecs.domain.bean.score.ExperimentFeature;
import com.westlake.air.pecs.domain.bean.score.RTNormalizationScores;
import com.westlake.air.pecs.utils.MathUtil;
import net.finmath.optimizer.LevenbergMarquardt;
import net.finmath.optimizer.SolverException;

import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-08-19 21:00
 */
public class ElutionScorer {

    public void calculateElutionModelScore(List<ExperimentFeature> experimentFeatures, RTNormalizationScores scores){
        float avgScore = 0.0f;
        for(ExperimentFeature feature: experimentFeatures){
            RtIntensityPairs preparedHullPoints = prepareElutionFit(feature);
            float sum = 0.0f;
            Float[] intArray = preparedHullPoints.getIntensityArray();
            Float[] rtArray = preparedHullPoints.getRtArray();
            for(float intens: intArray){
                sum += intens;
            }

            int medianIndex = 0;
            float count = 0f;
            for(int i=0; i<intArray.length; i++){
                count += intArray[i];
                if(count > sum /2f){
                    medianIndex = i-1;
                    break;
                }
            }
            double[] xInit = new double[4];
            xInit[0] = intArray[medianIndex];
            xInit[3] = rtArray[medianIndex];
            boolean symmetric = false;
            float symmetry;
            if(rtArray[medianIndex] - rtArray[0] == 0f){
                symmetric = true;
                symmetry = 10;
            }else {
                symmetry = Math.abs((rtArray[rtArray.length - 1] - rtArray[medianIndex]) / (rtArray[medianIndex] - rtArray[0]));
            }
            if(symmetry < 1){
                symmetry +=5;
            }
            xInit[1] = symmetry;
            xInit[2] = symmetry;
            double[] xInitOptimized;
            if(!symmetric){
                xInitOptimized = getLevenbergMarquardtOptimizer(preparedHullPoints, xInit);
            }else {
                xInitOptimized = xInit;
            }
            EmgModelParams emgModelParams = new EmgModelParams();
            emgModelParams = getEmgParams(preparedHullPoints, xInitOptimized, emgModelParams);
            double[] dataArray = getEmgSample(emgModelParams);

            Float[] modelData = new Float[rtArray.length];
            for(int i=0; i<rtArray.length; i++){
                double value = getValue(dataArray, rtArray[i], emgModelParams);
                modelData[i] = (float)value;
            }

            float fScore = pearsonCorrelationCoefficient(intArray, modelData);
            avgScore += fScore;
        }
        avgScore /= experimentFeatures.size();
        scores.setVarElutionModelFitScore(avgScore);
    }

    /**
     * prepareFit_
     * @param feature chromatogram level feature
     * @return extended rtIntensity array
     */
    private RtIntensityPairs prepareElutionFit(ExperimentFeature feature){
        List<Float> rtArray = feature.getHullRt();
        List<Float> intArray = feature.getHullInt();


        //get rt distance average
        float sum = rtArray.get(rtArray.size()-1) - rtArray.get(0);
        float rtDistanceAverage = sum / (rtArray.size() - 1);
        float rightSideRt = rtArray.get(rtArray.size()-1) + rtDistanceAverage;
        float leftSideRt = rtArray.get(0) - rtDistanceAverage;

        //get new List
        Float[] newRtArray = new Float[rtArray.size() + 6];
        Float[] newIntArray = new Float[intArray.size() + 6];
        assert intArray.size() == rtArray.size();

        for(int i=0; i<newRtArray.length; i++){
            if(i<3){
                newRtArray[i] = leftSideRt - (2 - i) * rtDistanceAverage;
                newIntArray[i] = 0.0f;
            } else if(i>newRtArray.length - 4){
                newRtArray[i] = rightSideRt + (i - newRtArray.length - 3) * rtDistanceAverage;
                newIntArray[i] = 0.0f;
            } else {
                newRtArray[i] = rtArray.get(i - 3);
                newIntArray[i] = intArray.get(i - 3);
            }
        }

        RtIntensityPairs rtIntensityPairs = new RtIntensityPairs();
        rtIntensityPairs.setRtArray(newRtArray);
        rtIntensityPairs.setIntensityArray(newIntArray);

        return rtIntensityPairs;
    }

    private EmgModelParams getEmgParams(RtIntensityPairs featurePrepared, double[] xInitOptimized, EmgModelParams emgModelParams){
        Float[] rtArray = featurePrepared.getRtArray();
        float minBound = rtArray[0];
        float maxBound = rtArray[rtArray.length - 1];
        float stdev = (float)Math.sqrt(emgModelParams.getVariance()) * emgModelParams.getToleranceStdevBox();
        minBound -= stdev;
        maxBound += stdev;
        emgModelParams.setBoundingBoxMax(maxBound);
        emgModelParams.setBoundingBoxMin(minBound);
        emgModelParams.setHeight(xInitOptimized[0]);
        emgModelParams.setWidth(xInitOptimized[1]);
        emgModelParams.setSymmetry(xInitOptimized[2]);
        emgModelParams.setRetention(xInitOptimized[3]);
        return emgModelParams;
    }

    private double[] getEmgSample(EmgModelParams emgModelParams){
        float min = emgModelParams.getBoundingBoxMin();
        float max = emgModelParams.getBoundingBoxMax();
        if(max == min) {
            return null;
        }

        double height = emgModelParams.getHeight();
        double width = emgModelParams.getWidth();
        double symmetry = emgModelParams.getSymmetry();
        double retention = emgModelParams.getRetention();
        double step = emgModelParams.getInterpolationStep();

        double[] data = new double[(int)((max - min)/step) + 1];

        double sqrt2Pi = Math.sqrt(2 * Math.PI);
        double termSq2 = -Constants.EMG_CONST / Math.sqrt(2.0);
        double part1 = height * width / symmetry;
        double part2 = Math.pow(width, 2) / (2 * Math.pow(symmetry, 2));
        double part3 = width / symmetry;
        double position = min;
        double tmp;
        for(int i=0; position < max; i++){
            position = min + i * step;
            tmp = position - retention;
            data[i] = (part1 * sqrt2Pi * Math.exp(part2 - (tmp / symmetry)) / (1 + Math.exp(termSq2 * ((tmp / width) - part3))));
        }
        return data;
    }

    private double getValue(double[] data, float rt, EmgModelParams emgModelParams){
        //key2index
        rt -= emgModelParams.getBoundingBoxMin();
        rt /= emgModelParams.getInterpolationStep();
        int dataMaxIndex = data.length - 1;

        //modf
        if(rt < 0){
            if(rt > -1){
                return data[0] * (1 + rt);
            }else {
                return 0;
            }
        }else if(rt > dataMaxIndex){
            if(rt < dataMaxIndex + 1){
                return data[dataMaxIndex] * (1 - rt + dataMaxIndex);
            }else {
                return 0;
            }
        }else {
            return data[(int)rt + 1] * (rt - (int)rt) + data[(int)rt] * (1 - rt + (int)rt);
        }

    }

    private float pearsonCorrelationCoefficient(Float[] realData, Float[] modelData){
        assert realData.length == modelData.length;
        float realDataAverage = MathUtil.getAverage(realData);
        float modelDataAverage = MathUtil.getAverage(modelData);
        float numerator = 0.0f, realDenominator = 0.0f, modelDenominator = 0.0f;
        float realTemp, modelTemp;
        for(int i=0; i<realData.length; i++){
            realTemp = realData[i] - realDataAverage;
            modelTemp = modelData[i] - modelDataAverage;
            numerator += realTemp * modelTemp;
            realDenominator += realTemp * realTemp;
            modelDenominator += modelTemp * modelTemp;
        }
        float denominator = (float) Math.sqrt(realDenominator * modelDenominator);
        if(denominator == 0){
            return -1.0f;
        }else {
            return numerator / denominator;
        }
    }

    /**
     *
     * @param preparedPairs result of prepareElutionFit
     * @param xInit xInit * 4
     */
    private double[] getLevenbergMarquardtOptimizer(RtIntensityPairs preparedPairs, double[] xInit){
        double sqrt2Pi = Math.sqrt(2 * Math.PI);
        double sqrt2 = Math.sqrt(2);

        LevenbergMarquardt optimizer = new LevenbergMarquardt() {
            @Override
            public void setValues(double[] values, double[] parameters){
                double h = parameters[0];
                double w = parameters[1];
                double s = parameters[2];
                double z = parameters[3];
                for(int i=0; i<preparedPairs.getIntensityArray().length; i++){
                    double t = preparedPairs.getRtArray()[i];
                    double e = preparedPairs.getIntensityArray()[i];
                    values[i] =(h * w / s) * sqrt2Pi * Math.exp((Math.pow(w, 2) / (2 * Math.pow(s, 2))) - ((t - z) / s)) / (1 + Math.exp((-Constants.EMG_CONST / sqrt2) * (((t - z) / w) - w / s))) - e;
                }
            }
        };
        optimizer.setInitialParameters(xInit);
        optimizer.setMaxIteration(Constants.EMG_MAX_ITERATION);
        optimizer.setTargetValues(new double[preparedPairs.getRtArray().length]);
        optimizer.setWeights(new double[]{1.0, 1.0, 1.0, 1.0});
        try{
            optimizer.run();
            return optimizer.getBestFitParameters();
        }catch (SolverException exception){
            exception.printStackTrace();
            System.out.println("LevenbergMarquardt SolverException.");
            return null;
        }
    }

}
