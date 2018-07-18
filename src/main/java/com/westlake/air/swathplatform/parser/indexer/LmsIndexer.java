package com.westlake.air.swathplatform.parser.indexer;

import com.westlake.air.swathplatform.domain.db.ScanIndexDO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;

/**
 * ">"的byte编码是62
 * <p>
 * Created by James Lu MiaoShan
 * Time: 2018-07-04 20:29
 */
@Component("lmsIndexer")
public class LmsIndexer {

    public static int TAIL_TRY = 30;

    private static final Pattern attrPattern = Pattern.compile("(\\w+)=\"([^\"]*)\"");

    public final Logger logger = LoggerFactory.getLogger(LmsIndexer.class);

    /**
     * 本算法得到的ScanIndex的End包含了换行符
     * ">"的byte编码是62
     *
     * @param file
     * @return
     * @throws IOException
     */
    public List<ScanIndexDO> index(File file) {

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

    public List<ScanIndexDO> index(File file, String experimentId) {
        RandomAccessFile raf = null;
        List<ScanIndexDO> list = null;
        try {
            raf = new RandomAccessFile(file, "r");
            list = index(file);
            for (ScanIndexDO scanIndex : list) {
                parseAttribute(raf, scanIndex);
                scanIndex.setExperimentId(experimentId);
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

    Long searchForLength(byte[] strToFind, int findTime, int findStr) {
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

    private byte[] read(RandomAccessFile raf, long start, int size) throws IOException {
        byte[] tmp = new byte[size];
        raf.seek(start);
        raf.read(tmp);

        return tmp;
    }

    //解析Scan标签的Attributes
    private void parseAttribute(RandomAccessFile raf, ScanIndexDO scanIndexDO) throws IOException {

        int focusAttributeCount = 2;
        byte[] readBytes = read(raf, scanIndexDO.getStart() + 1, 500);

        String read = new String(readBytes);
        read = read.substring(0, read.indexOf(">"));
        String[] tmp = null;
        if (read.contains("\n")) {
            tmp = read.split("\n");
        } else {//说明属性在一行里面
            tmp = read.split(" ");
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
