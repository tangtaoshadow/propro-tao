package com.westlake.air.pecs.test.parser;

import com.westlake.air.pecs.parser.MzXMLParser;
import com.westlake.air.pecs.test.BaseTest;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.util.ArrayList;

public class MzXMLParserTest extends BaseTest {

    @Autowired
    MzXMLParser mzXMLParser;

    ArrayList<String> filePaths = new ArrayList<>();

    public void init() {
        logger.info("Start init params");
        filePaths.add(FilenameUtils.normalize("D:\\data\\SGS\\mzxml\\napedro_L120224_001_Water.mzXML"));
        filePaths.add(FilenameUtils.normalize("D:\\data\\SGS\\mzxml\\napedro_L120224_002_Water.mzXML"));
        filePaths.add(FilenameUtils.normalize("D:\\data\\SGS\\mzxml\\napedro_L120224_003_Water.mzXML"));
        filePaths.add(FilenameUtils.normalize("D:\\data\\SGS\\mzxml\\napedro_L120224_004_Water.mzXML"));
        filePaths.add(FilenameUtils.normalize("D:\\data\\SGS\\mzxml\\napedro_L120224_005_Water.mzXML"));
        filePaths.add(FilenameUtils.normalize("D:\\data\\SGS\\mzxml\\napedro_L120224_006_Water.mzXML"));
        filePaths.add(FilenameUtils.normalize("D:\\data\\SGS\\mzxml\\napedro_L120224_007_Water.mzXML"));
        filePaths.add(FilenameUtils.normalize("D:\\data\\SGS\\mzxml\\napedro_L120224_008_Water.mzXML"));
        filePaths.add(FilenameUtils.normalize("D:\\data\\SGS\\mzxml\\napedro_L120224_009_Water.mzXML"));
        filePaths.add(FilenameUtils.normalize("D:\\data\\SGS\\mzxml\\napedro_L120224_010_Water.mzXML"));
    }

    @Test
    public void testParseIndex() {
        long start = System.currentTimeMillis();
        init();
        for (String path : filePaths) {
            File file = new File(path);

            try {
                Long result = mzXMLParser.parseIndexOffset(file);
                logger.info(result+"");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        logger.info("Cost:" + (System.currentTimeMillis() - start));

    }
}
