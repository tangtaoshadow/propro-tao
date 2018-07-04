package com.westlake.air.swathplatform.parser;

import com.westlake.air.swathplatform.parser.indexer.Indexer;
import com.westlake.air.swathplatform.parser.model.mzxml.*;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class MzXmlParser {


    private String FILE_ENCODING = "ISO-8859-1";
    private static int READ_COUNT_PER_TIME = 1;
    private static final Pattern attrPattern = Pattern.compile("(\\w+)=\"([^\"]*)\"");

    private static byte[] prefix = "<scanList>".getBytes();
    private static byte[] suffix = "</scanList>".getBytes();

    public final Logger logger = LoggerFactory.getLogger(MzXmlParser.class);

    public MzXmlParser() throws Exception {}

    public void parse(File file, Indexer iIndexer) throws Exception {

        long startTime = System.currentTimeMillis();
        HashMap<Integer,ScanIndex> indexMap = iIndexer.index(file);
        System.out.println(System.currentTimeMillis() - startTime + "");
        System.out.println("");
    }

    public void parse(File file) throws Exception {

        JAXBContext jc = null;
        Unmarshaller unmarshaller = null;
        try {
            if (jc == null) {
                jc = JAXBContext.newInstance(ModelConstants.MODEL_SCAN_PACKAGE);
                unmarshaller = jc.createUnmarshaller();
            }
        } catch (JAXBException e) {
            throw new IllegalStateException("Could not initialize unmarshaller", e);
        }

        Long indexOffset;
        HashMap<Integer, Long> indexMap = null;
        RandomAccessFile raf = new RandomAccessFile(file, "r");
//        try {
//
//            buildScanContent(indexOffset, indexMap, raf, unmarshaller);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            closeRandomAccessFile(raf);
//        }
    }

    public synchronized <T extends MzXMLObject> T parseXml(byte[] content, MzXmlElement element, Unmarshaller unmarshaller) throws Exception {
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
            logger.error(new String(content));
            throw new Exception("Error unmarshalling object: " + e.getMessage(), e);
        }

        return object;
    }

    public void buildScanContent(Long indexOffset, HashMap<Integer, Long> indexMap, RandomAccessFile raf, Unmarshaller unmarshaller) throws Exception {
        int size = indexMap.size();
        int startPointer = 1;
        int endPointer = (READ_COUNT_PER_TIME + 1) > size ? size : (READ_COUNT_PER_TIME + 1);

        boolean lastLoop = false;
        while (endPointer <= size) {

            ResultPair rp = getScanBuffer(startPointer, endPointer, indexOffset, indexMap, raf);

            //if the last line is not "</scan>",that means this line is the inner scan tag
            //check the tail of the buffer string
            boolean needChildCheck = false;
            while (!checkScanCloseTag(rp.getTail(), needChildCheck) && endPointer < size) {
                startPointer = endPointer;
                endPointer++;
                ResultPair tmp = getScanBuffer(startPointer, endPointer, indexOffset, indexMap, raf);
                rp.body = ArrayUtils.addAll(rp.body, tmp.body);
                rp.tail = tmp.tail;
                needChildCheck = true;
            }
            final byte[] joinedArray = new byte[prefix.length + rp.body.length + suffix.length];
            System.arraycopy(prefix, 0, joinedArray, 0, prefix.length);
            System.arraycopy(rp.body, 0, joinedArray, prefix.length, rp.body.length);
            System.arraycopy(suffix, 0, joinedArray, prefix.length + rp.body.length, suffix.length);
            ScanList scanList = parseXml(joinedArray, MzXmlElement.SIMPLE_SCAN, unmarshaller);

            List<Scan> scanArray = scanList.getScan();

            logger.info("Finished/Total:" + endPointer + "/" + size);
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
    }





    private void closeRandomAccessFile(RandomAccessFile raf) {
        if (raf != null) {
            try {
                raf.close();
                raf = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

//    private void parseFileEncode(RandomAccessFile rf) throws IOException {
//        rf.seek(0);
//        String firstLine = rf.readLine();
//        if (firstLine == null || !firstLine.contains("<?xml")) {
//            FILE_ENCODING = "ISO-8859-1";
//            return;
//        }
//
//        Matcher matcher = attrPattern.matcher(firstLine);
//        while (matcher.find()) {
//            if (matcher.group(1).equals("encoding")) {
//                FILE_ENCODING = matcher.group(2);
//            }
//        }
//    }

    private String getLastLine(RandomAccessFile raf, Long position) throws IOException {
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

    private ResultPair getScanBuffer(int startPoint, int endPoint, Long indexOffset, HashMap<Integer, Long> indexMap, RandomAccessFile rf) throws IOException {

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
