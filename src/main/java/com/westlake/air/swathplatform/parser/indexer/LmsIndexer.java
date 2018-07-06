package com.westlake.air.swathplatform.parser.indexer;

import com.westlake.air.swathplatform.parser.model.mzxml.ScanIndex;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

/**
 * ">"的byte编码是62
 * <p>
 * Created by James Lu MiaoShan
 * Time: 2018-07-04 20:29
 */
public class LmsIndexer implements Indexer {

    public static int TAIL_TRY = 30;

    /**
     * 本算法得到的ScanIndex的End包含了换行符
     * ">"的byte编码是62
     * @param file
     * @return
     * @throws IOException
     */
    @Override
    public List<ScanIndex> index(File file) throws IOException {

        RandomAccessFile raf = null;
        raf = new RandomAccessFile(file, "r");

        //获取索引的起始位置
        Long indexOffset = parseIndexOffset(raf);

        //根据索引的内容获取id以及对应的startPosition
        HashMap<Integer, ScanIndex> indexMap = parseScanStartPosition(indexOffset, raf);
        List<ScanIndex> indexList = new ArrayList<>();

        //记录总共的scan条数
        int totalCount = indexMap.size();

        Stack<ScanIndex> indexStack = new Stack<>();
        byte[] lastRead;
        //对所有的记录开始做for循环
        for (int i = 1; i <= totalCount; i++) {

            //如果已经是最后一个元素了
            if (i == totalCount) {
                lastRead = read(raf, indexOffset-50, 50);
                //如果stack中不为空,说明这个节点是一个二级节点
                if(!indexStack.isEmpty()){
                    //先处理一级节点的位置,因为包含了</msRun>这个额外的标签,所以需要从indexOffset的位置往前搜索2个">"
                    ScanIndex lastIndex = indexStack.pop();
                    Long length = searchForLength(lastRead,2,62);
                    lastIndex.setEnd(indexOffset - length);

                    //再处理二级节点,二级节点则是从indexOffset往前处理3个">"
                    Long length2 = searchForLength(lastRead,3,62);
                    indexMap.get(i).setEnd(indexOffset-length2);

                    //二级节点处理完毕以后将二级节点add到对应的一级节点中
                    lastIndex.add(indexMap.get(i));

                    //将一级节点存储List中
                    indexList.add(lastIndex);
                    break;
                }else{
                    //如果是空说明最后一个节点是一级节点,直接找倒数第二个>即可定位
                    Long length = searchForLength(lastRead,2,62);
                    indexMap.get(i).setEnd(indexOffset-length);
                    break;
                }
            }

            ScanIndex index = indexMap.get(i);
            ScanIndex nextIndex = indexMap.get(i + 1);
            byte[] tailBytes = read(raf, nextIndex.getStart() - TAIL_TRY, TAIL_TRY);

            String tail = new String(tailBytes);
            //如果stack中不为空,说明这个节点是一个二级节点
            if (!indexStack.isEmpty()) {
                //如果包含了两个</scan> 说明已经跳出了一级节点到了下一个一级节点
                if (StringUtils.countMatches(tail, "</scan>") == 2) {
                    //如果检测到两个</scan>,那么认为是跳出了二级节点了,开始为上一个一级节点做索引
                    ScanIndex lastIndex = indexStack.pop();
                    lastIndex.setEnd(nextIndex.getStart() - 1);

                    //再为二级节点做索引,二级节点的索引为nextIndex索引一步一步向前搜索,直到搜索到第2个">",我们先往前读30个字节
                    //因为">"的byte编码是62,所以要搜索倒数第二个62所处的位置
                    Long length = searchForLength(tailBytes, 2, 62);
                    index.setEnd(nextIndex.getStart() - length);

                    //将二级节点add到一级节点中
                    lastIndex.add(index);

                    indexList.add(lastIndex);
                    continue;
                }

                //一级节点下的兄弟节点,还未跳出一级节点
                if (tail.contains("</scan>")) {
                    index.setEnd(nextIndex.getStart() - 1);
                    //操作堆栈中的一级节点,但是不要pop出来
                    indexStack.peek().add(index);
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
        return indexList;
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

    public HashMap<Integer, ScanIndex> parseScanStartPosition(Long indexOffset, RandomAccessFile rf) throws IOException {
        HashMap<Integer, ScanIndex> indexMap = new HashMap<>();
        rf.seek(indexOffset);
        int indexSize = (int) (rf.length() - 1 - indexOffset);
        byte[] indexArray = new byte[indexSize];
        rf.read(indexArray);
        String totalLine = new String(indexArray);
        String[] indexLines = totalLine.split("\n");
        for (String line : indexLines) {
            if (line.contains("<offset")) {
                line = line.trim();
                String id = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""));
                String offset = line.substring(line.indexOf(">") + 1, line.lastIndexOf("<"));
                indexMap.put(Integer.valueOf(id.trim()), new ScanIndex(Integer.valueOf(id.trim()), Long.valueOf(offset.trim()), null));
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
}
