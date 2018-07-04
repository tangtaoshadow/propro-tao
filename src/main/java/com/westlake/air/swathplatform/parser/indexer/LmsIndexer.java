package com.westlake.air.swathplatform.parser.indexer;

import com.westlake.air.swathplatform.parser.model.mzxml.ScanIndex;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
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
     *
     * @param file
     * @return
     * @throws IOException
     */
    @Override
    public HashMap<Integer, ScanIndex> index(File file) throws IOException {

        RandomAccessFile raf = null;
        raf = new RandomAccessFile(file, "r");

        //获取索引的起始位置
        Long indexOffset = parseIndexOffset(raf);

        //根据索引的内容获取id以及对应的startPosition
        HashMap<Integer, ScanIndex> indexMap = parseScanStartPosition(indexOffset, raf);

        //记录总共的scan条数
        int totalCount = indexMap.size();

        Stack<ScanIndex> indexStack = new Stack<>();
        //对所有的记录开始做for循环
        for (int i = 1; i <= totalCount; i++) {

            if (i == totalCount) {
                byte[] lastRead = new byte[50];
                raf.seek(indexOffset-lastRead.length);
                raf.read(lastRead);
                if(!indexStack.isEmpty()){
                    ScanIndex lastIndex = indexStack.pop();
                    lastIndex.setEnd(indexOffset - 1);

                    Long length = findForLength(lastRead,2,62);
                    lastIndex.setEnd(indexOffset - length);

                    Long length2 = findForLength(lastRead,3,62);
                    indexMap.get(i).setEnd(indexOffset-length2);
                    break;
                }else{
                    Long length2 = findForLength(lastRead,2,62);
                    indexMap.get(i).setEnd(indexOffset-length2);
                    break;
                }


            }

            byte[] tailBytes = new byte[TAIL_TRY];
            ScanIndex index = indexMap.get(i);
            ScanIndex nextIndex = indexMap.get(i + 1);
            raf.seek(nextIndex.getStart() - TAIL_TRY);
            raf.read(tailBytes);
            String tail = new String(tailBytes);
            if (!indexStack.isEmpty()) {
                //如果包含了两个</scan> 说明已经跳出了父节点到了下一个父节点
                if (StringUtils.countMatches(tail, "</scan>") == 2) {
                    //如果检测到两个</scan>,那么认为是跳出了MS2了,开始为MS1的Scan做索引
                    ScanIndex lastIndex = indexStack.pop();
                    lastIndex.setEnd(nextIndex.getStart() - 1);
                    //再为MS2的Scan做索引,MS2的索引为nextIndex索引一步一步向前搜索,直到搜索到第2个">",我们先往前读30个字节
                    //因为">"的byte编码是62,所以要搜索倒数第二个62所处的位置
                    Long length = findForLength(tailBytes, 2, 62);
                    index.setEnd(nextIndex.getStart() - length);
                    continue;
                }

                //父节点下的兄弟节点
                if (tail.contains("</scan>")) {
                    index.setEnd(nextIndex.getStart() - 1);
                    continue;
                }
            }

            if (tail.contains("</scan>")) {
                index.setEnd(nextIndex.getStart() - 1);
                continue;
            }

            if (!tail.contains("</scan>")) {
                indexStack.push(index);
                continue;
            }

        }
        return indexMap;
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

    Long findForLength(byte[] strToFind, int findTime, int findStr) {
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
}
