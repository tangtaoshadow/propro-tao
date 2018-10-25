package com.westlake.air.pecs.parser;

import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.domain.bean.analyse.MzIntensityPairs;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;

@Component("airFileParser")
public class AirFileParser extends BaseParser {

    public MzIntensityPairs parseValue(RandomAccessFile raf, long start, long end, String compressionType, String precision){

        try {
            raf.seek(start);
            byte[] reader = new byte[(int) (end - start)];
            raf.read(reader);
            String tmp = new String(reader);
            String[] mzIntensity = tmp.split(Constants.CHANGE_LINE);
            String mzStr = mzIntensity[0];
            String intensityStr = mzIntensity[1];
            Float[] mzArray = getValues(new Base64().decode(mzStr));
            Float[] intensityArray = getValues(new Base64().decode(intensityStr), Integer.parseInt(precision), "zlib".equalsIgnoreCase(compressionType), ByteOrder.BIG_ENDIAN);
            return new MzIntensityPairs(mzArray, intensityArray);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }
}
