package com.westlake.air.pecs.test.experiment;

import com.westlake.air.pecs.test.BaseTest;
import com.westlake.air.pecs.utils.CompressUtil;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CompressorTest extends BaseTest {

    public static void main(String[] args){
        float[] a = new float[5];
        a[0] = 1.1f;
        a[1] = 1.2f;
        a[2] = 1.3f;
        a[3] = 1.4f;
        a[4] = 1.5f;
        byte[] b = CompressUtil.transToByte(a);
        byte[] c = ArrayUtils.addAll(b, "\r\n".getBytes());

        File file = new File("D:/test.bin");
        try {
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
