package com.westlake.air.pecs.utils;

import com.alibaba.fastjson.JSON;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.airus.ScoreData;
import com.westlake.air.pecs.domain.bean.airus.TrainAndTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
                if (i) sum++;
            }
            Double[][] extractedRow = new Double[sum][array[0].length];
            int j = 0;
            for (int i = 0; i < array.length; i++) {
                if (isDecoy[i]) {
                    for (int k = 0; k < array[0].length; k++)
                        extractedRow[j][k] = array[i][k];
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

    private static Integer[] concat2d(Integer[] arrayA, Integer[] arrayB) {
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
     * @param indexSet 去重集合
     * @return
     */
    private static TrainAndTest extract3dRow(Double[][] array, Integer[] groupNumId, Boolean[] isDecoy, HashSet<Integer> indexSet) {
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
                if(indexSet.contains(groupNumId[i])){
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

    /**
     * 将GroupId简化为Integer数组
     * getGroupId({"100_run0","100_run0","DECOY_100_run0"})
     * -> {0, 0, 1}
     */
    public static Integer[] getGroupNumId(String[] groupId) {
        if (groupId[0] != null) {
            Integer[] b = new Integer[groupId.length];
            String s = groupId[0];
            int groupNumId = 0;
            for (int i = 0; i < groupId.length; i++) {
                if (!s.equals(groupId[i])) {
                    s = groupId[i];
                    groupNumId++;
                }
                b[i] = groupNumId;
            }
            return b;
        } else {
            logger.error("GetgroupNumId Error.\n");
            return null;
        }
    }

    public static ResultDO<Boolean[]> findTopIndex(Double[] array, Integer[] groupNumId) {
        ResultDO<Boolean[]> resultDO = new ResultDO<Boolean[]>();
        if (groupNumId.length == array.length) {
            int id = groupNumId[0];
            Boolean[] index = new Boolean[groupNumId.length];
            int tempIndex = 0;
            double b = array[0];
            for (int i = 0; i < groupNumId.length; i++) {

                if (groupNumId[i] != null && groupNumId[i] == id) {
                    if (array[i] > b) {
                        b = array[i];
                        tempIndex = i;
                        //index[i]=1;
                    }

                } else if (array[i] != null && groupNumId[i] != null) {
                    index[tempIndex] = true;
                    b = array[i];
                    id = groupNumId[i];
                    tempIndex = i;
                }
            }
            index[tempIndex] = true;
            for (int i = 0; i < groupNumId.length; i++) {
                if (index[i] == null) {
                    index[i] = false;
                }
            }
            resultDO.setModel(index);
            resultDO.setSuccess(true);
            return resultDO;
        } else {
            resultDO.setMsgInfo("FindTopIndex Error.\n");
            return resultDO;
        }
    }

    public static ResultDO<Double[][]> getDecoyPeaks(Double[][] array, Boolean[] isDecoy) {
        ResultDO<Double[][]> decoyPeaks = new ResultDO<Double[][]>();
        if (array.length == isDecoy.length) {
            return ArrayUtils.extract3dRow(array, isDecoy);
        } else {
            decoyPeaks.setMsgInfo("GetDecoyPeaks Error.\n");
            return decoyPeaks;
        }
    }

    public static ResultDO<Double[]> getDecoyPeaks(Double[] array, Boolean[] isDecoy) {
        ResultDO<Double[]> decoyPeaks = new ResultDO<Double[]>();
        if (array.length == isDecoy.length) {
            return ArrayUtils.extract3dRow(array, isDecoy);
        } else {
            decoyPeaks.setMsgInfo("GetDecoyPeaks Error.\n");
            return decoyPeaks;
        }
    }

    public static ResultDO<Integer[]> getDecoyPeaks(Integer[] array, Boolean[] isDecoy) {
        ResultDO<Integer[]> decoyPeaks = new ResultDO<Integer[]>();
        if (array.length == isDecoy.length) {
            return ArrayUtils.extract3dRow(array, isDecoy);
        } else {
            decoyPeaks.setMsgInfo("GetDecoyPeaks Error.\n");
            return decoyPeaks;
        }
    }

    public static ResultDO<Double[]> getTargetPeaks(Double[] array, Boolean[] isDecoy) {
        ResultDO<Double[]> targetPeaks = new ResultDO<Double[]>();
        if (array.length == isDecoy.length) {
            Boolean[] isTarget = getIsTarget(isDecoy);
            return ArrayUtils.extract3dRow(array, isTarget);
        } else {
            targetPeaks.setMsgInfo("GetDecoyPeaks Error.\n");
            return targetPeaks;
        }
    }

    public static ResultDO<Integer[]> getTargetPeaks(Integer[] array, Boolean[] isDecoy) {
        ResultDO<Integer[]> targetPeaks = new ResultDO<Integer[]>();
        if (array.length == isDecoy.length) {
            Boolean[] isTarget = getIsTarget(isDecoy);
            return ArrayUtils.extract3dRow(array, isTarget);
        } else {
            targetPeaks.setMsgInfo("GetDecoyPeaks Error.\n");
            return targetPeaks;
        }
    }

    public static ResultDO<Double[][]> getTopTargetPeaks(Double[][] array, Boolean[] isDecoy, Boolean[] index) {
        ResultDO<Double[][]> resultDO = new ResultDO<Double[][]>();
        Boolean[] isTopTarget = getIsTopTarget(isDecoy, index).getModel();
        if (isTopTarget != null && array.length == isTopTarget.length) {
            resultDO = ArrayUtils.getDecoyPeaks(array, isTopTarget);
            return resultDO;
        } else {
            resultDO.setMsgInfo("GetTopTargetPeaks Error.\n");
            return resultDO;
        }
    }

    public static ResultDO<Double[]> getTopTargetPeaks(Double[] array, Boolean[] isDecoy, Boolean[] index) {
        ResultDO<Double[]> resultDO = new ResultDO<Double[]>();
        Boolean[] isTopTarget = getIsTopTarget(isDecoy, index).getModel();
        if (isTopTarget != null && array.length == isTopTarget.length) {
            resultDO = ArrayUtils.getDecoyPeaks(array, isTopTarget);
            return resultDO;
        } else {
            resultDO.setMsgInfo("GetTopTargetPeaks Error.\n");
            return resultDO;
        }
    }

    public static ResultDO<Double[][]> getTopDecoyPeaks(Double[][] array, Boolean[] isDecoy, Boolean[] index) {
        ResultDO<Double[][]> resultDO = new ResultDO<Double[][]>();
        Boolean[] isTopDecoy = getIsTopDecoy(isDecoy, index).getModel();
        if (isTopDecoy != null && array.length == isTopDecoy.length) {
            resultDO = ArrayUtils.getDecoyPeaks(array, isTopDecoy);
            return resultDO;
        } else {
            resultDO.setMsgInfo("GetTopDecoyPeaks Error.\n");
            return resultDO;
        }
    }

    public static ResultDO<Double[]> getTopDecoyPeaks(Double[] array, Boolean[] isDecoy, Boolean[] index) {
        ResultDO<Double[]> resultDO = new ResultDO<Double[]>();
        Boolean[] isTopDecoy = getIsTopDecoy(isDecoy, index).getModel();
        if (isTopDecoy != null && array.length == isTopDecoy.length) {
            resultDO = ArrayUtils.getDecoyPeaks(array, isTopDecoy);
            return resultDO;
        } else {
            resultDO.setMsgInfo("GetTopDecoyPeaks Error.\n");
            return resultDO;
        }
    }

    /**
     * Get feature Matrix of useMainScore or not.
     */
    public static ResultDO<Double[][]> getFeatureMatrix(Double[][] array, Boolean useMainScore) {
        ResultDO<Double[][]> resultDO = new ResultDO<Double[][]>();
        if (array != null) {
            if (useMainScore) {
                resultDO = ArrayUtils.extract3dColumn(array, 0);
            } else {
                resultDO = ArrayUtils.extract3dColumn(array, 1);
            }
            return resultDO;
        } else {
            resultDO.setMsgInfo("GetFeatureMatrix Error.\n");
            return resultDO;
        }
    }

    public static Double[][] peaksFilter(Double[][] ttPeaks, Double[] ttScores, double cutOff) {
        int count = 0;
        for (double i : ttScores) {
            if (i >= cutOff) count++;
        }
        Double[][] targetPeaks = new Double[count][ttPeaks[0].length];
        int j = 0;
        for (int i = 0; i < ttScores.length; i++) {
            if (ttScores[i] >= cutOff) {
                targetPeaks[j] = ttPeaks[i];
                j++;
            }
        }
        return targetPeaks;
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

    private static Integer[] getPartOfArray(Integer[] array, int cutoff) {
        Integer[] result = new Integer[cutoff];
        System.arraycopy(array, 0, result, 0, cutoff);
        return result;
    }


    /**
     * 划分测试集与训练集,保证每一次对于同一份原始数据划分出的测试集都是同一份
     *
     * @param data
     * @param groupNumId
     * @param isDecoy
     * @param fraction   目前写死0.5
     * @param isTest
     * @return
     */
    public static TrainAndTest splitForXval(Double[][] data, Integer[] groupNumId, Boolean[] isDecoy, double fraction, boolean isTest) {
        Integer[] decoyIds = getDecoyPeaks(groupNumId, isDecoy).getModel();
        Integer[] targetIds = getTargetPeaks(groupNumId, isDecoy).getModel();

        if (!isTest) {
            List<Integer> decoyIdShuffle = Arrays.asList(decoyIds);
            List<Integer> targetIdShuffle = Arrays.asList(targetIds);
            Collections.shuffle(decoyIdShuffle);
            Collections.shuffle(targetIdShuffle);
            decoyIdShuffle.toArray(decoyIds);
            targetIdShuffle.toArray(targetIds);
        } else {
            TreeSet<Integer> decoyIdSet = new TreeSet<Integer>(Arrays.asList(decoyIds));
            TreeSet<Integer> targetIdSet = new TreeSet<Integer>(Arrays.asList(targetIds));

            decoyIds = new Integer[decoyIdSet.size()];
            decoyIdSet.toArray(decoyIds);
            targetIds = new Integer[targetIdSet.size()];
            targetIdSet.toArray(targetIds);
        }

        int decoyLength = (int) (decoyIds.length * fraction) + 1;
        int targetLength = (int) (targetIds.length * fraction) + 1;
        Integer[] learnIds = ArrayUtils.concat2d(getPartOfArray(decoyIds, decoyLength), getPartOfArray(targetIds, targetLength));

        HashSet<Integer> learnIdSet = new HashSet<Integer>(Arrays.asList(learnIds));
        return extract3dRow(data, groupNumId, isDecoy, learnIdSet);
    }

    /**
     * "1_run0">"19_run0"
     * "10_run0">"109_run0"
     * same sort as pyprophet
     */
    public static ScoreData fakeSortTgId(ScoreData scoreData) {
        String[] groupId = scoreData.getGroupId();
        Integer[] groupNumId = scoreData.getGroupNumId();
//        AirusUtils.sort(groupNumId);
//        Integer[] test1 = AirusUtils.sortedUnique(groupNumId);
        int groupIdLength = groupId.length;
        Double[] tgIdNum = new Double[groupIdLength];
        for (int i = 0; i < groupIdLength; i++) {
            String[] groupIdSplit = groupId[i].split("_");
            if (groupIdSplit[0].equals("DECOY")) {
                tgIdNum[i] = Double.parseDouble(groupIdSplit[1]);
//                groupId[i] = groupIdSplit[1] + "_" + groupIdSplit[2];
            } else {
                tgIdNum[i] = Double.parseDouble(groupIdSplit[0]);
//                groupId[i] = groupIdSplit[0] + "_" + groupIdSplit[1];
            }
            if (tgIdNum[i] < 10) {
                tgIdNum[i] = tgIdNum[i] * 100 + 99.5;
            } else if (tgIdNum[i] < 100) {
                tgIdNum[i] = tgIdNum[i] * 10 + 9.5;
            }

        }
        int[] index = AirusUtils.argSort(tgIdNum);
//        Integer[] indexTest = AirusUtils.argSort(tgIdNum);

//        AirusUtils.sort(indexTest);
//        Integer[] testNum = AirusUtils.sortedUnique(indexTest);
        Boolean[] isDecoy = scoreData.getIsDecoy();
        Double[][] scores = scoreData.getScoreData();
        String[] newGroupId = new String[groupIdLength];
        Boolean[] newIsDecoy = new Boolean[groupIdLength];
        Double[][] newScores = new Double[groupIdLength][scores[0].length];
//        int emmm=0;
//        for(int i=0;i<groupIdLength;i++){
//            if(indexTest[i] == 0){
//                emmm++;
//            }
//        }

        for (int i = 0; i < groupIdLength; i++) {
            int j = index[i];
            newGroupId[i] = groupId[j];
            newIsDecoy[i] = isDecoy[j];
            newScores[i] = scores[j];
//            newGroupNumId[i] = groupNumId[j];
        }
        Integer[] newGroupNumId = ArrayUtils.getGroupNumId(newGroupId);
//        Integer[] testGD = ArrayUtils.getGroupNumId(newGroupNumId).getFeedBack();
//        int hehe = testGD[9164];
//        AirusUtils.sort(groupNumId);
//        Integer[] test1 = AirusUtils.sortedUnique(groupNumId);
//        AirusUtils.sort(newGroupNumId);
//        Integer[] test = AirusUtils.sortedUnique(newGroupNumId);
        scoreData.setGroupId(newGroupId);
        scoreData.setIsDecoy(newIsDecoy);
        scoreData.setScoreData(newScores);
        scoreData.setGroupNumId(newGroupNumId);

        return scoreData;
    }

    private static Boolean[] getIsTarget(Boolean[] isDecoy) {
        Boolean[] isTarget = new Boolean[isDecoy.length];
        for (int i = 0; i < isDecoy.length; i++) {
            isTarget[i] = !isDecoy[i];
        }
        return isTarget;
    }

    private static ResultDO<Boolean[]> getIsTopDecoy(Boolean[] isDecoy, Boolean[] index) {
        ResultDO<Boolean[]> resultDO = new ResultDO<Boolean[]>();
        if (isDecoy.length == index.length) {
            Boolean[] isTopDecoy = new Boolean[isDecoy.length];
            for (int i = 0; i < isDecoy.length; i++) {
                isTopDecoy[i] = isDecoy[i] && index[i];
            }
            resultDO.setModel(isTopDecoy);
            resultDO.setSuccess(true);
            return resultDO;
        } else {
            resultDO.setMsgInfo("GetIsTopDecoy Error.\n");
            return resultDO;
        }
    }

    private static ResultDO<Boolean[]> getIsTopTarget(Boolean[] isDecoy, Boolean[] index) {
        ResultDO<Boolean[]> resultDO = new ResultDO<Boolean[]>();
        if (isDecoy.length == index.length) {
            Boolean[] isTopTarget = new Boolean[isDecoy.length];
            for (int i = 0; i < isDecoy.length; i++) {
                isTopTarget[i] = !isDecoy[i] && index[i];
            }
            resultDO.setModel(isTopTarget);
            resultDO.setSuccess(true);
            return resultDO;
        } else {
            resultDO.setMsgInfo("GetIsTopTarget Error.\n");
            return resultDO;
        }
    }

}
