package com.westlake.air.pecs.utils;

import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import org.apache.commons.lang3.ArrayUtils;

public class AnalyseDataUtil {

    public static void compress(AnalyseDataDO data) {
        if (data.getRtArray() != null) {
            data.setConvRtArray(CompressUtil.zlibCompress(CompressUtil.transToByte(ArrayUtils.toPrimitive(data.getRtArray()))));
            data.setRtArray(null);
        }
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
