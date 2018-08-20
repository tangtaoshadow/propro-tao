package com.westlake.air.pecs.utils;

import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.ExperimentDO;

import java.io.File;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-17 15:00
 */
public class ConvolutionUtil {

    /**
     * 计算 mz在[mzStart, mzEnd]范围对应的intensity和
     *
     * @param mzArray        从小到到已经排好序
     * @param intensityArray
     * @param mzStart
     * @param mzEnd
     * @return
     */
    public static Float accumulation(Float[] mzArray, Float[] intensityArray, Float mzStart, Float mzEnd) {
        Float result = 0f;
        int start = findIndex(mzArray, mzStart);
        int end = findIndex(mzArray, mzEnd) - 1;
        for (int index = start; index <= end; index++) {
            result += intensityArray[index];
        }
        return result;
    }

    public static ResultDO<File> checkExperiment(ExperimentDO experimentDO) {
        if (experimentDO == null) {
            return ResultDO.buildError(ResultCode.EXPERIMENT_NOT_EXISTED);
        }
        if (experimentDO.getFileLocation() == null || experimentDO.getFileLocation().isEmpty()) {
            return ResultDO.buildError(ResultCode.FILE_NOT_SET);
        }
        File file = new File(experimentDO.getFileLocation());
        if (!file.exists()) {
            return ResultDO.buildError(ResultCode.FILE_NOT_EXISTED);
        }

        ResultDO<File> resultDO = new ResultDO(true);
        resultDO.setModel(file);
        return resultDO;
    }

    // 找到从小到大排序的第一个大于目标值的索引
    private static int findIndex(Float[] array, Float target) {
        int pStart = 0, pEnd = array.length - 1;
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
}
