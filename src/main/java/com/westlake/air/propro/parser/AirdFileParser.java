package com.westlake.air.propro.parser;

import com.westlake.air.propro.constants.Constants;
import com.westlake.air.propro.constants.PositionType;
import com.westlake.air.propro.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.propro.domain.bean.scanindex.Position;
import com.westlake.air.propro.domain.db.ScanIndexDO;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

@Component("airdFileParser")
public class AirdFileParser extends BaseParser {

    /**
     * the result key is rt
     *
     * @param raf
     * @return
     * @throws Exception
     */
    public TreeMap<Float, MzIntensityPairs> parseSwathBlockValues(RandomAccessFile raf, ScanIndexDO indexDO, ByteOrder byteOrder) throws Exception {
        TreeMap<Float, MzIntensityPairs> map = new TreeMap<>();
        List<Float> rts = indexDO.getRts();
        List<MzIntensityPairs> pairsList = new ArrayList<>();

        raf.seek(indexDO.getPosStart(PositionType.SWATH));
        byte[] result = new byte[indexDO.getPosDelta(PositionType.SWATH).intValue()];

        raf.read(result);
        List<Integer> blockSizes = indexDO.getBlocks();

        int start = 0;
        for (int i = 0; i < blockSizes.size() - 1; i = i + 2) {
            byte[] mz = ArrayUtils.subarray(result, start, start + blockSizes.get(i));
            start = start + blockSizes.get(i);
            byte[] intensity = ArrayUtils.subarray(result, start, start + blockSizes.get(i + 1));
            start = start + blockSizes.get(i + 1);
            pairsList.add(new MzIntensityPairs(getMzValues(mz, byteOrder), getIntValues(intensity, byteOrder)));
        }
        if (rts.size() != pairsList.size()) {
            logger.error("RTs Length not equals to pairsList length!!!");
            throw new Exception("RTs Length not equals to pairsList length!!!");
        }
        for (int i = 0; i < rts.size(); i++) {
            map.put(rts.get(i), pairsList.get(i));
        }
        return map;
    }

    /**
     * 从aird文件中获取某一条记录
     * 由于Aird文件采用的必然是32位,zlib压缩,因此不需要再传入压缩类型和压缩精度
     *
     * @param raf
     * @param mzPos
     * @param intPos
     * @return
     */
    public MzIntensityPairs parseValue(RandomAccessFile raf, Position mzPos, Position intPos,ByteOrder order) {

        try {
            raf.seek(mzPos.getStart());
            byte[] reader = new byte[mzPos.getDelta().intValue()];
            raf.read(reader);
            Float[] mzArray = getMzValues(reader, order);
            raf.seek(intPos.getStart());
            reader = new byte[intPos.getDelta().intValue()];
            raf.read(reader);

            Float[] intensityArray = getValues(reader, Constants.AIRD_PRECISION_32, true, order);
            return new MzIntensityPairs(mzArray, intensityArray);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }
}
