package com.westlake.air.pecs.compressor;

import com.westlake.air.pecs.domain.db.ScanIndexDO;
import com.westlake.air.pecs.parser.MzMLParser;
import com.westlake.air.pecs.parser.MzXMLParser;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-09 14:57
 */
@Component("mzXMLCompressor")
public class MzXMLCompressor {

    public final Logger logger = LoggerFactory.getLogger(MzMLParser.class);

    @Autowired
    MzXMLParser mzXMLParser;

    public void convert(String originFilePath, String targetFilePath) {
        RandomAccessFile raf = null;
        RandomAccessFile copyRaf = null;
        try {
            //程序开始时时间
            long startTime = System.currentTimeMillis();
            //初始读入文件
            File file = new File(originFilePath);
            raf = new RandomAccessFile(file, "r");
            //建立新的写文件目录
            File filewrite = new File(targetFilePath);
            if (!filewrite.exists()) {
                filewrite.createNewFile();
            }
            copyRaf = new RandomAccessFile(filewrite, "rw");

            //解析数据
            //获得offset起始位置
            long indexOffset = mzXMLParser.parseIndexOffset(raf);
            //将每个offest部分的id和索引值存到hashmap中
            HashMap<Integer, ScanIndexDO> dataMap = mzXMLParser.parseScanStartPosition(indexOffset, raf);
            //从hashmap中得到索引的一个List序列，使用该序列进行scan位置判定
            List<ScanIndexDO> startList = mzXMLParser.indexForSwath(file);
            //对一定数目组的scan块进行转化并返回数组值
            int groupNumber = 3000;
            int k = 0;
            long[] newStartList = new long[startList.size()];
            //写入最开始的scan块之前的文件内容,不做修改
            writeNewString(0, startList.get(0).getStart(), raf, copyRaf);
            //写入修改过后的scan块内容,并记录经过每段修改后scan块减少的大小,记录在newstartlist中,保存的为缩短值
            while (k < startList.size() - groupNumber) {
                long start = System.currentTimeMillis();
                String[] newString = parseAndUpdateOne(raf, startList.get(k).getStart(), startList.get(k + groupNumber - 1).getEnd(), groupNumber, startList, k);
                for (int i = 0; i < groupNumber; i++) {
                    newStartList[i + k] = Long.parseLong(newString[i]);
                }
                copyRaf.writeBytes(newString[groupNumber]);
                writeNewString(startList.get(k + groupNumber - 1).getEnd(), startList.get(k + groupNumber).getStart(), raf, copyRaf);
                logger.info("Cost Time:" + (System.currentTimeMillis() - start));
                k += groupNumber;
            }
            //写最后一段的scan块
            String[] newString = parseAndUpdateOne(raf, startList.get(k).getStart(), startList.get(startList.size() - 1).getEnd(), startList.size() - k, startList, k);
            for (int i = 0; i < startList.size() - k; i++) {
                newStartList[i + k] = Long.parseLong(newString[i]);
            }
            copyRaf.writeBytes(newString[startList.size() - k]);
            //写新的索引位置
            writeOffset(raf, copyRaf, startList, newStartList, startList.get(startList.size() - 1).getEnd(), raf.length() - 1, indexOffset);

            //关闭文件
            raf.close();
            copyRaf.close();

            //程序结束时间
            long endTime = System.currentTimeMillis();

            //计算程序总耗时
            logger.info("程序运行时间：" + (endTime - startTime) + "ms");
        } catch (Exception e) {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

            if (copyRaf != null) {
                try {
                    copyRaf.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

    }

    //写最后一段的offset
    private void writeOffset(RandomAccessFile raf, RandomAccessFile copyraf, List<ScanIndexDO> startlist, long[] newStartlist, long start, long end, long indexoffset) {
        try {
            byte[] reader = mzXMLParser.read(raf, start, (int) (end - start));
            String tmp = new String(reader);
            String[] indexLines = tmp.split("\n");
            long totaldecrease = 0;
            StringBuilder stringBuilder = new StringBuilder();
            int i = 1;
            int k = 0;
            //找到最开始的offset位置
            while (k < indexLines.length) {
                stringBuilder.append(indexLines[k]).append("\n");
                if (indexLines[k].contains("<offset")) {
                    break;
                }
                k++;
            }
            //对每行中的offset进行替换
            while (i < startlist.size()) {
                String current = String.valueOf(startlist.get(i).getStart());
                totaldecrease += newStartlist[i - 1];
                String target = String.valueOf(startlist.get(i).getStart() - totaldecrease);
                indexLines[k + i] = indexLines[k + i].replace(current, target);
                stringBuilder.append(indexLines[k + i]).append("\n");
                i++;
            }
            totaldecrease = totaldecrease + newStartlist[startlist.size() - 1];
            int m = k + i;
            //写最末尾的字符串,并计算indexoffset
            while (m < indexLines.length) {
                if (indexLines[m].contains("<indexOffset")) {
                    indexLines[m] = indexLines[m].replace(String.valueOf(indexoffset), String.valueOf(indexoffset - totaldecrease));
                }
                stringBuilder.append(indexLines[m]).append("\n");
                m++;
            }
            String result = stringBuilder.toString();
            copyraf.writeBytes(result);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    //对每个模块里的内容进行修改, 并记录每一个scan缩短的长度
    public String[] parseAndUpdateOne(RandomAccessFile raf, long start, long end, int groupNumber, List<ScanIndexDO> Startlist, int k) {
        try {
            raf.seek(start);
            byte[] reader = new byte[(int) (end - start)];
            raf.read(reader);
            String tmp = new String(reader);
            String[] finalresult = new String[groupNumber + 1];
            int startindex = 0;
            long[] beginpos = new long[groupNumber];
            long[] endpos = new long[groupNumber];
            int num = 0;
            //结果缓冲StringBuilder
            StringBuilder result = new StringBuilder();
            //获得字符串开始与结尾的相对位置
            while (tmp.indexOf("m/z-int", startindex) != -1) {
                beginpos[num] = tmp.indexOf("m/z-int", startindex) + 9;
                endpos[num] = tmp.indexOf("</peaks>", startindex);
                startindex = (int) endpos[num] + 1;
                num++;
            }

            //生成新的String格式并记录减少的字节数目
            result.append(tmp.substring(0, (int) beginpos[0]));
            for (int i = 0; i < groupNumber - 1; i++) {
                byte[] tmpbyte = tmp.substring((int) beginpos[i], (int) endpos[i]).getBytes();
                result.append(compress64To32(tmpbyte));
                finalresult[i] = (tmpbyte.length - compress64To32(tmpbyte).length()) + "";
                result.append(tmp.substring((int) endpos[i], (int) beginpos[i + 1]));
            }
            //返回结果为string的scan块
            result.append(compress64To32(tmp.substring((int) beginpos[groupNumber - 1], (int) endpos[groupNumber - 1]).getBytes()));
            finalresult[groupNumber - 1] = (tmp.substring((int) beginpos[groupNumber - 1], (int) endpos[groupNumber - 1]).length() - compress64To32(tmp.substring((int) beginpos[groupNumber - 1], (int) endpos[groupNumber - 1]).getBytes()).length()) + "";
            result.append(tmp.substring((int) endpos[groupNumber - 1]));
            finalresult[groupNumber] = result.toString().replace("precision=\"64\"", "precision=\"32\"");
            return finalresult;
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    private String compress64To32(byte[] reader) {
        Float[] values = mzXMLParser.getValues(new Base64().decode(reader), 64, true, ByteOrder.BIG_ENDIAN);
        float[] fvalues = new float[values.length];
        for (int index = 0; index < values.length; index++) {
            fvalues[index] = values[index];
        }
        FloatBuffer floatBuffer = FloatBuffer.wrap(fvalues);
        ByteBuffer byteBuffer = ByteBuffer.allocate(floatBuffer.capacity() * 4);
        byteBuffer.asFloatBuffer().put(floatBuffer);
        byte[] bytearray = byteBuffer.array();
        byte[] compressedvalues = mzXMLParser.compress(bytearray);
        String finalresult = new String(new Base64().encode(compressedvalues));
        return finalresult;
    }

    //给定开始和结束索引,把这中间的字符串从原始文件copy到新的文件上去
    public void writeNewString(long stringstart, long stringend, RandomAccessFile raf, RandomAccessFile copyraf) {
        try {
            byte[] reader = mzXMLParser.read(raf, stringstart, (int) (stringend - stringstart));
            String tobewrite = new String(reader);
            copyraf.writeBytes(tobewrite);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
