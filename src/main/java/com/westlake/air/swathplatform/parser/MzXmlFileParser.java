package com.westlake.air.swathplatform.parser;

import com.westlake.air.swathplatform.parser.model.mzxml.*;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MzXmlFileParser {

    private File srcFile;
    private Long indexOffset;
    private RandomAccessFile raf = null;
    private String FILE_ENCODING = "ISO-8859-1";
    private HashMap<Integer, Long> indexMap = new HashMap<>();
    private static int READ_COUNT_PER_TIME = 1;
    private static final Pattern attrPattern = Pattern.compile("(\\w+)=\"([^\"]*)\"");

    private static byte[] prefix = "<scanList>".getBytes();
    private static byte[] suffix = "</scanList>".getBytes();

    public final Logger logger = LoggerFactory.getLogger(MzXmlFileParser.class);

    JAXBContext jc = null;
    private Unmarshaller unmarshaller = null;

    public MzXmlFileParser(File file) throws Exception {
        super();
        this.srcFile = file;

        try {
            if (jc == null) {
                jc = JAXBContext.newInstance(ModelConstants.MODEL_SCAN_PACKAGE);
                unmarshaller = jc.createUnmarshaller();
            }
        } catch (JAXBException e) {
            throw new IllegalStateException("Could not initialize unmarshaller", e);
        }

        try {
            parseFileEncode();
            System.out.println("parse file encode finished,file encode:" + FILE_ENCODING);
            parseIndexOffset();
            System.out.println("parse scan index offset finished,indexOffset:" + indexOffset);
            parseScanIndex();
            System.out.println("parse scan index finished,indeMap size:" + indexMap.size());
            buildScanContent();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeRandomAccessFile();
        }

    }

    public synchronized <T extends MzXMLObject> T parse(byte[] content, MzXmlElement element) throws Exception {
        T object;
        try {

            if (content == null || element == null) {
                return null;
            }
            final SAXParserFactory sax = SAXParserFactory.newInstance();
            sax.setNamespaceAware(false);
            final XMLReader xmlReader = sax.newSAXParser().getXMLReader();
            @SuppressWarnings("unchecked")
            JAXBElement<T> holder = unmarshaller.unmarshal(new SAXSource(xmlReader, new InputSource(new ByteArrayInputStream(content))), element.getClassType());
            object = holder.getValue();
        } catch (JAXBException e) {
            System.out.println(new String(content));
            throw new Exception("Error unmarshalling object: " + e.getMessage(), e);
        }

        return object;
    }

    public void buildScanContent() throws Exception {
        int size = indexMap.size();
        int startPointer = 1;
        int endPointer = (READ_COUNT_PER_TIME + 1) > size ? size : (READ_COUNT_PER_TIME + 1);

        File file = new File("D:\\testJson");
        if(!file.exists()) {
            file.createNewFile();
        }

        RandomAccessFile wf = new RandomAccessFile(file,"rw");

        boolean lastLoop = false;
        while (endPointer <= size) {

            ResultPair rp = getScanBuffer(startPointer, endPointer);

            //if the last line is not "</scan>",that means this line is the inner scan tag
            //check the tail of the buffer string
            boolean needChildCheck = false;
            while (!checkScanCloseTag(rp.getTail(), needChildCheck) && endPointer < size) {
                startPointer = endPointer;
                endPointer++;
                ResultPair tmp = getScanBuffer(startPointer, endPointer);
                rp.body = ArrayUtils.addAll(rp.body, tmp.body);
                rp.tail = tmp.tail;
                needChildCheck = true;
            }
            final byte[] joinedArray = new byte[prefix.length + rp.body.length + suffix.length];
            System.arraycopy(prefix, 0, joinedArray, 0, prefix.length);
            System.arraycopy(rp.body, 0, joinedArray, prefix.length, rp.body.length);
            System.arraycopy(suffix, 0, joinedArray, prefix.length + rp.body.length, suffix.length);
            ScanList scanList = parse(joinedArray, MzXmlElement.SIMPLE_SCAN);

            List<Scan> scanArray = scanList.getScan();

            System.out.println("Finished/Total:" + endPointer + "/" + size);
            startPointer = endPointer;
            if (lastLoop) {
                break;
            }
            if (endPointer >= size) {
                break;
            }
            if ((endPointer + READ_COUNT_PER_TIME) >= size) {
                endPointer = size;
                lastLoop = true;
            } else {
                endPointer += READ_COUNT_PER_TIME;
            }
        }

        wf.close();
    }

    public void parseScanIndex() throws IOException {
        RandomAccessFile rf = getRandomAccessFile();
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
                indexMap.put(Integer.valueOf(id.trim()), Long.valueOf(offset.trim()));
            }
        }
    }

    public void parseIndexOffset() throws IOException {
        RandomAccessFile rf = getRandomAccessFile();
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
                    setIndexOffset(Long.valueOf(line));
                    return;
                }
            }

            rf.seek(--position);
        }
    }

    private RandomAccessFile getRandomAccessFile() throws IOException {
        if (raf == null) {
            raf = new RandomAccessFile(srcFile, "r");
        }
        return raf;
    }

    private void closeRandomAccessFile() {
        if (raf != null) {
            try {
                raf.close();
                raf = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void parseFileEncode() throws IOException {
        RandomAccessFile rf = getRandomAccessFile();
        rf.seek(0);
        String firstLine = rf.readLine();
        if (firstLine == null || !firstLine.contains("<?xml")) {
            FILE_ENCODING = "ISO-8859-1";
            return;
        }

        Matcher matcher = attrPattern.matcher(firstLine);
        while (matcher.find()) {
            if (matcher.group(1).equals("encoding")) {
                FILE_ENCODING = matcher.group(2);
            }
        }
    }

    private String getLastLine(Long position) throws IOException {
        raf = getRandomAccessFile();
        long len = raf.length();   //文件长度
        long nextend = len - 1;
        int c = -1;
        String line = "";
        boolean b = false;
        while (nextend > 0) {
            raf.seek(nextend);
            c = raf.read();
            if (c == '\n') {
                if (!b) {
                    b = true;
                } else {
                    line = new String(raf.readLine().getBytes("ISO-8859-1"), "gbk");
                    break;
                }
            }
            nextend--;
        }
        raf.close();
        return line;
    }

    private ResultPair getScanBuffer(int startPoint, int endPoint) throws IOException {

        RandomAccessFile rf = getRandomAccessFile();

        long endBufferPoint;
        boolean isLastScan = false;
        if (endPoint >= indexMap.size()) {
            endBufferPoint = indexOffset;
            isLastScan = true;
        } else {
            endBufferPoint = indexMap.get(endPoint);
        }
        int readSize = (int) (endBufferPoint - indexMap.get(startPoint));
        if (readSize <= 0) {
            throw new IOException("indexOffset must be error!");
        }
        rf.seek(indexMap.get(startPoint));
        byte[] readBuffer = new byte[readSize];
        rf.read(readBuffer);

        String tailStr = null;
        if (readSize > 100) {
            tailStr = new String(ArrayUtils.subarray(readBuffer, readSize - 100, readSize));
        } else {
            tailStr = new String(readBuffer);
        }

        if (isLastScan) {
            tailStr = new String(tailStr.replace("</msRun>", ""));
            readBuffer = (new String(readBuffer).replace("</msRun>", "")).getBytes();
        }
        ResultPair rp = new ResultPair(readBuffer, new String(tailStr.trim()));
        return rp;
    }

    private boolean checkScanCloseTag(String checkStr, boolean needChildCheck) {

        //String "</scan>" length is 7,check if the last Pointer is pointed to the father Scan Tag,not the child
        checkStr = checkStr.trim();
        String tmp = new String(checkStr.substring(checkStr.length() - 7, checkStr.length()));

        if (!tmp.endsWith("</scan>")) {
            return false;
        }

        if (needChildCheck) {
            checkStr = checkStr.substring(0, checkStr.length() - 7).trim();
            String tmp2 = new String(checkStr.substring(checkStr.length() - 7, checkStr.length()));
            if (!tmp2.endsWith("</scan>")) {
                return false;
            }
        }

        return true;
    }

    public Long getIndexOffset() {
        return indexOffset;
    }

    public void setIndexOffset(Long indexOffset) {
        this.indexOffset = indexOffset;
    }

    public class ResultPair {

        public ResultPair(byte[] body, String tail) {
            this.body = body;
            this.tail = tail;
        }

        byte[] body;

        String tail;

        public byte[] getBody() {
            return body;
        }

        public void setBody(byte[] body) {
            this.body = body;
        }

        public String getTail() {
            return tail;
        }

        public void setTail(String tail) {
            this.tail = tail;
        }

    }
}
