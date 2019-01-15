package com.westlake.air.pecs.utils;

import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.RandomAccessFile;
import java.util.HashMap;

public class AnalyseDataUtil {

    public static final Logger logger = LoggerFactory.getLogger(AnalyseDataUtil.class);

    public static ResultDO<AnalyseDataDO> readConvDataFromFile(AnalyseDataDO data, RandomAccessFile raf) throws Exception {
        if (data.getPosDeltaList().size() != (data.getMzMap().size() + 1)) {
            logger.error(ResultCode.POSITION_DELTA_LIST_LENGTH_NOT_EQUAL_TO_MZMAP_PLUS_ONE.getMessage());
            return ResultDO.buildError(ResultCode.POSITION_DELTA_LIST_LENGTH_NOT_EQUAL_TO_MZMAP_PLUS_ONE);
        }
        long start = data.getStartPos();
        raf.seek(start);
        byte[] rtArray = new byte[data.getPosDeltaList().get(0)];
        raf.read(rtArray);
        data.setConvRtArray(rtArray);
        start = start + rtArray.length;
        int i = 1;
        //依次读取PosDeltaList中的位置信息
        for (String key : data.getConvIntensityMap().keySet()) {
            byte[] intensityArray = new byte[data.getPosDeltaList().get(i)];
            raf.seek(start);
            raf.read(intensityArray);
            data.getConvIntensityMap().put(key, intensityArray);
            start = start + intensityArray.length;
            i++;
        }
        ResultDO<AnalyseDataDO> resultDO = new ResultDO(true);
        resultDO.setModel(data);
        return resultDO;
    }

    public static void compress(AnalyseDataDO data) {
        if (data.getRtArray() != null) {
            data.setConvRtArray(CompressUtil.zlibCompress(CompressUtil.transToByte(ArrayUtils.toPrimitive(data.getRtArray()))));
            data.setRtArray(null);
        }
        data.setConvIntensityMap(new HashMap<>());
        for (String cutInfo : data.getIntensityMap().keySet()) {
            Float[] intensities = data.getIntensityMap().get(cutInfo);
            if (intensities != null) {
                data.getConvIntensityMap().put(cutInfo, CompressUtil.zlibCompress(CompressUtil.transToByte(ArrayUtils.toPrimitive(intensities))));
            } else {
                data.getConvIntensityMap().put(cutInfo, null);
            }
        }
        data.setIntensityMap(null);
        data.setCompressed(true);
    }

    public static void decompress(AnalyseDataDO data) {
        if (data.getConvRtArray() != null) {
            data.setRtArray(CompressUtil.transToFloat(CompressUtil.zlibDecompress(data.getConvRtArray())));
            data.setConvRtArray(null);
        }
        for (String cutInfo : data.getConvIntensityMap().keySet()) {
            byte[] intensities = data.getConvIntensityMap().get(cutInfo);
            if (intensities != null) {
                data.getIntensityMap().put(cutInfo, CompressUtil.transToFloat(CompressUtil.zlibDecompress(intensities)));
            } else {
                data.getIntensityMap().put(cutInfo, null);
            }
        }
        data.setConvIntensityMap(null);
        data.setCompressed(false);
    }
}
