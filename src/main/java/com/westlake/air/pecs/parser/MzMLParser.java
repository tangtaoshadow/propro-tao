package com.westlake.air.pecs.parser;

import com.westlake.air.pecs.domain.bean.MzIntensityPairs;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Song Jian
 * Time: 2018-07-25 20-14
 */
@Component
public class MzMLParser extends BaseExpParser{

    public final Logger logger = LoggerFactory.getLogger(MzMLParser.class);

    /**
     * 为了加快解析，采取两个循环：解析索引区循环，然后根据索引找到数据区，对数据区一次循环获取属性，而不是分成很多次循环
     * @param file
     * @param experimentId
     * @return List<ScanIndexDO>
     */
    @Override
    public List<ScanIndexDO> index(File file, String experimentId) {
        RandomAccessFile raf = null;
        List<ScanIndexDO> list = new ArrayList<>();
        try {
            raf = new RandomAccessFile(file, "r");
            parseOffsetLists(raf, list); // 循环一次，有多少offset就有多少list元素，每个元素暂时填进去start和num
            parseAttributes(raf, experimentId, list);
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
    private void parseAttributes(RandomAccessFile rf, String experimentId, List<ScanIndexDO> list) throws IOException {
        int len = list.size();
        int level = 0, parentNum = 0;
        Float rt, precursorMz, precursorMzStart, precursorMzEnd, offset;
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
                    if (level == 1)
                        break;
                } else if (line.contains("scan start time") && line.contains("second")) {
                    rt = searchValue(line);
                    scanIndexDO.setRt(rt);
                } else if (level == 2 && line.contains("isolation window target m/z")) {
                    precursorMz = searchValue(line);
                    scanIndexDO.setPrecursorMz(precursorMz);
                } else if (level == 2 && line.contains("isolation window lower offset")) {
                    offset = searchValue(line);
                    precursorMzStart = scanIndexDO.getPrecursorMz() - offset;
                    precursorMzEnd = scanIndexDO.getPrecursorMz() + offset;
                    scanIndexDO.setPrecursorMzStart(precursorMzStart);
                    scanIndexDO.setPrecursorMzEnd(precursorMzEnd);
                    break;
                }
            }
            if (i % 10000 == 0) {
                logger.info(String.format("已扫描索引: %.2f%%，平均每个光谱用时：%.2f ms", (double)i/len*100, (double)(System.currentTimeMillis() - start)/i));
            }
        }
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
        return null;
    }

    //不需要实现!!!
    @Override
    public MzIntensityPairs getPeakMap(byte[] value, int precision, boolean isZlibCompression) {
        return null;
    }

    @Override
    public MzIntensityPairs getPeakMap(byte[] mz, byte[] intensity, int mzPrecision, int intensityPrecision, boolean isZlibCompression) {
        Float[] mzArray = getValues(mz, mzPrecision, isZlibCompression);
        Float[] intensityArray = getValues(intensity, intensityPrecision, isZlibCompression);

        if (mzArray == null || intensityArray == null || mzArray.length != intensityArray.length) {
            return null;
        }

        return new MzIntensityPairs(mzArray, intensityArray);
    }
}

