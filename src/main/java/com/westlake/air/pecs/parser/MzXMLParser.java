package com.westlake.air.pecs.parser;

import com.westlake.air.pecs.domain.bean.MzIntensityPairs;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import com.westlake.air.pecs.parser.model.mzxml.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 16:50
 */
@Component
public class MzXMLParser extends BaseExpParser {

    public final Logger logger = LoggerFactory.getLogger(MzXMLParser.class);

    public Class<?>[] classes = new Class[]{
            DataProcessing.class, Maldi.class, MsInstrument.class, MsRun.class, MzXML.class, NameValue.class,
            OntologyEntry.class, Operator.class, Orientation.class, ParentFile.class, Pattern.class,
            Peaks.class, Plate.class, PrecursorMz.class, Robot.class, Scan.class, ScanOrigin.class,
            Separation.class, SeparationTechnique.class, Software.class, Spot.class, Spotting.class,
    };

    public MzXMLParser() {
    }

    private void prepare() {
        airXStream.processAnnotations(classes);
        airXStream.allowTypes(classes);
    }

    @Override
    public List<ScanIndexDO> index(File file, String experimentId) {
        RandomAccessFile raf = null;
        List<ScanIndexDO> list = null;
        try {
            raf = new RandomAccessFile(file, "r");
            list = indexForSwath(file);
            int count = 0;

            ScanIndexDO currentMS1 = null;
            for (ScanIndexDO scanIndex : list) {
                parseAttribute(raf, scanIndex);
                scanIndex.setExperimentId(experimentId);

                if (scanIndex.getMsLevel() == 1) {
                    currentMS1 = scanIndex;
                } else {
                    if (currentMS1 == null) {
                        continue;
                    } else {
                        scanIndex.setParentNum(currentMS1.getNum());
                    }
                }

                count++;
                if (count % 10000 == 0) {
                    logger.info("已扫描索引:" + count + "/" + list.size() + "条");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (raf != null) {
                    raf.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return list;
    }

    @Override
    public MzIntensityPairs parseOne(RandomAccessFile raf, long start, long end) {
        try {
            raf.seek(start);
            byte[] reader = new byte[(int) (end - start)];
            raf.read(reader);
            String tmp = new String(reader);
            String[] content = tmp.substring(tmp.indexOf("<peaks"), tmp.indexOf("</peaks>")).split(">");
            String[] attributes = content[0].split("\n");
            String precision = null;
            String compressionType = null;
            int targetCount = 0;
            for (String attribute : attributes) {
                if (attribute.trim().contains("compressionType")) {
                    compressionType = attribute.split("=")[1].replace("\"", "");
                    targetCount++;
                }
                if (attribute.trim().contains("precision")) {
                    precision = attribute.split("=")[1].replace("\"", "");
                    targetCount++;
                }
                if (targetCount == 2) {
                    break;
                }
            }
            String value = content[1];
            MzIntensityPairs pairs = getPeakMap(new Base64().decode(value), Integer.parseInt(precision), "zlib".equalsIgnoreCase(compressionType));
            return pairs;
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        return null;
    }

    //For MzXML
    @Override
    public MzIntensityPairs getPeakMap(byte[] value, int precision, boolean isZlibCompression) {

        Float[] values = getValues(value, precision, isZlibCompression, ByteOrder.BIG_ENDIAN);

        TreeMap<Float, Float> map = new TreeMap<>();
        for (int peakIndex = 0; peakIndex < values.length - 1; peakIndex += 2) {
            Float mz = values[peakIndex];
            Float intensity = values[peakIndex + 1];
            map.put(mz, intensity);
        }

        Float[] mzArray = new Float[map.size()];
        Float[] intensityArray = new Float[map.size()];
        int i = 0;
        for (Float key : map.keySet()) {
            mzArray[i] = key;
            intensityArray[i] = map.get(key);
            i++;
        }

        MzIntensityPairs pairs = new MzIntensityPairs();
        pairs.setMzArray(mzArray);
        pairs.setIntensityArray(intensityArray);

        return pairs;
    }

    //不需要实现
    @Override
    public MzIntensityPairs getPeakMap(byte[] mz, byte[] intensity, int mzPrecision, int intensityPrecision, boolean isZlibCompression) {
        return null;
    }

    public Long parseIndexOffset(RandomAccessFile rf) throws IOException {
        long position = rf.length() - 1;
        rf.seek(position);
        byte words[] = new byte[1];
        for (int i = 0; i < 1000; i++) {
            rf.read(words);
            if (words[0] == '\n') {
                //detect the line break symbol
                String line = rf.readLine();
                if (line != null && line.contains("<indexOffset>")) {
                    line = line.trim().replace("<indexOffset>", "").replace("</indexOffset>", "");
                    return Long.valueOf(line);
                }
            }

            rf.seek(--position);
        }
        return 0L;
    }
    /**
     * 本算法得到的ScanIndex的End包含了换行符
     * ">"的byte编码是62
     * <p>
     * 小区域关键帧检测算法
     *
     * @param file
     * @return
     * @throws IOException
     */
    private List<ScanIndexDO> index(File file) {

        List<ScanIndexDO> indexList = new ArrayList<>();
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            //获取索引的起始位置
            Long indexOffset = parseIndexOffset(raf);

            //根据索引的内容获取id以及对应的startPosition
            HashMap<Integer, ScanIndexDO> indexMap = parseScanStartPosition(indexOffset, raf);

            //记录总共的scan条数
            int totalCount = indexMap.size();

            Stack<ScanIndexDO> indexStack = new Stack<>();
            byte[] lastRead;
            //对所有的记录开始做for循环
            for (int i = 1; i <= totalCount; i++) {

                //如果已经是最后一个元素了
                if (i == totalCount) {
                    //读取尾部的50个字符
                    lastRead = read(raf, indexOffset - 50, 50);
                    //如果stack中不为空,说明这个节点是一个二级节点
                    if (!indexStack.isEmpty()) {
                        //先处理一级节点的位置,因为包含了</msRun>这个额外的标签,所以需要从indexOffset的位置往前搜索2个">"
                        ScanIndexDO lastIndex = indexStack.pop();
                        Long length = searchForLength(lastRead, 2, 62);
                        lastIndex.setEnd(indexOffset - length);

                        //再处理二级节点,二级节点则是从indexOffset往前处理3个">"
                        Long length2 = searchForLength(lastRead, 3, 62);
                        indexMap.get(i).setEnd(indexOffset - length2);

                        //二级节点处理完毕以后为二级节点设置对应的父节点
                        indexMap.get(i).setParentNum(lastIndex.getNum());

                        //将一级节点存储List中
                        indexList.add(lastIndex);
                        indexList.add(indexMap.get(i));
                        break;
                    } else {
                        //如果是空说明最后一个节点是一级节点,直接找倒数第二个>即可定位
                        Long length = searchForLength(lastRead, 2, 62);
                        indexMap.get(i).setEnd(indexOffset - length);
                        indexList.add(indexMap.get(i));
                        break;
                    }
                }

                ScanIndexDO index = indexMap.get(i);

                ScanIndexDO nextIndex = indexMap.get(i + 1);
                byte[] tailBytes = read(raf, nextIndex.getStart() - TAIL_TRY, TAIL_TRY);

                String tail = new String(tailBytes);
                //如果stack中不为空,说明这个节点是一个二级节点
                if (!indexStack.isEmpty()) {
                    //如果包含了两个</scan> 说明已经跳出了一级节点到了下一个一级节点
                    if (StringUtils.countMatches(tail, "</scan>") == 2) {
                        //如果检测到两个</scan>,那么认为是跳出了二级节点了,开始为上一个一级节点做索引
                        ScanIndexDO lastIndex = indexStack.pop();
                        lastIndex.setEnd(nextIndex.getStart() - 1);

                        //再为二级节点做索引,二级节点的索引为nextIndex索引一步一步向前搜索,直到搜索到第2个">",我们先往前读30个字节
                        //因为">"的byte编码是62,所以要搜索倒数第二个62所处的位置
                        Long length = searchForLength(tailBytes, 2, 62);
                        index.setEnd(nextIndex.getStart() - length);

                        //将二级节点的ParentNum设置为对应的一级节点
                        index.setParentNum(lastIndex.getNum());

                        indexList.add(lastIndex);
                        indexList.add(index);
                        continue;
                    }

                    //一级节点下的兄弟节点,还未跳出一级节点
                    if (tail.contains("</scan>")) {
                        index.setEnd(nextIndex.getStart() - 1);
                        //操作堆栈中的一级节点,但是不要pop出来
                        index.setParentNum(indexStack.peek().getNum());
                        continue;
                    }
                }

                //如果stack中是空的,那么是一个一级节点
                if (tail.contains("</scan>")) {
                    index.setEnd(nextIndex.getStart() - 1);
                    indexList.add(index);
                    continue;
                }

                if (!tail.contains("</scan>")) {
                    indexStack.push(index);
                    continue;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return indexList;
    }

    /**
     * 本算法专门为Swath格式的mzXML优化,由于Swath的MzXML中是以Circle进行扫描到,即扫描的结果应该是MS1,MS2,MS2.....,MS1,MS2,MS2....这样的循环
     * 并且MS2不会嵌套到MS1里面,因此对于父子关系的判断和index()函数中的不同
     * 由于不存在嵌套关系,因此index()函数中的小区域关键帧检测算法也不需要了
     *
     * @param file
     * @return
     * @throws IOException
     */
    public List<ScanIndexDO> indexForSwath(File file) {

        List<ScanIndexDO> indexList = new ArrayList<>();
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            //获取索引的起始位置
            Long indexOffset = parseIndexOffset(raf);

            //根据索引的内容获取id以及对应的startPosition
            HashMap<Integer, ScanIndexDO> indexMap = parseScanStartPosition(indexOffset, raf);

            //记录总共的scan条数
            int totalCount = indexMap.size();
            byte[] lastRead;
            ScanIndexDO currentMS1 = null;
            //对所有的记录开始做for循环,先解析第一个MS1
            for (int i = 1; i <= totalCount; i++) {
                //如果已经是最后一个元素了
                if (i == totalCount) {
                    //读取尾部的50个字符
                    lastRead = read(raf, indexOffset - 50, 50);
                    Long length = searchForLength(lastRead, 2, 62);
                    indexMap.get(i).setEnd(indexOffset - length);
                    indexList.add(indexMap.get(i));
                    break;
                }
                ScanIndexDO index = indexMap.get(i);
                ScanIndexDO nextIndex = indexMap.get(i + 1);
                index.setEnd(nextIndex.getStart() - 1);
                indexList.add(index);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return indexList;
    }

    public HashMap<Integer, ScanIndexDO> parseScanStartPosition(Long indexOffset, RandomAccessFile rf) throws IOException {
        HashMap<Integer, ScanIndexDO> indexMap = new HashMap<>();
        rf.seek(indexOffset);
        int indexSize = (int) (rf.length() - 1 - indexOffset);
        byte[] indexArray = new byte[indexSize];
        rf.read(indexArray);
        String totalLine = new String(indexArray);
        String[] indexLines = totalLine.split("\n");
        int count = 1;
        for (String line : indexLines) {
            if (line.contains("<offset")) {
                line = line.trim();
                String id = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""));
                String offset = line.substring(line.indexOf(">") + 1, line.lastIndexOf("<"));
                indexMap.put(count, new ScanIndexDO(Integer.valueOf(id.trim()), Long.valueOf(offset.trim()), null));
                count++;
            }
        }
        return indexMap;
    }

    private Long searchForLength(byte[] strToFind, int findTime, int findStr) {
        int count = 0;
        //因为">"的byte编码时62,所以要搜索倒数第二个62所处的位置
        for (int j = strToFind.length - 1; j >= 0; j--) {
            if (strToFind[j] == findStr) {
                count++;
            }
            if (count == findTime) {
                return (long) (strToFind.length - j - 1);
            }
        }
        return 0L;
    }

    public byte[] read(RandomAccessFile raf, long start, int size) throws IOException {
        byte[] tmp = new byte[size];
        raf.seek(start);
        raf.read(tmp);

        return tmp;
    }

    //解析Scan标签的Attributes
    private void parseAttribute(RandomAccessFile raf, ScanIndexDO scanIndexDO) throws IOException {

        //仅关注两个attribute msLevel和retentionTime.因此如果扫描到这两个属性以后就可以跳出循环以节省时间开销
        int focusAttributeCount = 2;
        byte[] readBytes = read(raf, scanIndexDO.getStart() + 1, 600);

        String read = new String(readBytes);
        String precursorMz;
        if (read.contains("precursorMz")) {
            precursorMz = read.substring(read.indexOf("<precursorMz"), read.indexOf("</precursorMz>") + 14);
            scanIndexDO.setPrecursorMz(Float.parseFloat(precursorMz.substring(precursorMz.indexOf(">") + 1, precursorMz.indexOf("</"))));
            String attributeForPrecursorMz = precursorMz.substring(0, precursorMz.indexOf(">"));
            String[] tmp = null;
            if (attributeForPrecursorMz.contains("\n")) {
                tmp = attributeForPrecursorMz.split("\n");
            } else {
                tmp = attributeForPrecursorMz.split(" ");
            }
            for (String tmpStr : tmp) {
                tmpStr = tmpStr.trim();
                if (tmpStr.startsWith("windowWideness")) {
                    scanIndexDO.setWindowWideness(Float.parseFloat(tmpStr.split("=")[1].replace("\"", "")));
                    break;
                }
            }
            scanIndexDO.setPrecursorMzStart(scanIndexDO.getPrecursorMz() - scanIndexDO.getWindowWideness() / 2);
            scanIndexDO.setPrecursorMzEnd(scanIndexDO.getPrecursorMz() + scanIndexDO.getWindowWideness() / 2);
        }

        String scan = read.substring(0, read.indexOf(">"));
        String[] tmp = null;
        if (scan.contains("\n")) {
            tmp = scan.split("\n");
        } else {//说明属性在一行里面
            tmp = scan.split(" ");
        }

        for (String tmpStr : tmp) {
            tmpStr = tmpStr.trim();
            if (tmpStr.startsWith("msLevel")) {
                scanIndexDO.setMsLevel(Integer.parseInt(tmpStr.split("=")[1].replace("\"", "")));
                focusAttributeCount--;
            }
            if (tmpStr.startsWith("retentionTime")) {
                scanIndexDO.setRtStr(tmpStr.split("=")[1].replace("\"", ""));
                focusAttributeCount--;
            }
            if (focusAttributeCount == 0) {
                break;
            }
        }

    }
}
