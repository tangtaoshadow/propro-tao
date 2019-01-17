package com.westlake.air.pecs.algorithm.PeakSplitter;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.exception.*;
import org.apache.commons.math3.fitting.AbstractCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.linear.DiagonalMatrix;
import org.apache.commons.math3.util.FastMath;

import java.util.*;

/**
 * Created by Nico Wang
 * Time: 2018-12-19 17:07
 */
public class MultiGaussianFitter extends AbstractCurveFitter {

    private final double[] initialGuess;
    private final int maxIter;
    private final int count;

    private static final Parametric FUNCTION = new Parametric() {
        public double value(double x, double... p) {
            double v = 1.0D / 0.0;

            try {
                v = super.value(x, p);
            } catch (NotStrictlyPositiveException var7) {
                ;
            }

            return v;
        }

        public double[] gradient(double x, double... p) {
            double[] v = new double[p.length];
            for(int index = 0; index < v.length; index ++){
                v[index] = 1.0D / 0.0;
            }
            try {
                v = super.gradient(x, p);
            } catch (NotStrictlyPositiveException var6) {
                ;
            }

            return v;
        }
    };


    private MultiGaussianFitter(double[] initialGuess, int maxIter, int count) {
        this.initialGuess = initialGuess;
        this.maxIter = maxIter;
        this.count = count;
    }

    public static MultiGaussianFitter create() {
        return new MultiGaussianFitter((double[])null, 2147483647, 1);
    }
    public MultiGaussianFitter withStartPoint(double[] newStart) {
        return new MultiGaussianFitter((double[])newStart.clone(), this.maxIter, this.count);
    }

    public MultiGaussianFitter withMaxIterations(int newMaxIter) {
        return new MultiGaussianFitter(this.initialGuess, newMaxIter, this.count);
    }
    public MultiGaussianFitter withCount(int count) {
        return new MultiGaussianFitter(this.initialGuess, this.maxIter, count);
    }

    @Override
    protected LeastSquaresProblem getProblem(Collection<WeightedObservedPoint> observations) {
        int len = observations.size();
        double[] target = new double[len];
        double[] weights = new double[len];
        int i = 0;

        for(Iterator i$ = observations.iterator(); i$.hasNext(); ++i) {
            WeightedObservedPoint obs = (WeightedObservedPoint)i$.next();
            target[i] = obs.getY();
            weights[i] = obs.getWeight();
        }

        TheoreticalValuesFunction model = new TheoreticalValuesFunction(FUNCTION, observations);
        double[] startPoint = (new GaussParamGuesser(observations, this.count)).guess();
        return (new LeastSquaresBuilder()).maxEvaluations(2147483647).maxIterations(this.maxIter).start(startPoint).target(target).weight(new DiagonalMatrix(weights)).model(model.getModelFunction(), model.getModelFunctionJacobian()).build();

    }

    //modified
    public static class Parametric implements ParametricUnivariateFunction {
//        public Parametric() {
//        }

        //param[1]* np.exp(-np.power(x - param[3], 2.) / (2 * np.power(param[5], 2.)))
        public double value(double x, double... param) throws NullArgumentException, DimensionMismatchException, NotStrictlyPositiveException {
            double result = 0;
            int count = param.length/3;
            double[] tmpParam = new double[3];
            for(int i=0; i<count; i++){
                System.arraycopy(param, i * 3, tmpParam, 0, 3);
                result += valueSub(x, tmpParam);
            }
            return result;
        }

        private double valueSub(double x, double... param) throws NullArgumentException, DimensionMismatchException, NotStrictlyPositiveException {
            this.validateParameters(param);
            double diff = x - param[1];
            double i2s2 = 1.0D / (2.0D * param[2] * param[2]);
            return gaussValue(diff, param[0], i2s2);
        }

        public double[] gradient(double x, double... param) throws NullArgumentException, DimensionMismatchException, NotStrictlyPositiveException {
            int count = param.length/3;
            double[] tmpParam = new double[3];
            double[] tmpGradient;
            double[] result = new double[param.length];
            for(int i=0; i<count; i++){
                System.arraycopy(param, i * 3, tmpParam, 0, 3);
                tmpGradient = gradientSub(x, tmpParam);
                System.arraycopy(tmpGradient, 0, result, i * 3, 3);
            }
            return result;
        }

        private double[] gradientSub(double x, double... param) throws NullArgumentException, DimensionMismatchException, NotStrictlyPositiveException {
            this.validateParameters(param);
            double norm = param[0];
            double diff = x - param[1];
            double sigma = param[2];
            double i2s2 = 1.0D / (2.0D * sigma * sigma);
            double n = gaussValue(diff, 1.0D, i2s2);
            double m = norm * n * 2.0D * i2s2 * diff;
            double s = m * diff / sigma;
            return new double[]{n, m, s};
        }

        private void validateParameters(double[] param) throws NullArgumentException, DimensionMismatchException, NotStrictlyPositiveException {
            if (param == null) {
                throw new NullArgumentException();
            } else if (param.length % 3 != 0) {
                throw new DimensionMismatchException(param.length, 3);
            } else if (param[2] <= 0.0D) {
                throw new NotStrictlyPositiveException(param[2]);
            }
        }
    }

    private static double gaussValue(double xMinusMean, double norm, double i2s2) {
        return norm * FastMath.exp(-xMinusMean * xMinusMean * i2s2);
    }
}



















