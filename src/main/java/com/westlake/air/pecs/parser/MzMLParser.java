package com.westlake.air.pecs.parser;

import com.westlake.air.pecs.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.parser.model.mzxml.*;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by Song Jian
 * Time: 2018-07-25 20-14
 */
@Component
public class MzMLParser extends BaseExpParser{

    public final Logger logger = LoggerFactory.getLogger(MzMLParser.class);

    public Class<?>[] classes = new Class[]{
            DataProcessing.class, Maldi.class, MsInstrument.class, MsRun.class, MzXML.class, NameValue.class,
            OntologyEntry.class, Operator.class, Orientation.class, ParentFile.class, Pattern.class,
            Peaks.class, Plate.class, PrecursorMz.class, Robot.class, Scan.class, ScanOrigin.class,
            Separation.class, SeparationTechnique.class, Software.class, Spot.class, Spotting.class,
    };

    private void prepare() {
        airXStream.processAnnotations(classes);
        airXStream.allowTypes(classes);
    }

    /**
     * 为了加快解析，采取两个循环：解析索引区循环，然后根据索引找到数据区，对数据区一次循环获取属性，而不是分成很多次循环
     * @param file
     * @param experimentId
     * @return List<ScanIndexDO>
     */
    @Override
    public List<ScanIndexDO> index(File file, String experimentId,Float overlap, TaskDO taskDO) {
        RandomAccessFile raf = null;
        List<ScanIndexDO> list = new ArrayList<>();
        try {
            raf = new RandomAccessFile(file, "r");
            parseOffsetLists(raf, list); // 循环一次，有多少offset就有多少list元素，每个元素暂时填进去start和num
            parseAttributes(raf, experimentId,overlap, list);
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

    private void parseOffsetLists(RandomAccessFile rf, List<ScanIndexDO> list) throws IOException {
        // 首先获取索引的起始位置
        Long indexOffset = parseIndexOffset(rf);
        // 然后获取索引list
        rf.seek(indexOffset);
        int indexSize = (int) (rf.length() - 1 - indexOffset); // 可能包含chromatogram部分
        byte[] indexArray = new byte[indexSize];
        rf.read(indexArray);
        String totalLine = new String(indexArray);
        String[] indexLines = totalLine.split("\n");
        int count = 1;
        for (String line : indexLines) {
            if (line.contains("<offset")) {
                line = line.trim();
//                String idRef = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""));
                String offset = line.substring(line.indexOf(">") + 1, line.lastIndexOf("<"));
                list.add(new ScanIndexDO(count-1, Long.valueOf(offset.trim()), null)); // zero based
                count++;
            } else if (line.contains("</index>")) {// 表示spectrum部分索引已经结束，不再处理后续chromatogram部分
                return;
            }
        }
    }

    // 一个循环，解决每个数据块的end，experiment id, parentNum, precurserMz, precurserMzStrart, precurserMzEnd, precurserWindow, ms level, rt,
    private void parseAttributes(RandomAccessFile rf, String experimentId,Float overlap, List<ScanIndexDO> list) throws IOException {
        int len = list.size();
        int level = 0, parentNum = 0;
        Float rt, precursorMz, precursorMzStart, precursorMzEnd, offset = 0F;
        String line, dataBlock;
        String[] datas;
        Long pos;
        byte words[] = new byte[4000]; // 4000个能够保证读满说明字段
        long start = System.currentTimeMillis();
        for (int i = 0; i < len; i++) { // 倒数第一个数据块稍后处理
            ScanIndexDO scanIndexDO = list.get(i);
            // 设置end
            if (i != len - 1)
                scanIndexDO.setEnd(list.get(i+1).getStart() - 1);
            // 设置experiment id
            scanIndexDO.setExperimentId(experimentId);
            // ms_level and parentNum, precursorMzStart, preceursorMzEnd,
            rf.seek(scanIndexDO.getStart());
            rf.read(words);
            dataBlock = String.valueOf(words);
            dataBlock = words.toString();
            dataBlock = new String(words);
            datas = dataBlock.split("\n");
            for (int j = 0; j < datas.length; j++) {
                line = datas[j]; // 读后文件指针调到下一行行首
                if (line.contains("ms level")) {
                    level = searchValue(line).intValue();
                    if (level == 1)
                        parentNum = scanIndexDO.getNum();
                    else if (level == 2 && parentNum != 0)
                        scanIndexDO.setParentNum(parentNum);
                    scanIndexDO.setMsLevel(level);
                } else if (line.contains("scan start time") && line.contains("minute")) {
                    rt = searchValue(line) * 60.0F;
                    scanIndexDO.setRt(rt);
                    scanIndexDO.setRtStr(String.format("%.3f", rt));
                    if (level == 1)
                        break;
                } else if (line.contains("scan start time") && line.contains("second")) {
                    rt = searchValue(line);
                    scanIndexDO.setRt(rt);
                    scanIndexDO.setRtStr(String.format("%.3f", rt));
                } else if (level == 2 && line.contains("selected ion m/z")) {
                    precursorMz = searchValue(line);
                    scanIndexDO.setPrecursorMz(precursorMz);
                    precursorMzStart = scanIndexDO.getPrecursorMz() - offset;
                    precursorMzEnd = scanIndexDO.getPrecursorMz() + offset;
                    scanIndexDO.setPrecursorMzStart(precursorMzStart);
                    scanIndexDO.setPrecursorMzEnd(precursorMzEnd);
                    scanIndexDO.setWindowWideness(2*offset);
                    break;
                } else if (level == 2 && line.contains("isolation window lower offset")) {
                    offset = searchValue(line);
                    if(overlap != null){
                        offset = offset - overlap/2;
                    }
                }
            }
            if (i % 10000 == 0 && i != 0) {
                logger.info(String.format("已扫描索引: %.2f%%，平均每个光谱用时：%.2f ms", (double)i/len*100, (double)(System.currentTimeMillis() - start)/i));
            }
        }
        logger.info(String.format("已扫描索引: %.2f%%，平均每个光谱用时：%.2f ms", (double)len/len*100, (double)(System.currentTimeMillis() - start)/len));
        // 结尾情况特殊处理：由于存在spetrum后续是chromatogram情况，前向搜索spetrum的结束位置
        pos = list.get(len-1).getStart();
        rf.seek(pos);
        for (int i = 0; i < 100; i++) {
            line = rf.readLine();
            if (line != null && line.contains("</spectrum>")) { // readline得到"</>"之后已经调到下一行行首了
                Long end = rf.getFilePointer();
                list.get(len-1).setEnd(end);
                return;
            }
        }
    }


    private Float searchValue(String line) {
        Float result;
        String[] lines = line.trim().split(" ");
        for(String str : lines) {
            if (str.contains("value")) {
                result = Float.valueOf(str.substring(str.lastIndexOf("=") + 2, str.lastIndexOf("\"")));
                return result;
            }
        }
        return 0F;
    }

    private Long parseIndexOffset(RandomAccessFile rf) throws IOException {
        long position = rf.length() - 2; // 注意，read完之后，指针指向后一个字节，此时如果readline是下一行
        rf.seek(position);
        byte words[] = new byte[1];
        for (int i = 0; i < 1000; i++) {
            rf.read(words);
            if (words[0] == '\n') {
                String line = rf.readLine(); // readline从当前指针一直读到下一个\n
                if (line != null && line.contains("<indexListOffset>")) {
                    line = line.trim().replace("<indexListOffset>", "").replace("</indexListOffset>", "");
                    return Long.valueOf(line);
                }
            }
            rf.seek(--position);
        }
        return 0L;
    }

    @Override
    public MzIntensityPairs parseOne(RandomAccessFile raf, long start, long end) {
        try {
            boolean isMzBinary = false;
            boolean isIntensityBinary = false;
            String mzPrecision = null, intensityPrecision = null;
            String mzCompressionType = null, intensityCompressionType = null;
            String mzBinary = null, intensityBinary = null;
            raf.seek(start);
            byte[] reader = new byte[(int) (end - start)];
            raf.read(reader);
            String dataBlock = new String(reader);
            String binaryBlock = dataBlock.substring(dataBlock.indexOf("<binaryDataArrayList"), dataBlock.indexOf("</binaryDataArrayList"));
            String[] lines = binaryBlock.split("\n");
            for (String line : lines) {
                if (line.contains("64-bit float") && mzPrecision == null) {
                    mzPrecision = "64";
                } else if (line.contains("64-bit float")) {
                    intensityPrecision = "64";
                }
                if (line.contains("zlib") && mzCompressionType == null) {
                    mzCompressionType = "zlib";
                } else if (line.contains("zlib")) {
                    intensityCompressionType = "zlib";
                }
                if (line.contains("<binary>") && mzBinary == null) {
                    mzBinary = line.substring(line.indexOf(">")+1, line.indexOf("</binary>"));
                } else if (line.contains("<binary>")) {
                    intensityBinary = line.substring(line.indexOf(">")+1, line.indexOf("</binary>"));
                }
            }
            MzIntensityPairs pairs = getPeakMap(new Base64().decode(mzBinary),
                    new Base64().decode(intensityBinary),
                    Integer.parseInt(mzPrecision),
                    Integer.parseInt(intensityPrecision),
                    "zlib".equalsIgnoreCase(intensityCompressionType));
            return pairs;
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    //不需要实现!!!
    @Override
    public MzIntensityPairs getPeakMap(byte[] value, int precision, boolean isZlibCompression) {
        return null;
    }

    @Override
    public MzIntensityPairs getPeakMap(byte[] mz, byte[] intensity, int mzPrecision, int intensityPrecision, boolean isZlibCompression) {
        Float[] mzArray = getValues(mz, mzPrecision, isZlibCompression, ByteOrder.LITTLE_ENDIAN);
        Float[] intensityArray = getValues(intensity, intensityPrecision, isZlibCompression, ByteOrder.LITTLE_ENDIAN);

        if (mzArray == null || intensityArray == null || mzArray.length != intensityArray.length) {
            return null;
        }

        return new MzIntensityPairs(mzArray, intensityArray);
    }
}

