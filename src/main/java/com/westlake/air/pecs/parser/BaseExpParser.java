package com.westlake.air.pecs.parser;

import com.westlake.air.pecs.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.parser.xml.AirXStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.zip.Deflater;
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

    public abstract List<ScanIndexDO> index(File file, String experimentId, TaskDO taskDO);

    public abstract MzIntensityPairs parseOne(RandomAccessFile raf, long start, long end);

    public abstract MzIntensityPairs getPeakMap(byte[] value, int precision, boolean isZlibCompression);

    public abstract MzIntensityPairs getPeakMap(byte[] mz, byte[] intensity, int mzPrecision, int intensityPrecision, boolean isZlibCompression);

    public Float[] getValues(byte[] value, int precision, boolean isCompression, ByteOrder byteOrder) {
        double[] doubleValues;
        Float[] floatValues;
        ByteBuffer byteBuffer = ByteBuffer.wrap(value);

        if (isCompression) {
            byteBuffer = ByteBuffer.wrap(decompress(byteBuffer.array()));
        }

        byteBuffer.order(byteOrder);
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

    //byte[]压缩为byte[]
    public byte[] compress(byte[] data) {
        byte[] output;

        Deflater compresser = new Deflater();

        compresser.reset();
        compresser.setInput(data);
        compresser.finish();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
        try {
            byte[] buf = new byte[1024];
            while (!compresser.finished()) {
                int i = compresser.deflate(buf);
                bos.write(buf, 0, i);
            }
            output = bos.toByteArray();
        } catch (Exception e) {
            output = data;
            e.printStackTrace();
        } finally {
            try {
                bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        compresser.end();
        return output;
    }

    public byte[] decompress(byte[] data) {
        byte[] output = new byte[0];

        Inflater decompresser = new Inflater();
        decompresser.reset();
        decompresser.setInput(data);

        ByteArrayOutputStream o = new ByteArrayOutputStream(data.length);
        try {
            byte[] buf = new byte[1024];
            while (!decompresser.finished()) {
                int i = decompresser.inflate(buf);
                o.write(buf, 0, i);
            }
            output = o.toByteArray();
        } catch (Exception e) {
            output = data;
            e.printStackTrace();
        } finally {
            try {
                o.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        decompresser.end();
        return output;
    }
}
