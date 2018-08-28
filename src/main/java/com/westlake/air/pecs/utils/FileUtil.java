package com.westlake.air.pecs.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-28 21:45
 */
public class FileUtil {

    public static String readFile(String filePath) throws IOException {
        File file = new File(FileUtil.class.getClassLoader().getResource(filePath).getPath());
        FileInputStream fis = new FileInputStream(file);
        int fileLength = fis.available();
        byte[] bytes = new byte[fileLength];
        fis.read(bytes);
        return new String(bytes, 0, fileLength);
    }
}
