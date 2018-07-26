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

    @Override
    public List<ScanIndexDO> index(File file, String experimentId) {
        RandomAccessFile raf = null;
        List<ScanIndexDO> list = null;
        try {
            raf = new RandomAccessFile(file, "r");
            list = index(file);
            int count = 0;
            for (ScanIndexDO scanIndex : list) {
                parseAttribute(raf, scanIndex);
                scanIndex.setExperimentId(experimentId);
                System.out.println(++count);
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

    private List<ScanIndexDO> index(File file) {

        List<ScanIndexDO> indexList = new ArrayList<>();
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");

            //获取索引的起始位置
            Long indexOffset = parseIndexOffset(raf);

            // 根据索引的内容获取id以及对应的startPosition
            HashMap<Integer, ScanIndexDO> indexMap = parseScanStartPosition(indexOffset, raf);

            // 根据检索的内容将第二个数据块的起始-1作为当前数据块的end
            parseScanEndPosition(indexMap, raf, indexOffset);

            //对所有的记录开始做for循环
            // 由于没有scan嵌套，简化解析
            for (int i = 1; i <= indexMap.size(); i++) {
                indexList.add(indexMap.get(i));
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


    private void parseScanEndPosition(HashMap<Integer, ScanIndexDO> map, RandomAccessFile rf, Long indexOffset) throws IOException {
        int len = map.size();
        String line;
        Long pos;
        byte ch[] = new byte[1];
        for (int i = 1; i < len; i++) {
            map.get(i).setEnd(map.get(i+1).getStart()-1);
        }
        // 结尾情况特殊处理：由于存在spetrum后续是chromatogram情况，前向搜索spetrum的结束位置
        pos = map.get(len).getStart();
        rf.seek(pos);
        for (int i = 0; i < 100; i++) {
            line = rf.readLine();
            if (line != null && line.contains("</spectrum>")) { // readline得到"</>"之后已经调到下一行行首了
                Long end = rf.getFilePointer();
                map.get(len).setEnd(end);
                return;
            }
        }
    }


    private HashMap<Integer, ScanIndexDO> parseScanStartPosition(Long indexOffset, RandomAccessFile rf) throws IOException {
        HashMap<Integer, ScanIndexDO> indexMap = new HashMap<>();
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
                String idRef = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""));
                String offset = line.substring(line.indexOf(">") + 1, line.lastIndexOf("<"));
                indexMap.put(count, new ScanIndexDO(Long.valueOf(offset.trim()), null));
                count++;
            }
            if (line.contains("</index>")) {// 表示spectrum部分索引已经结束，不再处理后续chromatogram部分
                return indexMap;
            }
        }
        return indexMap;
    }


    private byte[] read(RandomAccessFile raf, long start, int size) throws IOException {
        byte[] tmp = new byte[size];
        raf.seek(start);
        raf.read(tmp);

        return tmp;
    }

    //解析Scan标签的Attributes：暂时只获取ms_level和rt
    private void parseAttribute(RandomAccessFile raf, ScanIndexDO scanIndexDO) throws IOException {
        boolean isMsLevelFinish = false;
        boolean isRTFinish = false;
        raf.seek(scanIndexDO.getStart());
        while (raf.getFilePointer() < scanIndexDO.getEnd()) {
            String line = raf.readLine(); // 读后文件指针调到下一行行首
            if (line.contains("ms level")) {
                int level = Integer.parseInt(line.split("\"")[7]);
                scanIndexDO.setMsLevel(level);
                isMsLevelFinish = true;
            } else if (line.contains("scan start time") && line.contains("minute")) {
                Float rt = Float.valueOf(line.split("\"")[7]) * 60.0F;
                scanIndexDO.setRt(rt);
                isRTFinish = true;
            } else if (line.contains("scan start time") && line.contains("second")) {
                Float rt = Float.valueOf(line.split("\"")[7]);
                scanIndexDO.setRt(rt);
                isRTFinish = true;
            }
            if (isMsLevelFinish && isRTFinish)
                return;
        }
    }
}

