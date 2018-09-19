package com.westlake.air.pecs.algorithm;


import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.impl.RCDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.colt.matrix.linalg.SingularValueDecomposition;
import com.westlake.air.pecs.utils.AirusUtils;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.utils.ArrayUtils;
import org.springframework.stereotype.Component;

@Component
public class LDALearner {

    /**
     * Get clfScore with given confidence(params).
     */
    public ResultDO<Double[]> score(Double[][] peaks, Double[] params, boolean useMainScore) {
        ResultDO<Double[]> resultDO = new ResultDO<Double[]>();
        Double[][] featureMatrix = AirusUtils.getFeatureMatrix(peaks, useMainScore).getModel();
        if (featureMatrix != null) {
            resultDO = ArrayUtils.dot(featureMatrix, params);
            return resultDO;
        } else {
            resultDO.setMsgInfo("Score Error.\n");
            return resultDO;
        }
    }

    /**
     * Calculate average confidence(weight) of nevals(ssNumIter).
     */
    public Double[] averagedWeight(Double[][] weight){
        Double[] averagedW = new Double[weight[0].length];
        double sum = 0.0;
        for(int i=0;i<weight[0].length;i++){
            for(Double[] j : weight){
                sum += j[i];
            }
            averagedW[i] = sum / weight.length;
            sum =0;
        }
        return averagedW;
    }

    public Double[] learn(Double[][] decoyPeaks, Double[][] targetPeaks, boolean useMainScore){
        Double[][] x0 = AirusUtils.getFeatureMatrix(decoyPeaks,useMainScore).getModel();
        Double[][] x1 = AirusUtils.getFeatureMatrix(targetPeaks,useMainScore).getModel();
        Double[][] x = ArrayUtils.concat3d(x0,x1).getModel();
        Double[] y = new Double[x.length];
        Double[] w = new Double[x[0].length];
        for(int i=0;i<x0.length;i++){
            y[i] = 0.0;
        }
        for(int i = x0.length;i<x0.length+x1.length;i++){
            y[i] = 1.0;
        }
        Double[] mu0 = ArrayUtils.getRowMean(x0);
        Double[] mu1 = ArrayUtils.getRowMean(x1);
        double[][] xLine0 = new double[x0.length][x[0].length];
        double[][] xLine1 = new double[x1.length][x[0].length];
        for(int i=0;i<x0.length;i++) {
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
        DoubleMatrix2D Xc = new DenseDoubleMatrix2D(x.length,x[0].length);
        Xc.assign(ArrayUtils.concat3d(xLine0,xLine1).getModel());
        double[] std = new double[x[0].length];
        for(int j=0;j<x[0].length;j++){
            for(int i=0;i<x.length;i++){
                std[j] += Math.pow(Xc.get(i,j),2);
            }
            std[j] /= x.length;
            std[j] = Math.sqrt(std[j]);
            if(std[j] == 0) std[j] =1;
        }
        double fac = 1.0/(x.length -2);

        // 2) Within variance scaling
        double sqrtFac = Math.sqrt(fac);
        DoubleMatrix2D coltXc = new DenseDoubleMatrix2D(x.length,x[0].length);
        for(int j=0;j<x[0].length;j++) {
            for (int i = 0; i < x.length; i++) {
                double temp = Xc.get(i,j);
                temp = temp * sqrtFac / std[j];
                coltXc.set(i,j,temp);
            }
        }

        SingularValueDecomposition svdColt = new SingularValueDecomposition(coltXc);
        DoubleMatrix2D S = svdColt.getS();
        DoubleMatrix2D V = svdColt.getV();
        Algebra algebra = new Algebra();
        V = algebra.transpose(V);

        int rank =0;
        for(int i=0;i<S.columns();i++){
            if(S.get(i,i)>0.0001) rank++;
        }
        if(rank<S.columns()){
            System.out.println("Warning: Variables are collinear.");
        }
        int[] vIndex = new int[V.columns()];
        int[] sIndex = new int[S.columns()];
        for(int i=0;i<V.columns();i++){
            vIndex[i] =i;
        }
        for(int i=0;i<S.columns();i++){
            sIndex[i] =i;
        }
        int[] rankIndex = new int[rank];
        for(int i=0;i<rank;i++){
            rankIndex[i] = i;
        }
        V = V.viewSelection(rankIndex,vIndex);
        S = S.viewSelection(rankIndex,sIndex);
        double temp;
        for(int j=0;j<V.columns();j++) {
            for (int i = 0; i < V.rows(); i++) {
                temp = V.get(i,j);
                temp = temp / std[j];
                V.set(i,j,temp);
            }
        }
        V= algebra.transpose(V);
        DoubleMatrix2D scalings = new DenseDoubleMatrix2D(V.rows(),V.columns());
        for(int j=0;j<V.columns();j++) {
            for (int i = 0; i < V.rows(); i++) {
                scalings.set(i,j,V.get(i,j)/S.get(j,j));
            }
        }

        // 3) Between variance scaling
        temp = Math.sqrt(x.length*0.5*fac);
        double[][] means0 = new double[1][x[0].length];
        double[][] means1 = new double[1][x[0].length];
        for(int i=0;i<x[0].length;i++){
            means0[0][i] = mu0[i]*0.5 - mu1[i]*0.5;
            means0[0][i] *= temp;
            means1[0][i] = -means0[0][i];
        }
        double[][] means = ArrayUtils.concat3d(means0,means1).getModel();
        DoubleMatrix2D matMeans = new DenseDoubleMatrix2D(means.length,means[0].length);
        matMeans.assign(means);
        DoubleMatrix2D X = new DenseDoubleMatrix2D(matMeans.rows(),scalings.columns());
        matMeans.zMult(scalings, X);
        X = algebra.transpose(X);
        svdColt = new SingularValueDecomposition(X);
        V = svdColt.getU();

        int[] columnIndex = {0};
        int[] rowIndex = new int[V.rows()];
        for(int i=0;i<V.rows();i++){
            rowIndex[i] = i;
        }
        V=V.viewSelection(rowIndex,columnIndex);
        DoubleMatrix2D matResult = new RCDoubleMatrix2D(scalings.rows(),V.columns());
        scalings.zMult(V,matResult);

        for(int i=0;i<x[0].length;i++){
            w[i] = -matResult.get(i,0);
        }

        return w;
    }

}
