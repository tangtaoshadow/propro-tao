package com.westlake.air.pecs.utils;

import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.airus.FinalResult;
import com.westlake.air.pecs.domain.bean.airus.ScoreData;
import com.westlake.air.pecs.domain.bean.airus.TrainAndTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by Nico Wang Ruimin
 * Time: 2018-06-13 22:08
 */
public class AirusUtils {

    public static final Logger logger = LoggerFactory.getLogger(AirusUtils.class);

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

    public static Boolean[] findTopIndex(Double[] array, Integer[] groupNumId) {

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
            return index;
        } else {
            logger.error("FindTopIndex Error.");
            return null;
        }
    }

    public static Double[][] getDecoyPeaks(Double[][] array, Boolean[] isDecoy) {
        if (array.length == isDecoy.length) {
            return ArrayUtils.extract3dRow(array, isDecoy);
        } else {
            logger.error("GetDecoyPeaks Error");
            return null;
        }
    }

    public static Double[] getDecoyPeaks(Double[] array, Boolean[] isDecoy) {

        if (array.length == isDecoy.length) {
            return ArrayUtils.extract3dRow(array, isDecoy);
        } else {
            logger.error("GetDecoyPeaks Error");
            return null;
        }
    }

    public static Integer[] getDecoyPeaks(Integer[] array, Boolean[] isDecoy) {
        ResultDO<Integer[]> decoyPeaks = new ResultDO<Integer[]>();
        if (array.length == isDecoy.length) {
            return ArrayUtils.extract3dRow(array, isDecoy);
        } else {
            logger.error("GetDecoyPeaks Error");
            return null;
        }
    }

    public static Double[] getTargetPeaks(Double[] array, Boolean[] isDecoy) {
        if (array.length == isDecoy.length) {
            Boolean[] isTarget = getIsTarget(isDecoy);
            return ArrayUtils.extract3dRow(array, isTarget);
        } else {
            logger.error("GetDecoyPeaks Error");
            return null;
        }
    }

    public static Integer[] getTargetPeaks(Integer[] array, Boolean[] isDecoy) {
        if (array.length == isDecoy.length) {
            Boolean[] isTarget = getIsTarget(isDecoy);
            return ArrayUtils.extract3dRow(array, isTarget);
        } else {
            logger.error("GetDecoyPeaks Error");
            return null;
        }
    }

    public static Double[][] getTopTargetPeaks(Double[][] array, Boolean[] isDecoy, Boolean[] index) {
        Boolean[] isTopTarget = getIsTopTarget(isDecoy, index);
        if (isTopTarget != null && array.length == isTopTarget.length) {
            return getDecoyPeaks(array, isTopTarget);
        } else {
            logger.error("GetTopTargetPeaks Error");
            return null;
        }
    }

    public static Double[] getTopTargetPeaks(Double[] array, Boolean[] isDecoy, Boolean[] index) {
        Boolean[] isTopTarget = getIsTopTarget(isDecoy, index);
        if (isTopTarget != null && array.length == isTopTarget.length) {
            return getDecoyPeaks(array, isTopTarget);
        } else {
            logger.error("GetTopTargetPeaks Error");
            return null;
        }
    }

    public static Double[][] getTopDecoyPeaks(Double[][] array, Boolean[] isDecoy, Boolean[] index) {
        Boolean[] isTopDecoy = getIsTopDecoy(isDecoy, index);
        if (isTopDecoy != null && array.length == isTopDecoy.length) {
            return getDecoyPeaks(array, isTopDecoy);
        } else {
            logger.error("GetTopDecoyPeaks Error");
            return null;
        }
    }

    public static Double[] getTopDecoyPeaks(Double[] array, Boolean[] isDecoy, Boolean[] index) {
        Boolean[] isTopDecoy = getIsTopDecoy(isDecoy, index);
        if (isTopDecoy != null && array.length == isTopDecoy.length) {
            return getDecoyPeaks(array, isTopDecoy);
        } else {
            logger.error("GetTopDecoyPeaks Error");
            return null;
        }
    }

    /**
     * Get feature Matrix of useMainScore or not.
     */
    public static Double[][] getFeatureMatrix(Double[][] array, Boolean useMainScore) {
        if (array != null) {
            if (useMainScore) {
                return ArrayUtils.extract3dColumn(array, 0);
            } else {
                return ArrayUtils.extract3dColumn(array, 1);
            }
        } else {
            logger.error("GetFeatureMatrix Error");
            return null;
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
        Integer[] decoyIds = getDecoyPeaks(groupNumId, isDecoy);
        Integer[] targetIds = getTargetPeaks(groupNumId, isDecoy);

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
        Integer[] learnIds = ArrayUtils.concat2d(ArrayUtils.getPartOfArray(decoyIds, decoyLength), ArrayUtils.getPartOfArray(targetIds, targetLength));

        HashSet<Integer> learnIdSet = new HashSet<Integer>(Arrays.asList(learnIds));
        return ArrayUtils.extract3dRow(data, groupNumId, isDecoy, learnIdSet);
    }

    public static ScoreData fakeSortTgId(ScoreData scoreData) {
        String[] groupId = scoreData.getGroupId();
        int groupIdLength = groupId.length;
        Integer[] index = ArrayUtils.indexBeforeSort(groupId);

        Boolean[] isDecoy = scoreData.getIsDecoy();
        Double[][] scores = scoreData.getScoreData();
        String[] newGroupId = new String[groupIdLength];
        Boolean[] newIsDecoy = new Boolean[groupIdLength];
        Double[][] newScores = new Double[groupIdLength][scores[0].length];

        for (int i = 0; i < groupIdLength; i++) {
            int j = index[i];
            newGroupId[i] = groupId[j];
            newIsDecoy[i] = isDecoy[j];
            newScores[i] = scores[j];
        }
        Integer[] newGroupNumId = AirusUtils.getGroupNumId(newGroupId);
        scoreData.setGroupId(newGroupId);
        scoreData.setIsDecoy(newIsDecoy);
        scoreData.setScoreData(newScores);
        scoreData.setGroupNumId(newGroupNumId);

        return scoreData;
    }

    public static int checkFdr(FinalResult finalResult) {
        int count = 0;
        for (double d : finalResult.getAllInfo().getStatMetrics().getFdr()) {
            if (d < 0.01) {
                count++;
            }
        }
        return count;
    }

    private static Boolean[] getIsTarget(Boolean[] isDecoy) {
        Boolean[] isTarget = new Boolean[isDecoy.length];
        for (int i = 0; i < isDecoy.length; i++) {
            isTarget[i] = !isDecoy[i];
        }
        return isTarget;
    }

    private static Boolean[] getIsTopDecoy(Boolean[] isDecoy, Boolean[] index) {
        if (isDecoy.length == index.length) {
            Boolean[] isTopDecoy = new Boolean[isDecoy.length];
            for (int i = 0; i < isDecoy.length; i++) {
                isTopDecoy[i] = isDecoy[i] && index[i];
            }
            return isTopDecoy;
        } else {
            logger.error("GetIsTopDecoy Error.Length not equals");
            return null;
        }
    }

    private static Boolean[] getIsTopTarget(Boolean[] isDecoy, Boolean[] index) {

        if (isDecoy.length == index.length) {
            Boolean[] isTopTarget = new Boolean[isDecoy.length];
            for (int i = 0; i < isDecoy.length; i++) {
                isTopTarget[i] = !isDecoy[i] && index[i];
            }
            return isTopTarget;
        } else {
            logger.error("GetIsTopTarget Error.Length not equals");
            return null;
        }
    }
}
