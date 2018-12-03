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
        int start = findIndex(mzArray, mzStart, true);
        int end = findIndex(mzArray, mzEnd, false);

        if(start == -1 || end == -1){
            return 0f;
        }
        for (int index = start; index <= end; index++) {
            result += intensityArray[index];
        }
        return result;
    }

    public static ResultDO<File> checkExperiment(ExperimentDO experimentDO) {
        if (experimentDO == null) {
            return ResultDO.buildError(ResultCode.EXPERIMENT_NOT_EXISTED);
        }
        if (!experimentDO.getHasAirusFile()) {
            return ResultDO.buildError(ResultCode.EXPERIMENT_MZXML_FILE_MUST_BE_CONVERTED_TO_AIRD_FORMAT_FILE_FIRST);
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
     * @param array
     * @param target
     * @return
     */
    public static int findIndex(Float[] array, Float target, boolean isLeftIndex) {
        if(array == null){
            return 0;
        }
        int pStart = 0, pEnd = array.length - 1;
        if(isLeftIndex){
            if(target < array[0]){
                return 0;
            }
            if(target > array[pEnd]){
                return -1;
            }
        }else{
            if(target < array[0]){
                return -1;
            }
            if(target > array[pEnd]){
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
        if (isLeftIndex){
            return pStart;
        }else{
            return pStart - 1;
        }
    }

    /**
     * 找到从小到大排序的第一个大于目标值的索引
     * 当目标值小于范围中的最小值时,返回-1
     * 当目标值大于范围中的最大值时,返回-2
     * @param array
     * @param target
     * @return
     */
    public static int findIndex(Double[] array, Double target, boolean isLeftIndex) {
        if(array == null){
            return 0;
        }
        int pStart = 0, pEnd = array.length - 1;
        if(isLeftIndex){
            if(target < array[0]){
                return 0;
            }
            if(target > array[pEnd]){
                return -1;
            }
        }else{
            if(target < array[0]){
                return -1;
            }
            if(target > array[pEnd]){
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
        if (isLeftIndex){
            return pStart;
        }else{
            return pStart - 1;
        }
    }


}
