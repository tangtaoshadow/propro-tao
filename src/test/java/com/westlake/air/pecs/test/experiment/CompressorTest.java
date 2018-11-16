package com.westlake.air.pecs.test.experiment;

import com.westlake.air.pecs.parser.AirdFileParser;
import com.westlake.air.pecs.test.BaseTest;
import com.westlake.air.pecs.utils.CompressUtil;
import com.westlake.air.pecs.utils.FileUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class CompressorTest extends BaseTest {

    @Autowired
    AirdFileParser airdFileParser;

    @Test
    public void test(){
        float[] a1 = new float[5];
        a1[0] = 1.1f;
        a1[1] = 1.2f;
        a1[2] = 1.3f;
        a1[3] = 1.4f;
        a1[4] = 1.5f;
        byte[] b = CompressUtil.transToByte(a1);
        byte[] c = ArrayUtils.addAll(b, "\r\n".getBytes());
        float[] a2 = new float[5];
        a2[0] = 12.12f;
        a2[1] = 12.22f;
        a2[2] = 12.32f;
        a2[3] = 12.42f;
        a2[4] = 12.52f;
        byte[] b2 = CompressUtil.transToByte(a2);
        byte[] d = ArrayUtils.addAll(c, b2);

        File file = new File("D:/test.bin");
        try {
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(d);
            FileUtil.close(bos);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            RandomAccessFile raf = new RandomAccessFile(file,"r");
            raf.seek(0);
            byte[] bb = new byte[b.length];
            raf.read(bb);
            ByteBuffer byteBuffer = ByteBuffer.wrap(CompressUtil.zlibDecompress(bb));
            byteBuffer.order(ByteOrder.BIG_ENDIAN);
            FloatBuffer floats = byteBuffer.asFloatBuffer();
            Float[] floatValues = new Float[floats.capacity()];
            for (int index = 0; index < floats.capacity(); index++) {
                floatValues[index] = floats.get(index);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
