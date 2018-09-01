package com.westlake.air.pecs.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;


import java.util.ArrayList;
import java.io.*;
import java.util.List;

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

    public static List<AnalyseDataDO> getAnalyseDataList(String filePath) throws IOException {
        String content = readFile(filePath);
        List<AnalyseDataDO> dataList = JSONArray.parseArray(content, AnalyseDataDO.class);
        return dataList;
    }

    public static RtIntensityPairsDouble txtReader(BufferedReader reader,String divide, int column1, int column2) throws IOException {
        String line = reader.readLine();
        List<Double> rtList = new ArrayList<>();
        List<Double> intensityList = new ArrayList<>();
        while (line != null){
            String[] item = line.split(divide);
            rtList.add(Double.parseDouble(item[column1]));
            intensityList.add(Double.parseDouble(item[column2]));
            line = reader.readLine();
        }
        Double[] rtArray = new Double[rtList.size()];
        Double[] intArray = new Double[intensityList.size()];
        for(int i=0; i<rtArray.length; i++){
            rtArray[i] = rtList.get(i);
            intArray[i] = intensityList.get(i);
        }
        return new RtIntensityPairsDouble(rtArray, intArray);

    }

    public static void writeFile(String filePath, List list) throws IOException {
        File file = new File(filePath);
        if(!file.exists()){
            file.createNewFile();
        }
        String content = JSONArray.toJSONString(list);
        byte[] b = content.getBytes();
        int l = b.length;
        OutputStream os = new FileOutputStream(file);
        os.write(b,0,l);
        os.close();
    }
}
