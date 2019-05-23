package com.westlake.air.propro.algorithm.scorer;

import com.westlake.air.propro.utils.MathUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nico Wang
 * Time: 2019-03-01 20:08
 */
public class IntensityScore {
    private double delta = 0.1d;
    private double deltaSlope = 0.01d;
    private double slopeStep = 0.2d;
    private double slopeThreshold = 0.001d;

    public double getIntensityScore(List<Double> libIntList, List<Double> expIntList){
        double slope = getSlope(libIntList, expIntList);
        double score = 0;
        for (int i=0; i<libIntList.size(); i++){
            score += (1d - shiftedSigmoid(Math.abs(expIntList.get(i) - libIntList.get(i) * slope)/(libIntList.get(i)*slope)));
        }
        score = score/libIntList.size();
        return score;
    }

    public double getSlope(List<Double> libIntList, List<Double> expIntList){
        double slope = MathUtil.sum(expIntList) / MathUtil.sum(libIntList);
//        double loss = getLoss(libIntList, expIntList, slope);
        double lastSlope = Double.MAX_VALUE;
//        System.out.println("Slope: "+slope+", loss: " + loss);
        while (Math.abs(slope - lastSlope) >= slopeThreshold) {
            lastSlope = slope;
            slope = updateSlope(libIntList, expIntList, slope);
//            loss = getLoss(libIntList, expIntList, slope);
//            System.out.println("Slope: "+slope+", loss: " + loss);
        }
        return slope;
    }

    private double getLoss(List<Double> libIntList, List<Double> expIntList, double slope){
        List<Double> normalizedDiffs = new ArrayList<>();
        for (int index = 0; index < libIntList.size(); index ++){
            double diff = libIntList.get(index) * slope - expIntList.get(index);
            double normalizedDiff = Math.abs(diff) / (libIntList.get(index) * slope);
            normalizedDiffs.add(normalizedDiff);
        }
        return huberLoss(normalizedDiffs, delta);
    }


    /**
     * Huber Loss 是一个用于回归问题的带参损失函数, 优点是能增强平方误差损失函数(MSE, mean square error)对离群点的鲁棒性。
     * @param diffs 归一化后的diff List
     * @param delta 线性误差与平方误差的阈值
     * @return
     */
    private double huberLoss(List<Double> diffs, double delta){
        double loss = 0;
        for (double diff: diffs){
            if (diff <= delta){
                loss += 0.5 * diff * diff;
            }else {
                loss += delta * diff - 0.5 * delta * delta;
            }
        }
        return loss;
    }

    private double shiftedSigmoid(double value){
//        =2/(1+EXP(-2*A4))-1
        return 2d/(1 + Math.exp(-2d * value)) - 1;
    }

    private double getGradient(List<Double> libIntList, List<Double> expIntList, double slope){
        double loss = getLoss(libIntList, expIntList, slope - deltaSlope);
        double deltaLoss = getLoss(libIntList, expIntList, slope + deltaSlope) - loss;
        return deltaLoss/deltaSlope/2d;
    }

    private double updateSlope(List<Double> libIntList, List<Double> expIntList, double slope){
        double gradient = getGradient(libIntList, expIntList, slope);
//        slope -= gradient * slopeStep;
        slope -= Math.random() * gradient * slopeStep;
        return slope;
    }

//    @Test
//    public void test(){
//        List<Double> expList = new ArrayList<>();
//        expList.add(0.1d);expList.add(0.2d);expList.add(0.3d);expList.add(0.4d);
//        List<Double> libList = new ArrayList<>();
//        libList.add(0.05d);libList.add(0.1d);libList.add(0.15d);libList.add(0.7d);
//        System.out.println("Score: " + getIntensityScore(libList, expList));
//
//        PeakGroup peakGroup = new PeakGroup();
//        HashMap<String, Double> ionIntMap = new HashMap<>();
//        HashMap<String, Double> libIntMap = new HashMap<>();
//        for (Integer i=0; i<libList.size(); i++){
//            ionIntMap.put(i.toString(), expList.get(i));
//            libIntMap.put(i.toString(), libList.get(i));
//        }
//        peakGroup.setIonIntensity(ionIntMap);
//        peakGroup.setPeakGroupInt(1);
//        FeatureScores featureScores = new FeatureScores();
//        LibraryScorer libraryScorer = new LibraryScorer();
//        libraryScorer.calculateLibraryScores(peakGroup, libIntMap, featureScores, null);
//        System.out.println("finish");
//    }
}
