package com.westlake.air.propro.utils;

import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.ExperimentDO;

import java.io.File;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-17 15:00
 */
public class ConvolutionUtil {

    /**
     * 计算 mz在[start, end]范围对应的intensity和
     *
     * @param mzArray        从小到到已经排好序
     * @param intensityArray
     * @param mzStart
     * @param mzEnd
     * @return
     */
    public static float accumulation(Float[] mzArray, Float[] intensityArray, Float mzStart, Float mzEnd) {
        float result = 0f;
        try{
            int start = findLeftIndex(mzArray, mzStart);
            if (start == -1) {
                return 0f;
            }
            while (mzArray[start] <= mzEnd) {
                //信号小于35的均认为是噪音直接删除
                if (intensityArray[start] <= 17) {
                    start++;
                    continue;
                }
                result += intensityArray[start];
                start++;
            }
        }catch (Exception e){
            return result;
        }
        return result;
    }

    public static ResultDO<File> checkExperiment(ExperimentDO experimentDO) {
        if (experimentDO == null) {
            return ResultDO.buildError(ResultCode.EXPERIMENT_NOT_EXISTED);
        }
        if (experimentDO.getAirdPath() == null || experimentDO.getAirdPath().isEmpty()) {
            return ResultDO.buildError(ResultCode.FILE_NOT_EXISTED);
        }
        File file = new File(experimentDO.getAirdPath());
        if (!file.exists()) {
            return ResultDO.buildError(ResultCode.FILE_NOT_EXISTED);
        }

        ResultDO<File> resultDO = new ResultDO(true);
        resultDO.setModel(file);
        return resultDO;
    }

    /**
     * 找到从小到大排序的第一个大于目标值的索引
     * 当目标值小于范围中的最小值时,返回-1
     * 当目标值大于范围中的最大值时,返回-2
     *
     * @param array
     * @param target
     * @return
     */
    public static int findLeftIndex(Float[] array, Float target) {
        int pEnd = array.length - 1;
        if (target <= array[0]) {
            return 0;
        }
        if (target >= array[pEnd]) {
            return -1;
        }

        int pStart = 0;
        while (pStart <= pEnd) {
            int tmp = (pStart + pEnd) / 2;
            if (target < array[tmp]) {
                pEnd = tmp - 1;
            } else if (target > array[tmp]) {
                pStart = tmp + 1;
            } else {
                return tmp;
            }
        }
        return pStart;

    }

    /**
     * 找到从小到大排序的第一个大于目标值的索引
     * 当目标值小于范围中的最小值时,返回-1
     * 当目标值大于范围中的最大值时,返回-2
     *
     * @param array
     * @param target
     * @return
     */
    public static int findIndex(Float[] array, Float target, boolean isLeftIndex) {
        if (array == null) {
            return 0;
        }
        int pStart = 0, pEnd = array.length - 1;
        if (isLeftIndex) {
            if (target < array[0]) {
                return 0;
            }
            if (target > array[pEnd]) {
                return -1;
            }
        } else {
            if (target < array[0]) {
                return -1;
            }
            if (target > array[pEnd]) {
                return pEnd;
            }
        }

        while (pStart <= pEnd) {
            int tmp = (pStart + pEnd) / 2;
            if (target < array[tmp]) {
                pEnd = tmp - 1;
            } else if (target > array[tmp]) {
                pStart = tmp + 1;
            } else {
                return tmp;
            }
        }
        if (isLeftIndex) {
            return pStart;
        } else {
            return pStart - 1;
        }
    }

    public static int findIndex(Float[] array, Float target, int pStart, int pEnd, boolean isLeftIndex) {
        if (array == null) {
            return 0;
        }
        if (isLeftIndex) {
            if (target < array[0]) {
                return 0;
            }
            if (target > array[pEnd]) {
                return -1;
            }
        } else {
            if (target < array[0]) {
                return -1;
            }
            if (target > array[pEnd]) {
                return pEnd;
            }
        }

        while (pStart <= pEnd) {
            int tmp = (pStart + pEnd) / 2;
            if (target < array[tmp]) {
                pEnd = tmp - 1;
            } else if (target > array[tmp]) {
                pStart = tmp + 1;
            } else {
                return tmp;
            }
        }
        if (isLeftIndex) {
            return pStart;
        } else {
            return pStart - 1;
        }
    }

    /**
     * 找到从小到大排序的第一个大于目标值的索引
     * 当目标值小于范围中的最小值时,返回-1
     * 当目标值大于范围中的最大值时,返回-2
     *
     * @param array
     * @param target
     * @return
     */
    public static int findIndex(Double[] array, Double target, boolean isLeftIndex) {
        if (array == null) {
            return 0;
        }
        int pStart = 0, pEnd = array.length - 1;
        if (isLeftIndex) {
            if (target < array[0]) {
                return 0;
            }
            if (target > array[pEnd]) {
                return -1;
            }
        } else {
            if (target < array[0]) {
                return -1;
            }
            if (target > array[pEnd]) {
                return pEnd;
            }
        }

        while (pStart <= pEnd) {
            int tmp = (pStart + pEnd) / 2;
            if (target < array[tmp]) {
                pEnd = tmp - 1;
            } else if (target > array[tmp]) {
                pStart = tmp + 1;
            } else {
                return tmp;
            }
        }
        if (isLeftIndex) {
            return pStart;
        } else {
            return pStart - 1;
        }
    }


}
