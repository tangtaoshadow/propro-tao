package com.westlake.air.pecs.parser;

import com.westlake.air.pecs.utils.CompressUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class BaseParser {

    protected static int PRECISION_32 = 32;
    protected static int PRECISION_64 = 64;

    public Float[] getValues(byte[] value, int precision, boolean isCompression, ByteOrder byteOrder) {
        double[] doubleValues;
        Float[] floatValues;
        ByteBuffer byteBuffer = ByteBuffer.wrap(value);

        if (isCompression) {
            byteBuffer = ByteBuffer.wrap(CompressUtil.decompress(byteBuffer.array()));
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

    public Float[] getValuesFromAird(byte[] value, int precision, boolean isCompression, ByteOrder byteOrder) {
        Float[] values = getValues(value, precision, isCompression, byteOrder);
        for (int i = 0; i < value.length; i++) {
            values[i] = (float)(Math.round(values[i]*1000)/1000);
        }
        return values;
    }
}
