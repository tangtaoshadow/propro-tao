package com.westlake.air.swathplatform.parser.indexer;

import com.westlake.air.swathplatform.parser.model.mzxml.Scan;
import com.westlake.air.swathplatform.parser.model.mzxml.ScanIndex;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import psidev.psi.tools.xxindex.StandardXpathAccess;
import psidev.psi.tools.xxindex.index.IndexElement;
import psidev.psi.tools.xxindex.index.XpathIndex;
import uk.ac.ebi.pride.tools.mzxml_parser.MzXMLFile;
import uk.ac.ebi.pride.tools.mzxml_parser.MzXMLParsingException;
import uk.ac.ebi.pride.tools.mzxml_parser.mzxml.model.MzXmlElement;

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
@Component("prideIndexer")
public class PrideIndexer implements Indexer {

    @Override
    public List<ScanIndex> index(File file) throws IOException {

       // build the xpath
       StandardXpathAccess xpathAccess = new StandardXpathAccess(file, MzXmlElement.getXpaths());
       // save the index
       XpathIndex index = xpathAccess.getIndex();
       // save the scan indexes
       List<ScanIndex> scanIndexList = new ArrayList<>();
       List<IndexElement> scanList1 = index.getElements(MzXmlElement.SCAN_LEVEL1.getXpath());
       List<IndexElement> scanList2 = index.getElements(MzXmlElement.SCAN_LEVEL2.getXpath());
       scanList1.addAll(scanList2);
       for(IndexElement indexElement : scanList1){
           ScanIndex scanIndex = new ScanIndex(indexElement.getStart(),indexElement.getStop());
           scanIndexList.add(scanIndex);
       }

       return scanIndexList;
    }



}
