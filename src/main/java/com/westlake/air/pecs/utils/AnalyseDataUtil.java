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
    }
}
