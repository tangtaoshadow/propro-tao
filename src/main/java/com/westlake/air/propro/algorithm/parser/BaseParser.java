package com.westlake.air.propro.algorithm.parser;

import com.westlake.air.propro.utils.CompressUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.*;

public class BaseParser {

    protected static int PRECISION_32 = 32;
    protected static int PRECISION_64 = 64;

    public final Logger logger = LoggerFactory.getLogger(BaseParser.class);

    public Float[] getValues(byte[] value, int precision, boolean isCompression, ByteOrder byteOrder) {
        double[] doubleValues;
        Float[] floatValues;
        ByteBuffer byteBuffer = null;

        if (isCompression) {
            byteBuffer = ByteBuffer.wrap(CompressUtil.zlibDecompress(value));
        } else {
            byteBuffer = ByteBuffer.wrap(value);
        }

        byteBuffer.order(byteOrder);
        if (precision == PRECISION_64) {
            DoubleBuffer doubleBuffer = byteBuffer.asDoubleBuffer();
            doubleValues = new double[doubleBuffer.capacity()];
            doubleBuffer.get(doubleValues);
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

    //默认为BIG_ENDIAN,精度为小数点后三位
    public Float[] getMzValues(byte[] value) throws Exception {
        return getMzValues(value, ByteOrder.BIG_ENDIAN);
    }

    /**
     * get mz values only for aird file
     *
     * @param value
     * @return
     */
    public Float[] getMzValues(byte[] value, ByteOrder order) throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.wrap(CompressUtil.zlibDecompress(value));
        byteBuffer.order(order);

        IntBuffer ints = byteBuffer.asIntBuffer();
        int[] intValues = new int[ints.capacity()];
        for (int i = 0; i < ints.capacity(); i++) {
            intValues[i] = ints.get(i);
        }
        intValues = CompressUtil.decompressForSortedInt(intValues);
        Float[] floatValues = new Float[intValues.length];
        for (int index = 0; index < intValues.length; index++) {
            floatValues[index] = (float) intValues[index] / 1000;
        }
        byteBuffer.clear();
        return floatValues;
    }

    //默认为BIG_ENDIAN
    public Float[] getIntValues(byte[] value) throws Exception {
        return getIntValues(value, ByteOrder.BIG_ENDIAN);
    }

    /**
     * get mz values only for aird file
     *
     * @param value
     * @return
     */
    public Float[] getIntValues(byte[] value, ByteOrder order) throws Exception {
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(CompressUtil.zlibDecompress(value));
            byteBuffer.order(order);

            FloatBuffer intensities = byteBuffer.asFloatBuffer();
            Float[] intValues = new Float[intensities.capacity()];
            for (int i = 0; i < intensities.capacity(); i++) {
                intValues[i] = intensities.get(i);
            }

            byteBuffer.clear();
            return intValues;
        } catch (Exception e) {
            throw e;
        }

    }

}
