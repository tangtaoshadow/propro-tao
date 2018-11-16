package com.westlake.air.pecs.parser;

import com.westlake.air.pecs.utils.CompressUtil;

import java.nio.*;

public class BaseParser {

    protected static int PRECISION_32 = 32;
    protected static int PRECISION_64 = 64;

    public Float[] getValues(byte[] value, int precision, boolean isCompression, ByteOrder byteOrder) {
        double[] doubleValues;
        Float[] floatValues;
        ByteBuffer byteBuffer = null;

        if (isCompression) {
            byteBuffer = ByteBuffer.wrap(CompressUtil.zlibDecompress(value));
        }else{
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

    /**
     * get mz values only for aird file
     * @param value
     * @return
     */
    public Float[] getMzValues(byte[] value) {

        Float[] floatValues;

        ByteBuffer byteBuffer = ByteBuffer.wrap(value);
        byteBuffer = ByteBuffer.wrap(CompressUtil.zlibDecompress(byteBuffer.array()));
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        IntBuffer ints = byteBuffer.asIntBuffer();
        int[] intValues = new int[ints.capacity()];
        for (int i = 0; i < ints.capacity(); i++) {
            intValues[i] = ints.get(i);
        }
        intValues = CompressUtil.decompressForSortedInt(intValues);
        floatValues = new Float[intValues.length];
        for (int index = 0; index < intValues.length; index++) {
            floatValues[index] = (float) intValues[index] / 1000;
        }
        byteBuffer.clear();
        return floatValues;
    }

}
