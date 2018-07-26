package com.westlake.air.pecs.parser;

import com.westlake.air.pecs.domain.bean.MzIntensityPairs;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import com.westlake.air.pecs.parser.xml.AirXStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-26 21:13
 */
public abstract class BaseExpParser {

    public final Logger logger = LoggerFactory.getLogger(BaseExpParser.class);

    protected static int TAIL_TRY = 30;
    protected static int PRECISION_32 = 32;
    protected static int PRECISION_64 = 64;
//    static final Pattern attrPattern = Pattern.compile("(\\w+)=\"([^\"]*)\"");

    @Autowired
    AirXStream airXStream;

    private static byte[] prefix = "<scanList>".getBytes();
    private static byte[] suffix = "</scanList>".getBytes();

    public abstract List<ScanIndexDO> index(File file, String experimentId);

    public abstract MzIntensityPairs parseOne(RandomAccessFile raf, long start, long end);

    public abstract MzIntensityPairs getPeakMap(byte[] value, int precision, boolean isZlibCompression);

    public abstract MzIntensityPairs getPeakMap(byte[] mz, byte[] intensity, int mzPrecision, int intensityPrecision, boolean isZlibCompression);

    protected Float[] getValues(byte[] value, int precision, boolean isCompression) {
        double[] doubleValues;
        Float[] floatValues;
        ByteBuffer byteBuffer = ByteBuffer.wrap(value);

        if (isCompression) {
            Inflater decompresser = new Inflater();
            decompresser.setInput(byteBuffer.array());

            byte[] decompressedData = new byte[byteBuffer.capacity() * 10];

            try {
                int usedLength = decompresser.inflate(decompressedData);
                byteBuffer = ByteBuffer.wrap(decompressedData, 0, usedLength);
            } catch (DataFormatException e) {
                logger.error("Decompress failed!", e);
            }
        }

        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        if (precision == PRECISION_64) {
            doubleValues = new double[byteBuffer.asDoubleBuffer().capacity()];
            byteBuffer.asDoubleBuffer().get(doubleValues);
            floatValues = new Float[doubleValues.length];
            for (int index = 0; index < doubleValues.length; index++) {
                floatValues[index] = (float) doubleValues[index];
            }
        } else {
            FloatBuffer floats = byteBuffer.asFloatBuffer();
            floatValues = new Float[floats.capacity()];

            for (int index = 0; index < floats.capacity(); index++) {
                floatValues[index] = floats.get(index);
            }
        }

        byteBuffer.clear();
        return floatValues;
    }
}
