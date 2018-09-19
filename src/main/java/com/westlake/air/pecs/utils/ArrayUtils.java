package com.westlake.air.pecs.utils;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.airus.IndexValue;
import com.westlake.air.pecs.domain.bean.airus.ScoreData;
import com.westlake.air.pecs.domain.bean.airus.TrainAndTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-12 21:44
 */

public class ArrayUtils {

    public static final Logger logger = LoggerFactory.getLogger(ArrayUtils.class);

    /**
     * Return reverse of given array[].
     */
    public static double[] reverse(double[] array) {

        int length = array.length;
        double[] result = new double[length];
        for (int i = 0; i < length; i++) {
            result[i] = array[length - 1 - i];
        }
        return result;

    }

    public static int[] reverse(int[] array) {
        int length = array.length;
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = array[length - 1 - i];
        }
        return result;

    }

    /**
     * Concatenate arrayA[] and arrayB[].
     */
    public static Double[] concat2d(Double[] arrayA, Double[] arrayB) {
        Double[] arrayC = new Double[arrayA.length + arrayB.length];
        System.arraycopy(arrayA, 0, arrayC, 0, arrayA.length);
        System.arraycopy(arrayB, 0, arrayC, arrayA.length, arrayB.length);
        return arrayC;
    }

    /**
     * Concatenate arrayA[] and arrayB[][] y dimension.
     */
    public static ResultDO<Double[][]> concat3d(Double[][] arrayA, Double[][] arrayB) {
        ResultDO<Double[][]> resultDO = new ResultDO<Double[][]>();
        if (arrayA[0].length == arrayB[0].length) {
            Double[][] c = new Double[arrayA.length + arrayB.length][arrayA[0].length];
            for (int i = 0; i < arrayA.length; i++) {
                System.arraycopy(arrayA[i], 0, c[i], 0, arrayA[0].length);
            }
            for (int i = 0; i < arrayB.length; i++) {
                System.arraycopy(arrayB[i], 0, c[i + arrayA.length], 0, arrayB[0].length);
            }
            resultDO.setModel(c);
            resultDO.setSuccess(true);
            return resultDO;
        } else {
            resultDO.setMsgInfo("Concat3d Error.\n");
            return resultDO;
        }
    }

    public static ResultDO<double[][]> concat3d(double[][] arrayA, double[][] arrayB) {
        ResultDO<double[][]> resultDO = new ResultDO<double[][]>();
        if (arrayA[0].length == arrayB[0].length) {
            double[][] c = new double[arrayA.length + arrayB.length][arrayA[0].length];
            for (int i = 0; i < arrayA.length; i++) {
                System.arraycopy(arrayA[i], 0, c[i], 0, arrayA[0].length);
            }
            for (int i = 0; i < arrayB.length; i++) {
                System.arraycopy(arrayB[i], 0, c[i + arrayA.length], 0, arrayB[0].length);
            }
            resultDO.setModel(c);
            resultDO.setSuccess(true);
            return resultDO;
        } else {
            resultDO.setMsgInfo("Concat3d Error.\n");
            return resultDO;
        }
    }

    /**
     * Extract array from array.
     */
    public static ResultDO<Double[]> extract2d(Double[] array, Integer begin, Integer end) {
        ResultDO<Double[]> resultDO = new ResultDO<Double[]>();
        if (begin <= end && end < array.length) {
            Double[] b = new Double[end - begin + 1];
            System.arraycopy(array, begin, b, 0, end - begin + 1);
            resultDO.setModel(b);
            resultDO.setSuccess(true);
            return resultDO;
        } else {
            resultDO.setMsgInfo("Extract2d Error.\n");
            return resultDO;
        }
    }

    public static ResultDO<Double[]> extractRow(Double[] array, Integer[] row) {
        ResultDO<Double[]> resultDO = new ResultDO<Double[]>();
        Double[] result = new Double[row.length];
        for (int i = 0; i < row.length; i++) {
            if (row[i] > -1 && row[i] < array.length) {
                result[i] = array[row[i]];
            } else {
                resultDO.setMsgInfo("ExtractRow Error.\n");
                return resultDO;
            }
        }
        resultDO.setModel(result);
        resultDO.setSuccess(true);
        return resultDO;
    }

    public static ResultDO<double[]> extractRow(double[] array, Integer[] row) {
        ResultDO<double[]> resultDO = new ResultDO<double[]>();
        double[] result = new double[row.length];
        for (int i = 0; i < row.length; i++) {
            if (row[i] > -1 && row[i] < array.length) {
                result[i] = array[row[i]];
            } else {
                resultDO.setMsgInfo("ExtractRow Error.\n");
                return resultDO;
            }
        }
        resultDO.setModel(result);
        resultDO.setSuccess(true);
        return resultDO;
    }

    public static ResultDO<String[]> extractRow(String[] array, Integer[] row) {
        ResultDO<String[]> resultDO = new ResultDO<String[]>();
        String[] result = new String[row.length];
        for (int i = 0; i < row.length; i++) {
            if (row[i] > -1 && row[i] < array.length) {
                result[i] = array[row[i]];
            } else {
                resultDO.setMsgInfo("ExtractRow Error.\n");
                return resultDO;
            }
        }
        resultDO.setModel(result);
        resultDO.setSuccess(true);
        return resultDO;
    }

    public static ResultDO<Double[][]> extract3dColumn(Double[][] array, Integer begin, Integer end) {
        ResultDO<Double[][]> resultDO = new ResultDO<Double[][]>();
        if (begin <= end && end < array[0].length) {
            Double[][] b = new Double[array.length][end - begin + 1];
            for (int i = 0; i < array.length; i++) {
                System.arraycopy(array[i], begin, b[i], 0, end - begin + 1);
            }
            resultDO.setModel(b);
            resultDO.setSuccess(true);
            return resultDO;
        } else {
            resultDO.setMsgInfo("Extract3dColumn Error.\n");
            return resultDO;
        }
    }

    public static ResultDO<Double[][]> extract3dColumn(Double[][] array, Integer begin) {
        return extract3dColumn(array, begin, array[0].length - 1);
    }

    public static ResultDO<Double[][]> extract3dRow(Double[][] array, Boolean[] isDecoy) {
        ResultDO<Double[][]> resultDO = new ResultDO<Double[][]>();
        if (array.length == isDecoy.length) {
            int sum = 0;
            for (Boolean i : isDecoy) {
                if (i) {
                    sum++;
                }
            }
            Double[][] extractedRow = new Double[sum][array[0].length];
            int j = 0;
            for (int i = 0; i < array.length; i++) {
                if (isDecoy[i]) {
                    for (int k = 0; k < array[0].length; k++) {
                        extractedRow[j][k] = array[i][k];
                    }
                    j++;
                }
            }
            resultDO.setModel(extractedRow);
            resultDO.setSuccess(true);
            return resultDO;
        } else {
            resultDO.setMsgInfo("Extract3dRow Error.\n");
            return resultDO;
        }

    }

    public static Integer[] concat2d(Integer[] arrayA, Integer[] arrayB) {
        Integer[] arrayC = new Integer[arrayA.length + arrayB.length];
        System.arraycopy(arrayA, 0, arrayC, 0, arrayA.length);
        System.arraycopy(arrayB, 0, arrayC, arrayA.length, arrayB.length);
        return arrayC;
    }

    public static ResultDO<Double[]> extractColumn(Double[][] array, int column) {
        ResultDO<Double[]> resultDO = new ResultDO<Double[]>();
        Double[] result = new Double[array.length];
        if (column > -1 && column < array[0].length) {
            for (int i = 0; i < array.length; i++) {
                result[i] = array[i][column];
            }
            resultDO.setModel(result);
            resultDO.setSuccess(true);
            return resultDO;
        } else {
            resultDO.setMsgInfo("ExtractColumn Error.\n");
            return resultDO;
        }
    }

    /**
     * @param array
     * @param groupNumId
     * @param isDecoy
     * @param indexSet   去重集合
     * @return
     */
    public static TrainAndTest extract3dRow(Double[][] array, Integer[] groupNumId, Boolean[] isDecoy, HashSet<Integer> indexSet) {
        TrainAndTest trainAndTest = new TrainAndTest();

        if (array.length == groupNumId.length) {
            int k = -1, l = -1;
            int symbol;
            Double[][] trainRow = new Double[array.length][array[0].length];
            Double[][] testRow = new Double[array.length][array[0].length];
            Integer[] trainIdRow = new Integer[array.length];
            Integer[] testIdRow = new Integer[array.length];
            Boolean[] trainIsDecoy = new Boolean[array.length];
            Boolean[] testIsDecoy = new Boolean[array.length];

            for (int i = 0; i < groupNumId.length; i++) {
                symbol = k;
                if (indexSet.contains(groupNumId[i])) {
                    k++;
                    trainIdRow[k] = groupNumId[i];
                    trainRow[k] = array[i];
                    trainIsDecoy[k] = isDecoy[i];
                }
                if (k == symbol) {
                    l++;
                    testIdRow[l] = groupNumId[i];
                    testRow[l] = array[i];
                    testIsDecoy[l] = isDecoy[i];
                }
            }
            Double[][] extractedTrainRow = new Double[k + 1][array[0].length];
            Double[][] extractedTestRow = new Double[l + 1][array[0].length];
            Integer[] extractedTrainIdRow = new Integer[k + 1];
            Integer[] extractedTestIdRow = new Integer[l + 1];
            Boolean[] extractedTrainIsDecoyRow = new Boolean[k + 1];
            Boolean[] extractedTestIsDecoyRow = new Boolean[l + 1];
            for (int i = 0; i <= k; i++) {
                extractedTrainRow[i] = trainRow[i];
                extractedTrainIdRow[i] = trainIdRow[i];
                extractedTrainIsDecoyRow[i] = trainIsDecoy[i];

            }
            for (int i = 0; i <= l; i++) {
                extractedTestRow[i] = testRow[i];
                extractedTestIdRow[i] = testIdRow[i];
                extractedTestIsDecoyRow[i] = testIsDecoy[i];
            }
            trainAndTest.setTrainData(extractedTrainRow);
            trainAndTest.setTrainId(extractedTrainIdRow);
            trainAndTest.setTrainIsDecoy(extractedTrainIsDecoyRow);
            trainAndTest.setTestData(extractedTestRow);
            trainAndTest.setTestId(extractedTestIdRow);
            trainAndTest.setTestIsDecoy(extractedTestIsDecoyRow);

            return trainAndTest;
        } else {
            logger.error("Extract3dRow Error: array.length must be equal with id.length");
            return null;
        }

    }

    public static ResultDO<Double[]> extract3dRow(Double[] array, Boolean[] isDecoy) {
        ResultDO<Double[]> resultDO = new ResultDO<Double[]>();
        if (array.length == isDecoy.length) {
            int sum = 0;
            for (boolean i : isDecoy) {
                if (i) {
                    sum++;
                }
            }
            Double[] extractedRow = new Double[sum];
            int j = 0;
            for (int i = 0; i < array.length; i++) {
                if (isDecoy[i]) {
                    extractedRow[j] = array[i];
                    j++;
                }
            }
            resultDO.setModel(extractedRow);
            resultDO.setSuccess(true);
            return resultDO;
        } else {
            resultDO.setMsgInfo("Extract3dRow Error.\n");
            return resultDO;
        }

    }

    public static ResultDO<Integer[]> extract3dRow(Integer[] array, Boolean[] isDecoy) {
        ResultDO<Integer[]> resultDO = new ResultDO<Integer[]>();
        if (array.length == isDecoy.length) {
            int sum = 0;
            for (boolean i : isDecoy) {
                if (i) {
                    sum++;
                }
            }
            Integer[] extractedRow = new Integer[sum];
            int j = 0;
            for (int i = 0; i < array.length; i++) {
                if (isDecoy[i]) {
                    extractedRow[j] = array[i];
                    j++;
                }
            }
            resultDO.setModel(extractedRow);
            resultDO.setSuccess(true);
            return resultDO;
        } else {
            resultDO.setMsgInfo("Extract3dRow Error.\n");
            return resultDO;
        }

    }



    public static ResultDO<Double[]> dot(Double[][] array, Double[] w) {
        ResultDO<Double[]> resultDO = new ResultDO<Double[]>();
        int aLength = array.length;
        int wLength = w.length;
        if (array[0].length == wLength) {
            Double[] result = new Double[aLength];
            for (int i = 0; i < aLength; i++) {
                result[i] = 0.0;
                for (int j = 0; j < wLength; j++) {
                    result[i] += array[i][j] * w[j];
                }
            }
            resultDO.setModel(result);
            resultDO.setSuccess(true);
            return resultDO;
        } else {
            resultDO.setMsgInfo("Dot Error.\n");
            return resultDO;
        }
    }

    public static Integer[] getPartOfArray(Integer[] array, int cutoff) {
        Integer[] result = new Integer[cutoff];
        System.arraycopy(array, 0, result, 0, cutoff);
        return result;
    }

    /**
     * Get ascend sort index of array[].
     */
    public static Integer[] indexBeforeSort(Double[] array) {

        List<IndexValue> indexValues = IndexValue.buildList(array);
        Collections.sort(indexValues);

        int n = array.length;
        Integer[] result = new Integer[n];

        for (int i = 0; i < n; i++) {
            result[i] = indexValues.get(i).getIndex();
        }
        return result;
    }

    /**
     * Get descend sort index of array[].
     */
    public static Integer[] indexBeforeReversedSort(Double[] array) {
        List<IndexValue> indexValues = IndexValue.buildList(array);
        Collections.sort(indexValues);
        Collections.reverse(indexValues);
        int n = array.length;
        Integer[] result = new Integer[n];

        for (int i = 0; i < n; i++) {
            result[i] = indexValues.get(i).getIndex();
        }
        return result;
    }

    /**
     * Get unique array index of array[].
     */
    public static Integer[] sortedUniqueIndex(Integer[] array) {
        int j = 1;
        int value = array[0];
        List<Integer> index = new ArrayList<Integer>();
        index.add(0);
        for (int i = 0; i < array.length; i++) {
            if (array[i] != value) {
                j++;
                value = array[i];
                index.add(i);
            }
        }
        Integer[] result = new Integer[j];
        for (int i = 0; i < j; i++) {
            result[i] = index.get(i);
        }
        return result;
    }

    /**
     * rankDataMax([1,2,2,3,3,3,4])
     * -> [1, 3, 3, 6, 6, 6, 7]
     */
    public static double[] rankDataMax(Double[] array) {
        int n = array.length;
        double[] result = new double[n];
        int[] countSort = ArrayUtils.reverse(countSort(array));
        int count = 0;
        int index = array.length;
        for (int j : countSort) {
            for (int i = 0; i < j; i++) {
                result[count + i] = index - count;
            }
            count = count + j;
        }
        return result;
    }

    /**
     * rankDataMax([1,2,2,3,3,3,4])
     * -> [7, 5.5, 5.5, 3, 3, 3, 1]
     */
    public static double[] rankDataReversed(Double[] array) {
        int n = array.length;
        double[] result = new double[n];
        int[] countSortReversed = ArrayUtils.reverse(countSort(array));
        int count = 0;
        int index = array.length;
        for (int j : countSortReversed) {
            for (int i = 0; i < j; i++) {
                result[index - j + i] = count + (j + 1) / (double) 2;
            }
            index = index - j;
            count = count + j;
        }
        return result;
    }

    public static double mean(Double[] array) {
        int n = array.length;
        double sum = 0;
        for (Double i : array) {
            if(!i.isNaN()){
                sum += i;
            }
        }
        return sum / n;
    }

    public static double std(Double[] array) {
        double mean = mean(array);
        int length = array.length;
        double error = 0;
        for (Double i : array) {
            if(!i.isNaN()){
                error += Math.pow(i - mean, 2);
            }
        }
        error /= (double) length - 1;
        return Math.sqrt(error);
    }

    /**
     * Normalize a with a's mean and std.
     */
    public static Double[] normalize(Double[] array) {
        double mean = mean(array);
        Double[] result = array.clone();
        double std = std(array);
        for (int i = 0; i < array.length; i++) {
            result[i] = (result[i] - mean) / std;
        }
        return result;
    }

    /**
     * Normalize a with b's mean and std.
     */
    public static Double[] normalize(Double[] arrayA, Double[] arrayB) {
        double mean = mean(arrayB);
        double std = std(arrayB);
        Double[] result = arrayA.clone();
        for (int i = 0; i < arrayA.length; i++) {
            result[i] = (result[i] - mean) / std;
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
        sortOrder = findSortOrder(array);
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
    public static Double[] linspace(Double a, Double b, int numCutOffs) {
        Double[] result = new Double[numCutOffs];
        double inc = Math.abs(b - a) / (numCutOffs - 1);
        for (int i = 0; i < numCutOffs; i++) {
            result[i] = a + inc * i;
        }
        return result;
    }

    /**
     * Get the row-mean of rows in array[].
     */
    public static Double[] getRowMean(Double[][] array) {
        int arrayLength = array.length;
        int arrayWidth = array[0].length;
        Double[] rowMean = new Double[arrayWidth];
        double sumRowElement = 0;
        for (int i = 0; i < arrayWidth; i++) {
            for (int j = 0; j < arrayLength; j++) {
                sumRowElement += array[j][i];
            }
            rowMean[i] = sumRowElement / arrayLength;
            sumRowElement = 0;
        }
        return rowMean;
    }

    public static ResultDO<Double[]> lagrangeInterpolation(Double[] x, Double[] y) {
        int n = x.length;
        ResultDO<Double[]> resultDO = new ResultDO<Double[]>();
        Double[] results = new Double[n];
        if (n == y.length) {
            Double result;
            for (int i = 0; i < n; i++) {
                result = (double) 0;
                for (int j = 0; j < n - 2; j++) {
                    result += (x[i] - x[j + 1]) * (x[i] - x[j + 2]) / ((x[j] - x[j + 1]) * (x[j] - x[j + 2]));
                }
                result += (x[i] - x[n - 3]) * (x[i] - x[n - 1]) / ((x[n - 2] - x[n - 3]) * (x[n - 2] - x[n - 1]));
                result += (x[i] - x[n - 3]) * (x[i] - x[n - 2]) / ((x[n - 1] - x[n - 3]) * (x[n - 1] - x[n - 2]));
                results[i] = result;
            }
            resultDO.setSuccess(true);
            resultDO.setModel(results);
            return resultDO;
        } else {
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
    private static int numOfUnique(Double[] array) {
        int j = 1;
        double value = array[0];
        for (double i : array) {
            if (i != value) {
                j++;
                value = i;
            }
        }
        return j;
    }

    /**
     * count number of times corresponding to unique sorted array.
     */
    private static int[] countSort(Double[] array) {
        Double[] aSort = array.clone();
        Arrays.sort(aSort);
        int j = 0, k = 0;
        int[] result = new int[numOfUnique(aSort)];
        double value = aSort[0];
        for (int i = 0; i < aSort.length; i++) {
            if (aSort[i] == value) {
                j++;
            } else {
                result[k] = j;
                k++;
                j = 1;
                value = aSort[i];
            }

        }
        result[k] = j;
        return result;
    }

    /**
     * Find order of array.
     * 0: unsorted
     * 1: ascending
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

    public static void checkNull(Double[][] array) {
        for (int i = 0; i < array.length; i++) {
            for(int j = 0;j<array[i].length;j++){
                if(array[i][j].equals(Double.NaN)){
                    logger.info("发现空值:i/j:"+i+"/"+j);
                }
            }
        }
    }

}
