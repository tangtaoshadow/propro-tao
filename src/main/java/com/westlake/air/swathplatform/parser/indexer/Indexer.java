package com.westlake.air.swathplatform.parser.indexer;

import com.westlake.air.swathplatform.parser.model.mzxml.ScanIndex;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-04 20:29
 */
public interface Indexer {

    List<ScanIndex> index(File file) throws IOException;
}
