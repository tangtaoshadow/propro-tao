package com.westlake.air.swathplatform.parser;

import com.westlake.air.swathplatform.parser.indexer.Indexer;
import com.westlake.air.swathplatform.parser.model.mzxml.ScanIndex;
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

    }
}
