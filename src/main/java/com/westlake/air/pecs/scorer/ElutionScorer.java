package com.westlake.air.pecs.scorer;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairs;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.pecs.domain.bean.score.EmgModelParams;
import com.westlake.air.pecs.domain.bean.score.ExperimentFeature;
import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import com.westlake.air.pecs.utils.MathUtil;
import net.finmath.optimizer.SolverException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * scores.var_elution_model_fit_score
 * <p>
 * Created by Nico Wang Ruimin
 * Time: 2018-08-19 21:00
 */
@Component("elutionScorer")
public class ElutionScorer {

    public void calculateElutionModelScore(List<ExperimentFeature> experimentFeatures, FeatureScores scores) {
        double avgScore = 0.0d;
        for (ExperimentFeature feature : experimentFeatures) {
            RtIntensityPairsDouble preparedHullPoints = prepareElutionFit(feature);
            if(preparedHullPoints == null){
                avgScore += -1;
                continue;
            }
            double sum = 0.0d;
            Double[] intArray = preparedHullPoints.getIntensityArray();
            Double[] rtArray = preparedHullPoints.getRtArray();
            for (double intens : intArray) {
                sum += intens;
            }

            int medianIndex = 0;
            double count = 0d;
            for (int i = 0; i < intArray.length; i++) {
                count += intArray[i];
                if (count > sum / 2d) {
                    medianIndex = i - 1;
                    break;
                }
            }
            double[] xInit = new double[4];
            xInit[0] = intArray[medianIndex];
            xInit[3] = rtArray[medianIndex];
            boolean symmetric = false;
            double symmetry;
            if (rtArray[medianIndex] - rtArray[0] == 0d) {
                symmetric = true;
                symmetry = 10;
            } else {
                symmetry = Math.abs((rtArray[rtArray.length - 1] - rtArray[medianIndex]) / (rtArray[medianIndex] - rtArray[0]));
            }
            if (symmetry < 1) {
                symmetry += 5;
            }
            xInit[1] = symmetry;
            xInit[2] = symmetry;
            double[] xInitOptimized;
            if (!symmetric) {
                xInitOptimized = getLevenbergMarquardtOptimizer(preparedHullPoints, xInit);
            } else {
                xInitOptimized = xInit;
            }
            EmgModelParams emgModelParams = new EmgModelParams();

            emgModelParams = getEmgParams(preparedHullPoints, xInitOptimized, emgModelParams);
            double[] dataArray = getEmgSample(emgModelParams);

            Double[] modelData = new Double[rtArray.length];
            for (int i = 0; i < rtArray.length; i++) {
                modelData[i] = getValue(dataArray, rtArray[i], emgModelParams);
            }

            double fScore = pearsonCorrelationCoefficient(intArray, modelData);
            if(Double.isNaN(fScore)){
                System.out.printf("sss");
            }
            avgScore += fScore;
        }
        avgScore /= experimentFeatures.size();

        scores.setVarElutionModelFitScore(avgScore);
    }
    /**
     * prepareFit_
     *
     * @param feature chromatogram level feature
     * @return extended rtIntensity array
     */
    private RtIntensityPairsDouble prepareElutionFit(ExperimentFeature feature) {
        List<Double> rtArray = feature.getHullRt();
        List<Double> intArray = feature.getHullInt();

        if(rtArray.size() < 2){
            return null;
        }

        //get rt distance average
        double sum = rtArray.get(rtArray.size() - 1) - rtArray.get(0);
        double rtDistanceAverage = sum / (rtArray.size() - 1);

        //get new List
        Double[] newRtArray = new Double[rtArray.size() + 6];
        Double[] newIntArray = new Double[intArray.size() + 6];
        assert intArray.size() == rtArray.size();

        for (int i = 0; i < newRtArray.length; i++) {
            if (i < 3) {
                newRtArray[i] = (rtArray.get(0) - (3 - i) * rtDistanceAverage);
                newIntArray[i] = 0.0;
            } else if (i > newRtArray.length - 4) {
                newRtArray[i] = (rtArray.get(rtArray.size() - 1) + (i - rtArray.size() - 2) * rtDistanceAverage);
                newIntArray[i] = 0.0;
            } else {
                newRtArray[i] = rtArray.get(i - 3);
                newIntArray[i] = intArray.get(i - 3);
            }
        }

        RtIntensityPairsDouble rtIntensityPairs = new RtIntensityPairsDouble();
        rtIntensityPairs.setRtArray(newRtArray);
        rtIntensityPairs.setIntensityArray(newIntArray);

        return rtIntensityPairs;
    }

    private EmgModelParams getEmgParams(RtIntensityPairsDouble featurePrepared, double[] xInitOptimized, EmgModelParams emgModelParams) {
        Double[] rtArray = featurePrepared.getRtArray();
        double minBound = rtArray[0];
        double maxBound = rtArray[rtArray.length - 1];
        double stdev = Math.sqrt(emgModelParams.getVariance()) * emgModelParams.getToleranceStdevBox();
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

    private double[] getEmgSample(EmgModelParams emgModelParams) {
        double min = emgModelParams.getBoundingBoxMin();
        double max = emgModelParams.getBoundingBoxMax();
        if (max == min) {
            return null;
        }

        double height = emgModelParams.getHeight();
        double width = emgModelParams.getWidth();
        double symmetry = emgModelParams.getSymmetry();
        double retention = emgModelParams.getRetention();
        double step = emgModelParams.getInterpolationStep();

        double[] data = new double[(int) ((max - min) / step) + 2];

        double sqrt2Pi = Math.sqrt(2 * Math.PI);
        double termSq2 = -Constants.EMG_CONST / Math.sqrt(2.0);
        double part1 = height * width / symmetry;
        double part2 = Math.pow(width, 2) / (2 * Math.pow(symmetry, 2));
        double part3 = width / symmetry;
        double position = min;
        double tmp;
        for (int i = 0; position < max; i++) {
            position = min + i * step;
            tmp = position - retention;
            double under = 1 + Math.exp(termSq2 * ((tmp / width) - part3));
            double upper = part1 * sqrt2Pi * Math.exp(part2 - (tmp / symmetry));
            if(Double.isInfinite(under)){
                data[i] = 0;
            }else {
                data[i] = upper / under;
            }
            if(Double.isNaN(data[i])){
                System.out.println("NaN");
            }
        }
        return data;
    }

    private double getValue(double[] data, double rt, EmgModelParams emgModelParams) {
        //key2index
        rt -= emgModelParams.getBoundingBoxMin();
        rt /= emgModelParams.getInterpolationStep();
        int dataMaxIndex = data.length - 1;

        //modf
        if (rt < 0) {
            if (rt > -1) {
                return data[0] * (1 + rt);
            } else {
                return 0;
            }
        } else if (rt > dataMaxIndex) {
            if (rt < dataMaxIndex + 1) {
                return data[dataMaxIndex] * (1 - rt + dataMaxIndex);
            } else {
                return 0;
            }
        } else {
            return data[(int) rt + 1] * (rt - (int) rt) + data[(int) rt] * (1 - rt + (int) rt);
        }

    }

    private double pearsonCorrelationCoefficient(Double[] realData, Double[] modelData) {
        assert realData.length == modelData.length;
        double realDataAverage = MathUtil.getAverage(realData);
        double modelDataAverage = MathUtil.getAverage(modelData);
        double numerator = 0.0d, realDenominator = 0.0d, modelDenominator = 0.0d;
        double realTemp, modelTemp;
        for (int i = 0; i < realData.length; i++) {
            realTemp = realData[i] - realDataAverage;
            modelTemp = modelData[i] - modelDataAverage;
            numerator += realTemp * modelTemp;
            realDenominator += realTemp * realTemp;
            modelDenominator += modelTemp * modelTemp;
        }
        double denominator = Math.sqrt(realDenominator * modelDenominator);
        if (denominator == 0) {
            return -1.0d;
        } else {
            return numerator / denominator;
        }
    }

    /**
     * @param preparedPairs result of prepareElutionFit
     * @param xInit         xInit * 4
     */
    private double[] getLevenbergMarquardtOptimizer(RtIntensityPairsDouble preparedPairs, double[] xInit) {
        double sqrt2Pi = Math.sqrt(2 * Math.PI);
        double sqrt2 = Math.sqrt(2);
        double[] weight = new double[preparedPairs.getRtArray().length];
        for (int i = 0; i < weight.length; i++) {
            weight[i] = 1.0;
        }

        LevenbergMarquardt optimizer = new LevenbergMarquardt() {
            @Override
            public void setValues(double[] parameters, double[] values) {
                double h = parameters[0];
                double w = parameters[1];
                double s = parameters[2];
                double z = parameters[3];
                for (int i = 0; i < preparedPairs.getIntensityArray().length; i++) {
                    double t = preparedPairs.getRtArray()[i];
                    double e = preparedPairs.getIntensityArray()[i];
                    values[i] = (h * w / s) * sqrt2Pi * Math.exp((Math.pow(w, 2) / (2 * Math.pow(s, 2))) - ((t - z) / s)) / (1 + Math.exp((-Constants.EMG_CONST / sqrt2) * (((t - z) / w) - w / s))) - e;
                }
            }
//            @Override
//            public void setDerivatives(double[] parameters, double[][] derivatives) {
//
//            }
        };
        double[][] derivatives = new double[4][preparedPairs.getRtArray().length];
        double h = xInit[0];
        double w = xInit[1];
        double s = xInit[2];
        double z = xInit[3];
        for (int i = 0; i < derivatives[0].length; i++) {
            double t = preparedPairs.getRtArray()[i];
            double exp1 = Math.exp(((w * w) / (2 * s * s)) - ((t - z) / s));
            double exp2 = (1 + Math.exp((-Constants.EMG_CONST / sqrt2) * (((t - z) / w) - w / s)));
            double exp3 = Math.exp((-Constants.EMG_CONST / sqrt2) * (((t - z) / w) - w / s));
            derivatives[0][i] = w / s * sqrt2Pi * exp1 / exp2;
            derivatives[1][i] = h / s * sqrt2Pi * exp1 / exp2 + (h * w * w) / (s * s * s) * sqrt2Pi * exp1 / exp2 + (Constants.EMG_CONST * h * w) / s * sqrt2Pi * exp1 * (-(t - z) / (w * w) - 1 / s) * exp3 / ((exp2 * exp2) * sqrt2);
            derivatives[2][i] = -h * w / (s * s) * sqrt2Pi * exp1 / exp2 + h * w / s * sqrt2Pi * (-(w * w) / (s * s * s) + (t - z) / (s * s)) * exp1 / exp2 + (Constants.EMG_CONST * h * w * w) / (s * s * s) * sqrt2Pi * exp1 * exp3 / ((exp2 * exp2) * sqrt2);
            derivatives[3][i] = h * w / (s * s) * sqrt2Pi * exp1 / exp2 - (Constants.EMG_CONST * h) / s * sqrt2Pi * exp1 * exp3 / ((exp2 * exp2) * sqrt2);
        }
        optimizer.setDerivativeCurrent(derivatives);
        optimizer.setInitialParameters(xInit);
        optimizer.setMaxIteration(Constants.EMG_MAX_ITERATION);
        optimizer.setTargetValues(new double[preparedPairs.getRtArray().length]);
        optimizer.setWeights(weight);
        try {
            optimizer.run();
            return optimizer.getBestFitParameters();
        } catch (SolverException exception) {
            exception.printStackTrace();
            System.out.println("LevenbergMarquardt SolverException.");
            return null;
        }
    }

}
