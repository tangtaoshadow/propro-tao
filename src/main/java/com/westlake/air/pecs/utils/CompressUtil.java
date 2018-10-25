package com.westlake.air.pecs.utils;

import me.lemire.integercompression.*;
import me.lemire.integercompression.differential.*;
import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class CompressUtil {

    //byte[]压缩为byte[]
    public static byte[] compress(byte[] data) {
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

    public static byte[] decompress(byte[] data) {
        byte[] output = null;

        Inflater decompresser = new Inflater();
        decompresser.reset();
        decompresser.setInput(data);

        ByteArrayOutputStream o = new ByteArrayOutputStream(data.length);
        try {
            byte[] buf = new byte[2048];
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

    public static int[] compressForSortedInt(int[] target) {
        SkippableIntegratedComposition codec = new SkippableIntegratedComposition(new IntegratedBinaryPacking(), new IntegratedVariableByte());
        // output vector should be large enough...
        int[] compressed = new int[target.length + 1024];
        IntWrapper inputoffset = new IntWrapper(0);
        IntWrapper outputoffset = new IntWrapper(1);
        codec.headlessCompress(target, inputoffset, target.length, compressed, outputoffset, new IntWrapper(0));
        compressed[0] = target.length;
        compressed = Arrays.copyOf(compressed, outputoffset.intValue());
        return compressed;
    }

    public static int[] decompressForSortedInt(int[] compressed) {
        SkippableIntegratedComposition codec = new SkippableIntegratedComposition(new IntegratedBinaryPacking(), new IntegratedVariableByte());
        int size = compressed[0];
        // output vector should be large enough...
        int[] recovered = new int[size];
        IntWrapper inPoso = new IntWrapper(1);
        IntWrapper outPoso = new IntWrapper(0);
        IntWrapper recoffset = new IntWrapper(0);
        codec.headlessUncompress(compressed, inPoso, compressed.length, recovered, recoffset, size, outPoso);

        return recovered;
    }

    /**
     * 效果并不是很好的功能函数
     * @param target
     * @return
     */
    public static int[] compressForUnsortedInt(int[] target) {
        IntegerCODEC codec =  new Composition(new FastPFOR(), new VariableByte());
        // output vector should be large enough...
        int[] compressed = new int[target.length + 1024];
        IntWrapper inputoffset = new IntWrapper(0);
        IntWrapper outputoffset = new IntWrapper(0);
        codec.compress(target,inputoffset,target.length,compressed,outputoffset);
        compressed[0] = target.length;
        compressed = Arrays.copyOf(compressed, outputoffset.intValue());
        return compressed;
    }

    /**
     * 效果并不是很好的功能函数
     * @param compressed
     * @return
     */
    public static int[] decompressForUnsortedInt(int[] compressed) {
        IntegerCODEC codec =  new Composition(new FastPFOR(), new VariableByte());
        int[] recovered = new int[compressed[0]];
        IntWrapper recoffset = new IntWrapper(0);
        codec.uncompress(compressed,new IntWrapper(0),compressed.length,recovered,recoffset);

        return recovered;
    }

    public static String transToString(float[] target) {
        FloatBuffer fbTarget = FloatBuffer.wrap(target);
        ByteBuffer bbTarget = ByteBuffer.allocate(fbTarget.capacity() * 4);
        bbTarget.asFloatBuffer().put(fbTarget);
        byte[] targetArray = bbTarget.array();
        byte[] targetCompressedArray = CompressUtil.compress(targetArray);
        String targetStr = new String(new Base64().encode(targetCompressedArray));
        return targetStr;
    }

    public static String transToString(int[] target) {
        IntBuffer ibTarget = IntBuffer.wrap(target);
        ByteBuffer bbTarget = ByteBuffer.allocate(ibTarget.capacity() * 4);
        bbTarget.asIntBuffer().put(ibTarget);
        byte[] targetArray = bbTarget.array();
        byte[] targetCompressedArray = CompressUtil.compress(targetArray);
        String targetStr = new String(new Base64().encode(targetCompressedArray));
        return targetStr;
    }

}
