package com.westlake.air.pecs.parser;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.pecs.domain.bean.scanindex.Position;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;

@Component("airdFileParser")
public class AirdFileParser extends BaseParser {

    public MzIntensityPairs parseValueFromText(RandomAccessFile raf, Position position, String compressionType, String precision) {

        try {
            raf.seek(position.getStart());
            byte[] reader = new byte[position.getDelta().intValue()];
            raf.read(reader);
            String tmp = new String(reader);
            String[] mzIntensity = tmp.split(Constants.CHANGE_LINE);
            String mzStr = mzIntensity[0];
            String intensityStr = mzIntensity[1];
            Float[] mzArray = getMzValues(new Base64().decode(mzStr));
            Float[] intensityArray = getValues(new Base64().decode(intensityStr), Integer.parseInt(precision), "zlib".equalsIgnoreCase(compressionType), ByteOrder.BIG_ENDIAN);
            return new MzIntensityPairs(mzArray, intensityArray);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    public MzIntensityPairs parseValueFromBin(RandomAccessFile raf, Position mzPos, Position intPos, String compressionType, String precision) {

        try {
            raf.seek(mzPos.getStart());
            byte[] reader = new byte[mzPos.getDelta().intValue()];
            raf.read(reader);
            Float[] mzArray = getMzValues(reader);
            raf.seek(intPos.getStart());
            reader = new byte[intPos.getDelta().intValue()];
            raf.read(reader);
            Float[] intensityArray = getValues(reader, Integer.parseInt(precision), "zlib".equalsIgnoreCase(compressionType), ByteOrder.BIG_ENDIAN);
            return new MzIntensityPairs(mzArray, intensityArray);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }
}
