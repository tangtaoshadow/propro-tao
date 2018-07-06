package com.westlake.air.swathplatform.parser;

import com.westlake.air.swathplatform.parser.indexer.Indexer;
import com.westlake.air.swathplatform.parser.model.mzxml.*;
import com.westlake.air.swathplatform.parser.model.mzxml.Software;
import com.westlake.air.swathplatform.parser.model.traml.*;
import com.westlake.air.swathplatform.parser.xml.AirXStream;
import com.westlake.air.swathplatform.parser.xml.PeaksConverter;
import com.westlake.air.swathplatform.parser.xml.PrecursorMzConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.regex.Pattern;

@Component
public class MzXmlParser {

    @Autowired
    AirXStream airXStream;

    private static final Pattern attrPattern = Pattern.compile("(\\w+)=\"([^\"]*)\"");

    private static byte[] prefix = "<scanList>".getBytes();
    private static byte[] suffix = "</scanList>".getBytes();

    public final Logger logger = LoggerFactory.getLogger(MzXmlParser.class);

    public Class<?>[] classes = new Class[]{
            DataProcessing.class, Maldi.class, MsInstrument.class, MsRun.class, MzXML.class, NameValue.class,
            OntologyEntry.class, Operator.class, Orientation.class, ParentFile.class, Pattern.class,
            Peaks.class, Plate.class, PrecursorMz.class, Robot.class, Scan.class, ScanOrigin.class,
            Separation.class, SeparationTechnique.class, Software.class, Spot.class, Spotting.class,
    };

    public MzXmlParser() throws Exception {}

    private void prepare(){
        airXStream.processAnnotations(classes);
        airXStream.allowTypes(classes);
        airXStream.registerConverter(new PeaksConverter());
        airXStream.registerConverter(new PrecursorMzConverter());
    }

    public void parse(File file, Indexer iIndexer) throws Exception {
        prepare();

        HashMap<Integer,ScanIndex> indexMap = iIndexer.index(file);

    }
}
