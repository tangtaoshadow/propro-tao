package com.westlake.air.propro.algorithm.fitter;

import com.westlake.air.propro.domain.bean.score.SlopeIntercept;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by Nico Wang
 * Time: 2019-05-23 22:34
 */
@Component("linearFitter")
public class LinearFitter {

    /**
     * 最小二乘法线性拟合RTPairs
     */
    public SlopeIntercept leastSquare(List<Pair<Double,Double>> rtPairs) {
        WeightedObservedPoints obs = new WeightedObservedPoints();
        for (Pair<Double,Double> rtPair : rtPairs) {
            obs.add(rtPair.getRight(), rtPair.getLeft());
        }
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
        double[] coeff = fitter.fit(obs.toList());
        SlopeIntercept slopeIntercept = new SlopeIntercept();
        slopeIntercept.setSlope(coeff[1]);
        slopeIntercept.setIntercept(coeff[0]);
        return slopeIntercept;
    }

    public SlopeIntercept huberFit(List<Pair<Double, Double>> rtPairs){
        double tolerance = 0.001d;
        SlopeIntercept lastSlopeIntercept = getInitSlopeIntercept(rtPairs);
        SlopeIntercept slopeIntercept = updateHuberSlopeIntercept(rtPairs, lastSlopeIntercept);
        int count = 1;
        while (Math.abs(getHuberSlopeGradient(rtPairs, slopeIntercept)) > tolerance
                || Math.abs(getHuberInterceptGradient(rtPairs, slopeIntercept)) > tolerance){
            slopeIntercept = updateHuberSlopeIntercept(rtPairs, slopeIntercept);
            count ++;
        }
        System.out.println("----------------------- Huber " + count + " epochs -----------------------");
        return slopeIntercept;
    }
    public SlopeIntercept proproFit(List<Pair<Double, Double>> rtPairs){
        double tolerance = 0.001d;
        SlopeIntercept lastSlopeIntercept = getInitSlopeIntercept(rtPairs);
        SlopeIntercept slopeIntercept = updateProproSlopeIntercept(rtPairs, lastSlopeIntercept);
        int count = 1;
        while (Math.abs(getProproSlopeGradient(rtPairs, slopeIntercept)) > tolerance
                || Math.abs(getProproInterceptGradient(rtPairs, slopeIntercept)) > tolerance){
            slopeIntercept = updateProproSlopeIntercept(rtPairs, slopeIntercept);
            count ++;
        }
        System.out.println("----------------------- Propro " + count + " epochs -----------------------");
        return slopeIntercept;
    }
    private double getHuberLoss(List<Pair<Double, Double>> rtPairs, double slope, double intercept){
        double loss = 0d, delta = 1d;
        for (Pair<Double,Double> rtPair: rtPairs){
            double tempDiff = Math.abs(rtPair.getRight() * slope + intercept - rtPair.getLeft());
            if (tempDiff <= delta){
                loss += 0.5 * tempDiff * tempDiff;
            }else {
                loss += delta * tempDiff - 0.5 * delta * delta;
            }
        }
        return loss;
    }

    private double getProproLoss(List<Pair<Double, Double>> rtPairs, double slope, double intercept){
        double loss = 0d, delta = 1d;
        for (Pair<Double,Double> rtPair: rtPairs){
            double tempDiff = Math.abs(rtPair.getRight() * slope + intercept - rtPair.getLeft());
            if (tempDiff <= delta){
                loss += 0.5 * tempDiff * tempDiff;
            }else {
                loss += (Math.log(tempDiff) - Math.log(delta) + 0.5d) * delta * delta;
            }
        }
        return loss;
    }
    private double getHuberSlopeGradient(List<Pair<Double, Double>> rtPairs, SlopeIntercept slopeIntercept){
        double deltaSlope = 0.00000001d;
        double loss = getHuberLoss(rtPairs, slopeIntercept.getSlope() - deltaSlope, slopeIntercept.getIntercept());
        double deltaLoss = getHuberLoss(rtPairs, slopeIntercept.getSlope() + deltaSlope, slopeIntercept.getIntercept()) - loss;
        return deltaLoss/deltaSlope/2d;
    }

    private double getProproSlopeGradient(List<Pair<Double, Double>> rtPairs, SlopeIntercept slopeIntercept){
        double deltaSlope = 0.00000001d;
        double loss = getProproLoss(rtPairs, slopeIntercept.getSlope() - deltaSlope, slopeIntercept.getIntercept());
        double deltaLoss = getProproLoss(rtPairs, slopeIntercept.getSlope() + deltaSlope, slopeIntercept.getIntercept()) - loss;
        return deltaLoss/deltaSlope/2d;
    }
    private double getHuberInterceptGradient(List<Pair<Double, Double>> rtPairs, SlopeIntercept slopeIntercept){
        double deltaIntercept = 0.00000001d;
        double loss = getHuberLoss(rtPairs, slopeIntercept.getSlope(), slopeIntercept.getIntercept() - deltaIntercept);
        double deltaLoss = getHuberLoss(rtPairs, slopeIntercept.getSlope(), slopeIntercept.getIntercept() + deltaIntercept) - loss;
        return deltaLoss/deltaIntercept/2d;
    }

    private double getProproInterceptGradient(List<Pair<Double, Double>> rtPairs, SlopeIntercept slopeIntercept){
        double deltaIntercept = 0.00000001d;
        double loss = getProproLoss(rtPairs, slopeIntercept.getSlope(), slopeIntercept.getIntercept() - deltaIntercept);
        double deltaLoss = getProproLoss(rtPairs, slopeIntercept.getSlope(), slopeIntercept.getIntercept() + deltaIntercept) - loss;
        return deltaLoss/deltaIntercept/2d;
    }
    private SlopeIntercept updateHuberSlopeIntercept(List<Pair<Double, Double>> rtPairs, SlopeIntercept slopeIntercept){
        double slopeStep = 0.000001d, interceptStep = 0.1d;
        double sigma = 1d;
        double oriLoss = getHuberLoss(rtPairs, slopeIntercept.getSlope(), slopeIntercept.getIntercept());
        double slopeGradient = getHuberSlopeGradient(rtPairs, slopeIntercept);
        double interceptGradient = getHuberInterceptGradient(rtPairs, slopeIntercept);
        double intercept = slopeIntercept.getIntercept() - sigma * Math.random() * interceptStep * interceptGradient;
        double slope = slopeIntercept.getSlope() - sigma * Math.random() * slopeStep * slopeGradient;
        double updatedLoss = getHuberLoss(rtPairs, slope, intercept);
        while (updatedLoss > oriLoss){
            sigma = sigma / 2d;
            slope = slopeIntercept.getSlope() - sigma * Math.random() * slopeStep * slopeGradient;
            intercept = slopeIntercept.getIntercept() - sigma * Math.random() * interceptStep * interceptGradient;
            updatedLoss = getHuberLoss(rtPairs, slope, intercept);
        }
        System.out.println("Huber: " + updatedLoss + ", " + slope + ", " + intercept);
        return new SlopeIntercept(slope, intercept);
    }

    private SlopeIntercept updateProproSlopeIntercept(List<Pair<Double, Double>> rtPairs, SlopeIntercept slopeIntercept){
        double slopeStep = 0.00000001d, interceptStep = 0.1d;
        double sigma = 1d;
        double oriLoss = getProproLoss(rtPairs, slopeIntercept.getSlope(), slopeIntercept.getIntercept());
        double slopeGradient = getProproSlopeGradient(rtPairs, slopeIntercept);
        double interceptGradient = getProproInterceptGradient(rtPairs, slopeIntercept);
        double intercept = slopeIntercept.getIntercept() - sigma * Math.random() * interceptStep * interceptGradient;
        double slope = slopeIntercept.getSlope() - sigma * Math.random() * slopeStep * slopeGradient;
        double updatedLoss = getProproLoss(rtPairs, slope, intercept);
        while (updatedLoss > oriLoss){
            sigma = sigma / 2d;
            slope = slopeIntercept.getSlope() - sigma * Math.random() * slopeStep * slopeGradient;
            intercept = slopeIntercept.getIntercept() - sigma * Math.random() * interceptStep * interceptGradient;
            updatedLoss = getProproLoss(rtPairs, slope, intercept);
        }
        System.out.println("Propro: " + updatedLoss + ", " + slope + ", " + intercept);
        return new SlopeIntercept(slope, intercept);
    }

    private SlopeIntercept getInitSlopeIntercept(List<Pair<Double,Double>> rtPairs){
        double minLibRT = Double.MAX_VALUE;
        for (Pair<Double,Double> pair:rtPairs){
            if (pair.getLeft() < minLibRT){
                minLibRT = pair.getLeft();
            }
        }
        double max = Double.MIN_VALUE, min = Double.MAX_VALUE;
        int maxIndex = 0, minIndex = 0;
        for (int i=0; i<rtPairs.size(); i++){
            double product = (rtPairs.get(i).getLeft()-minLibRT + 10) * rtPairs.get(i).getRight();
            if (product > max){
                max = product;
                maxIndex = i;
            }
            if (product < min){
                min = product;
                minIndex = i;
            }
        }
        double slope = (rtPairs.get(maxIndex).getLeft() - rtPairs.get(minIndex).getLeft())
                /(rtPairs.get(maxIndex).getRight() - rtPairs.get(minIndex).getRight());
        double intercept = rtPairs.get(maxIndex).getLeft() - rtPairs.get(maxIndex).getRight() * slope;
        return new SlopeIntercept(slope, intercept);
    }
}
