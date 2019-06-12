package com.westlake.air.propro.utils;

import com.alibaba.fastjson.JSONArray;
import com.westlake.air.propro.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.propro.domain.bean.file.TableFile;
import com.westlake.air.propro.domain.db.AnalyseDataDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-28 21:45
 */
public class FileUtil {

    public final Logger logger = LoggerFactory.getLogger(getClass());
    public static final String COMMA = ",";
    public static final String TAB = "\t";

    public static String readFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        int fileLength = fis.available();
        byte[] bytes = new byte[fileLength];
        fis.read(bytes);
        return new String(bytes, 0, fileLength);
    }

    public static String readFile(String filePath) throws IOException {
        File file = new File(filePath);
        FileInputStream fis = new FileInputStream(file);
        int fileLength = fis.available();
        byte[] bytes = new byte[fileLength];
        fis.read(bytes);
        return new String(bytes, 0, fileLength);
    }

    public static TableFile readTableFile(String filePath) throws IOException {
        File file = new File(filePath);
        InputStreamReader isr = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(isr);
        String line = reader.readLine();
        if (line == null){
            return null;
        }
        String splitter = TAB;
        String[] columns = line.split(splitter);
        if (columns.length == 1){
            splitter = COMMA;
            columns = line.split(splitter);
        }
        HashMap<String, Integer> columnMap = new HashMap<>();
        List<String[]> fileData = new ArrayList<>();
        for (int i=0; i<columns.length; i++){
            columnMap.put(columns[i].toLowerCase(), i);
        }
        while ((line = reader.readLine()) != null){
            String[] lineSplit = line.split(splitter);
            if (lineSplit.length != columnMap.size()){
                return null;
            }
            fileData.add(lineSplit);
        }
        return new TableFile(columnMap, fileData);
    }

    public static String readFileFromSource(String filePath) throws IOException {
        File file = new File(FileUtil.class.getClassLoader().getResource(filePath).getPath());
        FileInputStream fis = new FileInputStream(file);
        int fileLength = fis.available();
        byte[] bytes = new byte[fileLength];
        fis.read(bytes);
        return new String(bytes, 0, fileLength);
    }

    public static List<AnalyseDataDO> readAnalyseDataFromJsonFile(String filePath) throws IOException {
        File file = new File(filePath);
        FileInputStream fis = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(fis));
        String line = br.readLine();
        List<AnalyseDataDO> dataList = new ArrayList<>();
        while (line != null) {
            dataList.addAll(JSONArray.parseArray(line, AnalyseDataDO.class));
            line = br.readLine();
        }
        br.close();
        return dataList;
    }

    //根据Aird文件获取同名同目录下的Aird索引文件的文件路径
    public static String getAirdIndexFilePath(String airdFilePath){
        return airdFilePath.substring(0,airdFilePath.lastIndexOf(".")) + ".json";
    }

    public static boolean isAirdFile(String airdFilePath){
        return airdFilePath.toLowerCase().endsWith(".aird");
    }

    public static boolean isAirdIndexFile(String airdIndexFilePath){
        return airdIndexFilePath.toLowerCase().endsWith(".json");
    }

    public static boolean isMzXMLFile(String mzXMLFilePath){
        return mzXMLFilePath.toLowerCase().endsWith(".mzxml");
    }

    public static List<AnalyseDataDO> getAnalyseDataList(String filePath) throws IOException {
        String content = readFile(filePath);
        List<AnalyseDataDO> dataList = JSONArray.parseArray(content, AnalyseDataDO.class);
        return dataList;
    }

    public static RtIntensityPairsDouble txtReader(BufferedReader reader, String divide, int column1, int column2) throws IOException {
        String line = reader.readLine();
        List<Double> rtList = new ArrayList<>();
        List<Double> intensityList = new ArrayList<>();
        while (line != null) {
            String[] item = line.split(divide);
            rtList.add(Double.parseDouble(item[column1]));
            intensityList.add(Double.parseDouble(item[column2]));
            line = reader.readLine();
        }
        Double[] rtArray = new Double[rtList.size()];
        Double[] intArray = new Double[intensityList.size()];
        for (int i = 0; i < rtArray.length; i++) {
            rtArray[i] = rtList.get(i);
            intArray[i] = intensityList.get(i);
        }
        return new RtIntensityPairsDouble(rtArray, intArray);

    }

    public static void writeFile(String filePath, String content, boolean isOverride) throws IOException {
        File file = new File(filePath);
        if (isOverride) {
            file.createNewFile();
        } else {
            if (!file.exists()) {
                file.createNewFile();
            }
        }

        byte[] b = content.getBytes();
        int l = b.length;
        OutputStream os = new FileOutputStream(file);
        os.write(b, 0, l);
        os.close();
    }

    public static void writeFile(String filePath, List list, boolean isOverride) throws IOException {
        File file = new File(filePath);
        if (isOverride) {
            file.createNewFile();
        } else {
            if (!file.exists()) {
                file.createNewFile();
            }
        }

        String content = JSONArray.toJSONString(list);
        byte[] b = content.getBytes();
        int l = b.length;
        OutputStream os = new FileOutputStream(file);
        os.write(b, 0, l);
        os.close();
    }

    public static void fileInputStreamSkip(FileInputStream inputStream, long skip) throws IOException {
        //避免IO错误
        while (skip > 0) {
            long amt = inputStream.skip(skip);
            if (amt == -1) {
                throw new RuntimeException(inputStream + ": unexpected EOF");
            }
            skip -= amt;
        }
    }

    public static void close(RandomAccessFile raf) {
        if (raf != null) {
            try {
                raf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void close(FileWriter fw) {
        if (fw != null) {
            try {
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void close(BufferedWriter bw) {
        if (bw != null) {
            try {
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void close(FileOutputStream fos) {
        if (fos != null) {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void close(BufferedOutputStream bos) {
        if (bos != null) {
            try {
                bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
