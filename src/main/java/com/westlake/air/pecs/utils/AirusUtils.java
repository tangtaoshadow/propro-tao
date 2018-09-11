package com.westlake.air.pecs.utils;

import com.westlake.air.pecs.domain.ResultDO;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-13 22:08
 */
public class AirusUtils {

    /**
     * Sort array[] as ascend order.
     */
    public static void sort(Comparable[] array) {
            int n = array.length;
            for (int i = 0; i < n; i++) {
                int min = i;
                for (int j = i + 1; j < n; j++) {
                    if (array[j].compareTo(array[min]) < 0) {
                        min = j;
                    }
                }
                exch(array, i, min);
            }
    }

    /**
     * Get ascend sort index of array[].
     */
    public static int[] argSort(Double[] array) {
        Double[] temp = array.clone();
        int n = temp.length;
        int[] result = new int[n];
        for (int i = 0; i < n; i++) {
            int min = 0;
            for (int j = 0; j < n; j++) {
                if (temp[j].compareTo(temp[min]) < 0) {
                    min = j;
                }
            }
            temp[min]= Double.MAX_VALUE;
            result[i] = min;
        }
        return result;
    }

    /**
     * Get descend sort index of array[].
     */
    public static Integer[] argSortReversed(Double[] array) {
        int n = array.length;
        Integer[] result = new Integer[n];
        for(int i=0;i<n;i++){
            result[i]=i;
        }
        for (int i = 0; i < n; i++) {
            int max = i;
            for (int j = i + 1; j < n; j++) {
                if (array[j].compareTo(array[max]) > 0) {
                    max = j;
                }
            }
            exch(array, i, max);
            exch(result,i,max);
        }
        return result;
    }

    /**
     * Get unique array of array[].
     */
    public static Integer[] sortedUnique(Integer[] array){
        if(array == null || array.length == 0){
            return new Integer[0];
        }
        int j =1;
        int value = array[0];
        List<Integer> index = new ArrayList<Integer>();
        index.add(0);
        for(int i=0;i<array.length;i++){
            if(array[i] != value){
                j++;
                value = array[i];
                index.add(i);
            }
        }
        Integer[] result = new Integer[j];
        for(int i =0;i<j;i++){
            result[i] = array[index.get(i)];
        }
        return result;
    }

    /**
     * Get unique array index of array[].
     */
    public static Integer[] sortedUniqueIndex (Integer[] array){
        int j =1;
        int value = array[0];
        List<Integer> index = new ArrayList<Integer>();
        index.add(0);
        for(int i=0;i<array.length;i++){
            if(array[i] != value){
                j++;
                value = array[i];
                index.add(i);
            }
        }
        Integer[] result = new Integer[j];
        for(int i =0;i<j;i++){
            result[i] = index.get(i);
        }
        return result;
    }

    /**
     * rankDataMax([1,2,2,3,3,3,4])
     * -> [1, 3, 3, 6, 6, 6, 7]
     */
    public static double[] rankDataMax(Double[] array){
        int n = array.length;
        double[] result = new double[n];
        int[] countSort = ArrayUtils.reverse(countSort(array));
        int count = 0;
        int index = array.length;
        for(int j:countSort){
            for(int i=0;i<j;i++){
                result[count +i] = index- count;
            }
            count = count+j;
        }
        return result;
    }

    /**
     * rankDataMax([1,2,2,3,3,3,4])
     * -> [7, 5.5, 5.5, 3, 3, 3, 1]
     */
    public static double[] rankDataReversed(Double[] array){
        int n = array.length;
        double[] result = new double[n];
        int[] countSortReversed = ArrayUtils.reverse(countSort(array));
        int count = 0;
        int index = array.length;
        for(int j:countSortReversed){
            for(int i=0;i<j;i++){
                result[index-j+i] = count + (j+1)/(double)2;
            }
            index = index -j;
            count = count+j;
        }
        return result;
    }

    public static double mean(Double[] array) {
        int n = array.length;
        double sum = 0;
        for (double i : array) {
            sum += i;
        }
        return sum / (double) n;
    }

    public static double std(Double[] array) {
        double mean = mean(array);
        int length = array.length;
        double error = 0;
        for (double i : array) {
            error += Math.pow(i - mean, 2);
        }
        error /= (double) length-1;
        return Math.sqrt(error);
    }

    /**
     * Normalize a with a's mean and std.
     */
    public static Double[] normalize(Double[] array){
        double mean = mean(array);
        Double[] result = array.clone();
        for(int i=0;i<array.length;i++){
            result[i] =(result[i] - mean) / std(array);
        }
        return result;
    }

    /**
     * Normalize a with b's mean and std.
     */
    public static Double[] normalize(Double[] arrayA, Double[] arrayB){
        double mean = mean(arrayB);
        double std = std(arrayB);
        Double[] result = arrayA.clone();
        for(int i=0;i<arrayA.length;i++){
            result[i] =(result[i] - mean) / std;
        }
        return result;
    }

    /**
     * Error function erf().
     */
    public static double erf(double t) {
        double result = 0.0;
        for (int i = 1; i < 101; i++) {
            result += t * 2.0 * Math.exp(-Math.pow(i * t / 100, 2)) / Math.sqrt(Math.PI) / 100.0;
        }
        return result;
    }

    /**
     * Count number of value in array[] <= present value.
     */
    public static int[] countNumPositives(Double[] array) {
        int i0 = 0, i1 = 0;
        int n = array.length;
        int[] result = new int[n];
        while (i0 < n) {
            while (i1 < n && array[i0].equals(array[i1])) {
                result[i1] = n - i0;
                i1++;
            }
            i0++;
        }
        return result;
    }

    /**
     * Get an array of Max in the rest.
     */
    public static double[] cumMax(double[] array) {
        double max = array[0];
        int length = array.length;
        double[] result = new double[length];
        for (int i = 0; i < length; i++) {
            if (array[i] > max) {
                max = array[i];
            }
            result[i] = max;
        }
        return result;
    }

    /**
     * Find index of the min value.
     */
    public static int argmin(double[] array) {
        double min = array[0];
        int minIndex = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i] < min) {
                min = array[i];
                minIndex = i;
            }
        }
        return minIndex;
    }

    /**
     * Count number of values bigger than threshold in array.
     */
    public static int countOverThreshold(Double[] array, double threshold) {
        int n = 0;
        for (double i : array) {
            if (i >= threshold) n++;
        }
        return n;
    }



    /**
     * Return index of nearest elements of samplePoints[] in array[].
     */
    public static Integer[] findNearestMatches(Double[] array, Double[] samplePoints, int useSortOrder) {

        int numBasis = array.length;
        int numSamples = samplePoints.length;
        Integer[] results = new Integer[numSamples];
        int i, bestJ;
        int low, mid, high;
        double spI, bestDist, dist;
        int sortOrder;

        if (useSortOrder != 1) {
            for (i = 0; i < numSamples; i++) {
                spI = samplePoints[i];
                bestJ = 0;
                bestDist = Math.abs(array[0] - spI);
                for (int j = 1; j < numBasis; j++) {
                    dist = Math.abs(array[j] - spI);
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestJ = j;
                    }
                }
                results[i] = bestJ;

            }
            return results;
        }
        sortOrder = AirusUtils.findSortOrder(array);
        for (i = 0; i < numSamples; i++) {
            spI = samplePoints[i];
            if (sortOrder == 0) {
                bestJ = 0;
                bestDist = Math.abs(array[0] - spI);
                for (int j = 1; j < numBasis; j++) {
                    dist = Math.abs(array[j] - spI);
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestJ = j;
                    }
                }
            } else if (sortOrder == 1) {
                low = 0;
                high = numBasis - 1;
                bestJ = -1;
                if (array[low] == spI) {
                    bestJ = low;
                } else if (array[high] == spI) {
                    bestJ = high;
                } else {
                    while (low < high - 1) {
                        mid = (low + high) / 2;
                        if (array[mid] == spI) {
                            bestJ = mid;
                        }
                        if (array[mid] < spI) {
                            low = mid;
                        } else {
                            high = mid;
                        }
                    }
                    if (bestJ == -1) {
                        if (Math.abs(array[low] - spI) < Math.abs(array[high] - spI)) {
                            bestJ = low;
                        } else {
                            bestJ = high;
                        }
                    }
                }
                while (bestJ > 0) {
                    if (array[bestJ - 1].equals(array[bestJ])) {
                        bestJ = bestJ - 1;
                    } else {
                        break;
                    }
                }
            } else {
                low = 0;
                high = numBasis - 1;
                bestJ = -1;
                if (array[low] == spI) {
                    bestJ = low;
                } else if (array[high] == spI) {
                    bestJ = high;
                } else {
                    while (low < high - 1) {
                        mid = (low + high) / 2;
                        if (array[mid] == spI) {
                            bestJ = mid;
                            break;
                        }
                        if (array[mid] > spI) {
                            low = mid;
                        } else {
                            high = mid;
                        }
                    }
                    if (bestJ == -1) {
                        if (Math.abs(array[low] - spI) < Math.abs(array[high] - spI)) {
                            bestJ = low;
                        } else {
                            bestJ = high;
                        }
                    }
                }
                while (bestJ > 0) {
                    if (array[bestJ - 1].equals(array[bestJ])) {
                        bestJ = bestJ - 1;
                    } else {
                        break;
                    }
                }
            }
            results[i] = bestJ;

        }
        return results;
    }

    /**
     * Get numCutOffs points equally picked from [a,b).
     */
    public static Double[] linspace(Double a, Double b, int numCutOffs){
        Double[] result = new Double[numCutOffs];
        double inc = Math.abs(b-a)/(numCutOffs-1);
        for(int i=0;i<numCutOffs;i++){
            result[i] = a + inc * i;
        }
        return result;
    }

    /**
     * Get the row-mean of rows in array[].
     */
    public static Double[] getRowMean(Double[][] array){
        int arrayLength = array.length;
        int arrayWidth = array[0].length;
        Double[] rowMean = new Double[arrayWidth];
        double sumRowElement =0;
        for(int i=0;i<arrayWidth;i++){
            for(int j=0;j<arrayLength;j++){
                sumRowElement += array[j][i];
            }
            rowMean[i] = sumRowElement / arrayLength;
            sumRowElement =0;
        }
        return rowMean;
    }

    public static ResultDO<Double[]> lagrangeInterpolation(Double[] x, Double[] y){
        int n = x.length;
        ResultDO<Double[]> resultDO = new ResultDO<Double[]>();
        Double[] results = new Double[n];
        if(n == y.length){
            Double result;
            for(int i=0;i<n;i++){
                result = (double)0;
                for(int j=0;j<n-2;j++){
                    result+= (x[i]-x[j+1])*(x[i]-x[j+2])/((x[j]-x[j+1])*(x[j]-x[j+2]));
                }
                result+=(x[i]-x[n-3])*(x[i]-x[n-1])/((x[n-2]-x[n-3])*(x[n-2]-x[n-1]));
                result+=(x[i]-x[n-3])*(x[i]-x[n-2])/((x[n-1]-x[n-3])*(x[n-1]-x[n-2]));
                results[i]=result;
            }
            resultDO.setSuccess(true);
            resultDO.setModel(results);
            return resultDO;
        }else{
            resultDO.setMsgInfo("Interpolation Error.\n");
            return resultDO;
        }
    }

    /**
     * Exchange position of element i,j in array[].
     */
    private static void exch(Comparable[] array, int i, int j) {
        Comparable t = array[i];
        array[i] = array[j];
        array[j] = t;
    }

    /**
     * Count number of different values in a **sorted** array.
     */
    private static int numOfUnique(Double[] array){
        int j =1;
        double value = array[0];
        for(double i : array){
            if(i != value){
                j++;
                value = i;
            }
        }
        return j;
    }

    /**
     * count number of times corresponding to unique sorted array.
     */
    private static int[] countSort(Double[] array){
        Double[] aSort = array.clone();
        sort(aSort);
        int j=0,k =0;
        int[] result = new int[numOfUnique(aSort)];
        double value =aSort[0];
        for(int i=0;i<aSort.length;i++){
            if(aSort[i]==value){
                j++;
            }else {
                result[k] = j;
                k++;
                j=1;
                value = aSort[i];
            }

        }
        result[k] = j;
        return result;
    }

    /**
     * Find order of array.
     *  0: unsorted
     *  1: ascending
     * -1: descending
     */
    private static int findSortOrder(Double[] array) {
        int i = 0;
        int n = array.length;
        if (n <= 1) {
            return 0;
        }
        while (i < n - 1 && array[i] == array[i + 1]) {
            i++;
        }
        if (i == n - 1) {
            return 1;
        }
        if (array[i] < array[i + 1]) {
            for (; i < n - 1; i++) {
                if (array[i] > array[i + 1]) {
                    return 0;
                }
            }
            return 1;
        } else {
            for (; i < n - 1; i++) {
                if (array[i] < array[i + 1]) {
                    return 0;
                }
            }
            return -1;
        }
    }

}
