package com.westlake.air.propro.algorithm.PeakSplitter;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.apache.commons.math3.exception.ZeroException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.util.FastMath;

import java.util.*;

/**
 * Created by Nico Wang
 * Time: 2018-12-20 13:13
 */
public class GaussParamGuesser  {
    private final List<Double> norm = new ArrayList<>();
    private final List<Double> mean = new ArrayList<>();
    private final List<Double> sigma = new ArrayList<>();
    //percentage of k gaining or losing percent threshold to define hidden peaks
    private final double margin = 0.5;
    private final double weight = 100;
    private final Strategy strategy = Strategy.Gradient;
    public enum Strategy{Count, Intensity, Gradient};

    public GaussParamGuesser(Collection<WeightedObservedPoint> observations, int count) {
        if (observations == null) {
            throw new NullArgumentException(LocalizedFormats.INPUT_ARRAY, new Object[0]);
        } else if (observations.size() < 3) {
            throw new NumberIsTooSmallException(observations.size(), 3, true);
        } else {
            double[] params;
            List<WeightedObservedPoint> sorted;
            switch (strategy){
                case Count:
                    sorted = this.sortObservations(observations);
                    params = this.basicGuess(sorted.toArray(new WeightedObservedPoint[0]));
                    System.out.println(JSON.toJSON(params));
                    for(int i=0; i<count; i++){
                        this.norm.add(params[0]);
                        this.mean.add((sorted.get(sorted.size()-1).getX() - sorted.get(0).getX()) / count*(i + 1) + sorted.get(0).getX());
                        this.sigma.add(params[2]);
                    }
                    break;
                case Intensity:
                    List<WeightedObservedPoint> tmpObs = new ArrayList<>();
                    boolean downHill = false;
                    WeightedObservedPoint lastPoint = new WeightedObservedPoint(0,0,0);

                    for(WeightedObservedPoint point: observations){
                        if(point.getY() < lastPoint.getY()){
                            downHill = true;
                        }
                        if(downHill && point.getY() > lastPoint.getY()){
                            downHill = false;
                            sorted = this.sortObservations(tmpObs);
                            params = this.basicGuess(sorted.toArray(new WeightedObservedPoint[0]));
                            this.norm.add(params[0]);
                            this.mean.add(params[1]);
                            this.sigma.add(params[2]);
                            tmpObs = new ArrayList<>();
                            tmpObs.add(lastPoint);
                        }
                        tmpObs.add(point);
                        lastPoint = point;
                    }
                    sorted = this.sortObservations(tmpObs);
                    params = this.basicGuess(sorted.toArray(new WeightedObservedPoint[0]));
                    this.norm.add(params[0]);
                    this.mean.add(params[1]);
                    this.sigma.add(params[2]);
                    break;
                case Gradient:
                    List<WeightedObservedPoint> obsList = Lists.newArrayList(observations);
                    List<Double> residualList = new ArrayList<>();
//                    List<Integer> localMaxIndexs = new ArrayList<>();
//                    List<Integer> localBoundaryIndex = new ArrayList<>();
//                    List<Double> localMaxX = new ArrayList<>();
                    List<Double> localBoundaryX = new ArrayList<>();
                    //get residual
                    residualList.add(obsList.get(1).getY()-obsList.get(0).getY());
                    localBoundaryX.add(obsList.get(0).getX());
                    localBoundaryX.add(obsList.get(obsList.size()-1).getX());
                    for(int i=1; i<obsList.size()-1; i++){
                        residualList.add(obsList.get(i+1).getY() - obsList.get(i).getY());
                        if(obsList.get(i).getY() == 0){
                            localBoundaryX.add(obsList.get(i).getX());
                        }
                    }
                    //find local max index
                    double left, mid = 0, right = 0;
                    for (int i=1; i<residualList.size()-1; i++){
                        left = residualList.get(i-1);
                        mid = residualList.get(i);
                        right = residualList.get(i+1);
                        //
                        if (left > 0){
                            if (mid < 0){
                                this.mean.add(obsList.get(i).getX());
                                this.norm.add(obsList.get(i).getY());
                            } else if (mid == 0 && right < 0){
                                this.mean.add((obsList.get(i).getX() + obsList.get(i+1).getX())/2.0D);
                                this.norm.add(obsList.get(i).getY());
                                i++;
                            } else if (mid >=0 && right >0 && mid <= left*(1-margin) && mid <= right*(1-margin)){
                                this.mean.add(obsList.get(i).getX());
                                this.norm.add(obsList.get(i).getY());
                                localBoundaryX.add(obsList.get(i+1).getX());
                                i++;
                            }
                        } else if (left == 0){
                            if (mid > 0){
                                localBoundaryX.add(obsList.get(i).getX());
                            } else if (mid < 0){
                                this.mean.add(obsList.get(i).getX());
                                this.norm.add(obsList.get(i).getY());
                            }
                        } else if (left < 0){
                            if(mid > 0){
                                localBoundaryX.add(obsList.get(i).getX());
                            } else if (mid == 0 && right > 0){
                                localBoundaryX.add((obsList.get(i).getX() + obsList.get(i+1).getX())/2.0D);
                                i++;
                            } else if (mid <= 0 && right < 0 && mid >= left*(1-margin) && mid >= right*(1-margin)){
                                localBoundaryX.add(obsList.get(i).getX());
                                this.mean.add(obsList.get(i+1).getX());
                                this.norm.add(obsList.get(i+1).getY());
                                i++;
                            }
                        }
                        //
//                        if(left > 0 && mid <= 0){
//                            localMaxIndexs.add(i);
//                        }else if(left <= 0 && mid > 0){
//                            localBoundaryIndex.add(i);
//                        } else if(left > 0 && mid > 0 && right > 0 && mid < left*(1-margin) && mid < right*(1-margin)){
//                            localMaxIndexs.add(i);
//                            localBoundaryIndex.add(i+1);
//                        }else if(left <= 0 && mid <=0 && right < 0 && mid >= left*(1-margin) && mid > right*(1-margin)){
//                            localMaxIndexs.add(i+1);
//                            localBoundaryIndex.add(i);
//                        }
                    }
                    Collections.sort(localBoundaryX);
                    int i=0;
                    for(int j=0; i<this.mean.size(); j++){
                        if (localBoundaryX.get(j) > this.mean.get(i)) {
                            this.sigma.add((localBoundaryX.get(j) - localBoundaryX.get(j - 1)) / (2.0D * FastMath.sqrt(2.0D * FastMath.log(2.0D))));
                            i++;
                        }
                    }
//                    if(mid >= 0 && right < 0) {
//                        localMaxIndexs.add(residualList.size()-1);
//                    }else if(mid <= 0 && right > 0){
//                        localBoundaryIndex.add(residualList.size()-1);
//                    }

//                    //change weight
//                    for(int i=0; i<localMaxIndexs.size(); i++){
//                        WeightedObservedPoint point = obsList.get(localMaxIndexs.get(i));
//                        point = new WeightedObservedPoint(this.weight, point.getX(), point.getY());
//                        obsList.set(localMaxIndexs.get(i), point);
//                    }

//                    sorted = this.sortObservations(observations);
//                    params = this.basicGuess(sorted.toArray(new WeightedObservedPoint[0]));
//                    System.out.println(JSON.toJSON(params));

//                    for (int i=0; i<localMaxIndexs.size(); i++){
////                        if(i == 0){
////                            left = (obsList.get(localMaxIndexs.get(i)).getX() + obsList.get(0).getX())/2;
////                            right = (obsList.get(localMaxIndexs.get(i+1)).getX() + obsList.get(localMaxIndexs.get(i)).getX())/2;
////                        }else if(i != localMaxIndexs.size()-1){
////                            left = right;
////                            right = (obsList.get(localMaxIndexs.get(i+1)).getX() + obsList.get(localMaxIndexs.get(i)).getX())/2;
////                        }else{
////                            left = right;
////                            right = (obsList.get(localMaxIndexs.get(i)).getX() + obsList.get(obsList.size()-1).getX())/2;
////                        }
//                        if(i == 0){
//                            left = (obsList.get(localMaxIndexs.get(i)).getX() + obsList.get(0).getX())/2;
//                            right = obsList.get(localBoundaryIndex.get(i)).getX();
//                        }else if(i != localMaxIndexs.size()-1){
//                            left = right;
//                            right = obsList.get(localBoundaryIndex.get(i)).getX();
//                        }else{
//                            left = right;
//                            right = (obsList.get(localMaxIndexs.get(i)).getX() + obsList.get(obsList.size()-1).getX())/2;
//                        }
//                        this.norm.add(obsList.get(localMaxIndexs.get(i)).getY());
//                        this.mean.add((left + right)/2.0D);
//                        this.sigma.add((right-left)/ (2.0D * FastMath.sqrt(2.0D * FastMath.log(2.0D))));
//                    }
                    break;
            }
            System.out.println("成功获得预估参数。");
            System.out.println("norm: " + JSON.toJSON(this.norm));
            System.out.println("mean: " + JSON.toJSON(this.mean));
            System.out.println("sigma: " + JSON.toJSON(this.sigma));
            System.out.println(System.currentTimeMillis());
        }
    }

    public double[] guess() {
        int length = this.norm.size();
        double[] result = new double[length * 3];
        for(int i=0; i<length; i++){
            result[3 * i] = this.norm.get(i);
            result[3 * i + 1] = this.mean.get(i);
            result[3 * i + 2] = this.sigma.get(i);
        }
        return result;
    }

    private List<WeightedObservedPoint> sortObservations(Collection<WeightedObservedPoint> unsorted) {
        List<WeightedObservedPoint> observations = new ArrayList(unsorted);
        Comparator<WeightedObservedPoint> cmp = new Comparator<WeightedObservedPoint>() {
            public int compare(WeightedObservedPoint p1, WeightedObservedPoint p2) {
                if (p1 == null && p2 == null) {
                    return 0;
                } else if (p1 == null) {
                    return -1;
                } else if (p2 == null) {
                    return 1;
                } else {
                    int cmpX = Double.compare(p1.getX(), p2.getX());
                    if (cmpX < 0) {
                        return -1;
                    } else if (cmpX > 0) {
                        return 1;
                    } else {
                        int cmpY = Double.compare(p1.getY(), p2.getY());
                        if (cmpY < 0) {
                            return -1;
                        } else if (cmpY > 0) {
                            return 1;
                        } else {
                            int cmpW = Double.compare(p1.getWeight(), p2.getWeight());
                            if (cmpW < 0) {
                                return -1;
                            } else {
                                return cmpW > 0 ? 1 : 0;
                            }
                        }
                    }
                }
            }
        };
        Collections.sort(observations, cmp);
        return observations;
    }

    private double[] basicGuess(WeightedObservedPoint[] points) {
        int maxYIdx = this.findMaxY(points);
        double n = points[maxYIdx].getY();
        double m = points[maxYIdx].getX();

        double fwhmApprox;
        double s;
        try {
            s = n + (m - n) / 2.0D;
            double fwhmX1 = this.interpolateXAtY(points, maxYIdx, -1, s);
            double fwhmX2 = this.interpolateXAtY(points, maxYIdx, 1, s);
            fwhmApprox = fwhmX2 - fwhmX1;
        } catch (OutOfRangeException var15) {
            fwhmApprox = points[points.length - 1].getX() - points[0].getX();
        }

        s = fwhmApprox / (2.0D * FastMath.sqrt(2.0D * FastMath.log(2.0D)));
        return new double[]{n, m, s};
    }

    private int findMaxY(WeightedObservedPoint[] points) {
        int maxYIdx = 0;

        for(int i = 1; i < points.length; ++i) {
            if (points[i].getY() > points[maxYIdx].getY()) {
                maxYIdx = i;
            }
        }

        return maxYIdx;
    }

    private double interpolateXAtY(WeightedObservedPoint[] points, int startIdx, int idxStep, double y) throws OutOfRangeException {
        if (idxStep == 0) {
            throw new ZeroException();
        } else {
            WeightedObservedPoint[] twoPoints = this.getInterpolationPointsForY(points, startIdx, idxStep, y);
            WeightedObservedPoint p1 = twoPoints[0];
            WeightedObservedPoint p2 = twoPoints[1];
            if (p1.getY() == y) {
                return p1.getX();
            } else {
                return p2.getY() == y ? p2.getX() : p1.getX() + (y - p1.getY()) * (p2.getX() - p1.getX()) / (p2.getY() - p1.getY());
            }
        }
    }

    private WeightedObservedPoint[] getInterpolationPointsForY(WeightedObservedPoint[] points, int startIdx, int idxStep, double y) throws OutOfRangeException {
        if (idxStep == 0) {
            throw new ZeroException();
        } else {
            int i = startIdx;

            while(true) {
                if (idxStep < 0) {
                    if (i + idxStep < 0) {
                        break;
                    }
                } else if (i + idxStep >= points.length) {
                    break;
                }

                WeightedObservedPoint p1 = points[i];
                WeightedObservedPoint p2 = points[i + idxStep];
                if (this.isBetween(y, p1.getY(), p2.getY())) {
                    if (idxStep < 0) {
                        return new WeightedObservedPoint[]{p2, p1};
                    }

                    return new WeightedObservedPoint[]{p1, p2};
                }

                i += idxStep;
            }

            throw new OutOfRangeException(y, -1.0D / 0.0, 1.0D / 0.0);
        }
    }

    private boolean isBetween(double value, double boundary1, double boundary2) {
        return value >= boundary1 && value <= boundary2 || value >= boundary2 && value <= boundary1;
    }
}
