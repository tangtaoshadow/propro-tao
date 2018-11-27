package com.westlake.air.pecs.algorithm;


import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.impl.RCDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.colt.matrix.linalg.SingularValueDecomposition;
import com.westlake.air.pecs.domain.bean.airus.TrainAndTest;
import com.westlake.air.pecs.domain.bean.airus.TrainData;
import com.westlake.air.pecs.domain.bean.airus.TrainPeaks;
import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import com.westlake.air.pecs.domain.bean.score.SimpleFeatureScores;
import com.westlake.air.pecs.domain.db.simple.SimpleScores;
import com.westlake.air.pecs.utils.AirusUtil;
import com.westlake.air.pecs.utils.ArrayUtil;
import com.westlake.air.pecs.utils.MathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class LDALearner {

    public final Logger logger = LoggerFactory.getLogger(LDALearner.class);

    /**
     * Get clfScore with given confidence(params).
     * 根据weightsMap计算子分数的(加权总分-平均加权总分)
     */
    public void score(TrainData data, HashMap<String, Double> weightsMap) {
        score(data.getTargets(), weightsMap);
        score(data.getDecoys(), weightsMap);
    }

    public void score(List<SimpleScores> scores, HashMap<String, Double> weightsMap){
        Set<Map.Entry<String, Double>> entries =  weightsMap.entrySet();
        for (SimpleScores score : scores) {
            for (FeatureScores featureScores : score.getFeatureScoresList()) {
                double addedScore = 0;
                for (Map.Entry<String, Double> entry : entries) {
                    addedScore += featureScores.getScoresMap().get(entry.getKey()) * entry.getValue();
                }
                featureScores.put(FeatureScores.ScoreType.WeightedTotalScore.getTypeName(), addedScore);
            }
        }
    }

    /**
     * Get clfScore with given confidence(params).
     */
    public Double[] score(Double[][] peaks, Double[] weights, boolean useMainScore) {

        Double[][] featureMatrix = AirusUtil.getFeatureMatrix(peaks, useMainScore);
        if (featureMatrix != null) {
            return MathUtil.dot(featureMatrix, weights);
        } else {
            logger.error("Score Error");
            return null;
        }
    }

    /**
     * Calculate average confidence(weight) of nevals(trainTimes).
     */
    public Double[] averagedWeight(Double[][] weights) {
        Double[] averagedW = new Double[weights[0].length];
        double sum = 0.0;
        for (int i = 0; i < weights[0].length; i++) {
            for (Double[] j : weights) {
                sum += j[i];
            }
            averagedW[i] = sum / weights.length;
            sum = 0;
        }
        return averagedW;
    }

    public Double[] learn(Double[][] decoyPeaks, Double[][] targetPeaks, boolean useMainScore) {
        Double[][] x0 = AirusUtil.getFeatureMatrix(decoyPeaks, useMainScore);
        Double[][] x1 = AirusUtil.getFeatureMatrix(targetPeaks, useMainScore);
        Double[][] x = ArrayUtil.concat3d(x0, x1);
        Double[] y = new Double[x.length];
        Double[] w = new Double[x[0].length];
        for (int i = 0; i < x0.length; i++) {
            y[i] = 0.0;
        }
        for (int i = x0.length; i < x0.length + x1.length; i++) {
            y[i] = 1.0;
        }
        Double[] mu0 = MathUtil.getRowMean(x0);
        Double[] mu1 = MathUtil.getRowMean(x1);
        double[][] xLine0 = new double[x0.length][x[0].length];
        double[][] xLine1 = new double[x1.length][x[0].length];
        for (int i = 0; i < x0.length; i++) {
            for (int j = 0; j < x[0].length; j++) {
                xLine0[i][j] = x0[i][j] - mu0[j];
            }
        }
        for (int i = 0; i < x1.length; i++) {
            for (int j = 0; j < x[0].length; j++) {
                xLine1[i][j] = x1[i][j] - mu1[j];
            }
        }

        /*
         * Colt solution Discriminant Analysis.py
         */
        // 1) Within (univariate) scaling by with classes std-dev
        DoubleMatrix2D Xc = new DenseDoubleMatrix2D(x.length, x[0].length);
        Xc.assign(ArrayUtil.concat3d(xLine0, xLine1));
        double[] std = new double[x[0].length];
        for (int j = 0; j < x[0].length; j++) {
            for (int i = 0; i < x.length; i++) {
                std[j] += Math.pow(Xc.get(i, j), 2);
            }
            std[j] /= x.length;
            std[j] = Math.sqrt(std[j]);
            if (std[j] == 0) std[j] = 1;
        }
        double fac = 1.0 / (x.length - 2);

        // 2) Within variance scaling
        double sqrtFac = Math.sqrt(fac);
        DoubleMatrix2D coltXc = new DenseDoubleMatrix2D(x.length, x[0].length);
        for (int j = 0; j < x[0].length; j++) {
            for (int i = 0; i < x.length; i++) {
                double temp = Xc.get(i, j);
                temp = temp * sqrtFac / std[j];
                coltXc.set(i, j, temp);
            }
        }

        SingularValueDecomposition svdColt = new SingularValueDecomposition(coltXc);
        DoubleMatrix2D S = svdColt.getS();
        DoubleMatrix2D V = svdColt.getV();
        Algebra algebra = new Algebra();
        V = algebra.transpose(V);

        int rank = 0;
        for (int i = 0; i < S.columns(); i++) {
            if (S.get(i, i) > 0.0001) rank++;
        }
        if (rank < S.columns()) {
//            logger.warn("Warning: Variables are collinear.");
        }
        int[] vIndex = new int[V.columns()];
        int[] sIndex = new int[S.columns()];
        for (int i = 0; i < V.columns(); i++) {
            vIndex[i] = i;
        }
        for (int i = 0; i < S.columns(); i++) {
            sIndex[i] = i;
        }
        int[] rankIndex = new int[rank];
        for (int i = 0; i < rank; i++) {
            rankIndex[i] = i;
        }
        V = V.viewSelection(rankIndex, vIndex);
        S = S.viewSelection(rankIndex, sIndex);
        double temp;
        for (int j = 0; j < V.columns(); j++) {
            for (int i = 0; i < V.rows(); i++) {
                temp = V.get(i, j);
                temp = temp / std[j];
                V.set(i, j, temp);
            }
        }
        V = algebra.transpose(V);
        DoubleMatrix2D scalings = new DenseDoubleMatrix2D(V.rows(), V.columns());
        for (int j = 0; j < V.columns(); j++) {
            for (int i = 0; i < V.rows(); i++) {
                scalings.set(i, j, V.get(i, j) / S.get(j, j));
            }
        }

        // 3) Between variance scaling
        temp = Math.sqrt(x.length * 0.5 * fac);
        double[][] means0 = new double[1][x[0].length];
        double[][] means1 = new double[1][x[0].length];
        for (int i = 0; i < x[0].length; i++) {
            means0[0][i] = mu0[i] * 0.5 - mu1[i] * 0.5;
            means0[0][i] *= temp;
            means1[0][i] = -means0[0][i];
        }
        double[][] means = ArrayUtil.concat3d(means0, means1);
        DoubleMatrix2D matMeans = new DenseDoubleMatrix2D(means.length, means[0].length);
        matMeans.assign(means);
        DoubleMatrix2D X = new DenseDoubleMatrix2D(matMeans.rows(), scalings.columns());
        matMeans.zMult(scalings, X);
        X = algebra.transpose(X);
        svdColt = new SingularValueDecomposition(X);
        V = svdColt.getU();

        int[] columnIndex = {0};
        int[] rowIndex = new int[V.rows()];
        for (int i = 0; i < V.rows(); i++) {
            rowIndex[i] = i;
        }
        V = V.viewSelection(rowIndex, columnIndex);
        DoubleMatrix2D matResult = new RCDoubleMatrix2D(scalings.rows(), V.columns());
        scalings.zMult(V, matResult);

        for (int i = 0; i < x[0].length; i++) {
            w[i] = -matResult.get(i, 0);
        }

        return w;
    }

    /**
     * @param trainPeaks
     * @param skipScoreType 需要在结果中剔除的主分数,如果为空则不删除
     * @return key为子分数的名称, value是该子分数的权重值
     */
    public HashMap<String, Double> learn(TrainPeaks trainPeaks, String skipScoreType) throws Exception {

        int totalLength = trainPeaks.getBestTargets().size() + trainPeaks.getTopDecoys().size();

        //计算Targets和Decoys中各个子分数的平均值,skipScoreType为需要略去不计算的子分数类型
        HashMap<String, Double> mu0 = AirusUtil.getScoresMeans(trainPeaks.getTopDecoys(), skipScoreType);
        HashMap<String, Double> mu1 = AirusUtil.getScoresMeans(trainPeaks.getBestTargets(), skipScoreType);

        // w.length子分数数目,需要剔除主分数
        int subscoreTypes = mu0.size();

        if(mu0.size() != mu1.size()){
            throw new Exception("mu0 must be equals to mu1!");
        }

        //targets和decoys数组中的子分数各自减去对应的平均值
        double[][] xLine0 = new double[trainPeaks.getTopDecoys().size()][subscoreTypes];
        double[][] xLine1 = new double[trainPeaks.getBestTargets().size()][subscoreTypes];
        for (int i = 0; i < trainPeaks.getTopDecoys().size(); i++) {
            int j = 0;
            for (String key : mu0.keySet()) {
                try{
                    xLine0[i][j] = trainPeaks.getTopDecoys().get(i).getScoresMap().get(key) - mu0.get(key);
                    j++;
                }catch (Exception e){
                    logger.info(key);
                }

            }
        }
        for (int i = 0; i < trainPeaks.getBestTargets().size(); i++) {
            int j = 0;
            for (String key : mu1.keySet()) {
                xLine1[i][j] = trainPeaks.getBestTargets().get(i).getScoresMap().get(key) - mu1.get(key);
                j++;
            }
        }

        /*
         * Colt solution Discriminant Analysis.py
         */
        // 1) Within (univariate) scaling by with classes std-dev
        DoubleMatrix2D Xc = new DenseDoubleMatrix2D(totalLength, subscoreTypes);
        Xc.assign(Objects.requireNonNull(ArrayUtil.concat3d(xLine0, xLine1)));
        double[] std = new double[subscoreTypes];
        for (int j = 0; j < subscoreTypes; j++) {
            for (int i = 0; i < totalLength; i++) {
                std[j] += Math.pow(Xc.get(i, j), 2);
            }
            std[j] /= totalLength;
            std[j] = Math.sqrt(std[j]);
            if (std[j] == 0) std[j] = 1;
        }
        double fac = 1.0 / (totalLength - 2);

        // 2) Within variance scaling
        double sqrtFac = Math.sqrt(fac);
        DoubleMatrix2D coltXc = new DenseDoubleMatrix2D(totalLength, subscoreTypes);
        for (int j = 0; j < subscoreTypes; j++) {
            for (int i = 0; i < totalLength; i++) {
                double temp = Xc.get(i, j);
                temp = temp * sqrtFac / std[j];
                coltXc.set(i, j, temp);
            }
        }

        SingularValueDecomposition svdColt = new SingularValueDecomposition(coltXc);
        DoubleMatrix2D S = svdColt.getS();
        DoubleMatrix2D V = svdColt.getV();
        Algebra algebra = new Algebra();
        V = algebra.transpose(V);

        int rank = 0;
        for (int i = 0; i < S.columns(); i++) {
            if (S.get(i, i) > 0.0001) rank++;
        }
        if (rank < S.columns()) {
//            logger.warn("Warning: Variables are collinear.");
        }
        int[] vIndex = new int[V.columns()];
        int[] sIndex = new int[S.columns()];
        for (int i = 0; i < V.columns(); i++) {
            vIndex[i] = i;
        }
        for (int i = 0; i < S.columns(); i++) {
            sIndex[i] = i;
        }
        int[] rankIndex = new int[rank];
        for (int i = 0; i < rank; i++) {
            rankIndex[i] = i;
        }
        V = V.viewSelection(rankIndex, vIndex);
        S = S.viewSelection(rankIndex, sIndex);
        double temp;
        for (int j = 0; j < V.columns(); j++) {
            for (int i = 0; i < V.rows(); i++) {
                temp = V.get(i, j);
                temp = temp / std[j];
                V.set(i, j, temp);
            }
        }
        V = algebra.transpose(V);
        DoubleMatrix2D scalings = new DenseDoubleMatrix2D(V.rows(), V.columns());
        for (int j = 0; j < V.columns(); j++) {
            for (int i = 0; i < V.rows(); i++) {
                scalings.set(i, j, V.get(i, j) / S.get(j, j));
            }
        }

        // 3) Between variance scaling
        temp = Math.sqrt(totalLength * 0.5 * fac);
        double[][] means0 = new double[1][subscoreTypes];
        double[][] means1 = new double[1][subscoreTypes];
        int tempI = 0;
        for (String key : mu0.keySet()) {
            means0[0][tempI] = mu0.get(key) * 0.5 - mu1.get(key) * 0.5;
            means0[0][tempI] *= temp;
            means1[0][tempI] = -means0[0][tempI];
            tempI++;
        }
        double[][] means = ArrayUtil.concat3d(means0, means1);
        DoubleMatrix2D matMeans = new DenseDoubleMatrix2D(means.length, means[0].length);
        matMeans.assign(means);
        DoubleMatrix2D X = new DenseDoubleMatrix2D(matMeans.rows(), scalings.columns());
        matMeans.zMult(scalings, X);
        X = algebra.transpose(X);
        svdColt = new SingularValueDecomposition(X);
        V = svdColt.getU();

        int[] columnIndex = {0};
        int[] rowIndex = new int[V.rows()];
        for (int i = 0; i < V.rows(); i++) {
            rowIndex[i] = i;
        }
        V = V.viewSelection(rowIndex, columnIndex);
        DoubleMatrix2D matResult = new RCDoubleMatrix2D(scalings.rows(), V.columns());
        scalings.zMult(V, matResult);
//        for (int i = 0; i < subscoreTypes; i++) {
//            weights[i] = -matResult.get(i, 0);
//        }

        HashMap<String, Double> weightsMap = new HashMap<>();
        int tempJ = 0;
        for (String key : mu0.keySet()) {
            weightsMap.put(key, -matResult.get(tempJ, 0));
            tempJ++;
        }
        return weightsMap;
    }

}
