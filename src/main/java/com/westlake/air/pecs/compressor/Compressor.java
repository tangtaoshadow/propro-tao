package com.westlake.air.pecs.compressor;

import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.pecs.domain.bean.analyse.WindowRang;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import com.westlake.air.pecs.domain.db.simple.SimpleScanIndex;
import com.westlake.air.pecs.domain.query.ScanIndexQuery;
import com.westlake.air.pecs.parser.MzXMLParser;
import com.westlake.air.pecs.service.ExperimentService;
import com.westlake.air.pecs.service.ScanIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.List;
import java.util.TreeMap;

@Component("compressor")
public class Compressor {

    public final Logger logger = LoggerFactory.getLogger(Compressor.class);

    public static final String SUFFIX_AIRUS_INFO = ".airusInfo";
    public static final String SUFFIX_AIRUS_DATA = ".airusData";

    @Autowired
    ScanIndexService scanIndexService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    MzXMLParser mzXMLParser;

    public ResultDO doCompress(ExperimentDO experimentDO) {
        String fileLocation = experimentDO.getFileLocation();
        File file = new File(fileLocation);
        String fileParent = file.getParent();
        String fileNameWithoutSuffix = file.getName().replace(".mzXML", "");
        String targetInfoFilePath = fileParent + "/" + fileNameWithoutSuffix + SUFFIX_AIRUS_INFO;
        String targetDataFilePath = fileParent + "/" + fileNameWithoutSuffix + SUFFIX_AIRUS_DATA;
        File targetInfoFile = new File(targetInfoFilePath);
        File targetDataFile = new File(targetDataFilePath);

        try{
            if(!targetInfoFile.exists()){
                targetInfoFile.createNewFile();
            }
            if(!targetDataFile.exists()){
                targetDataFile.createNewFile();
            }
            RandomAccessFile rafRead = new RandomAccessFile(file,"r");
            RandomAccessFile rafWriteData = new RandomAccessFile(targetDataFile, "wr");
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);

            List<WindowRang> windowRangs = experimentService.getWindows(experimentDO.getId());
            for(WindowRang rang : windowRangs){
                ScanIndexQuery query = new ScanIndexQuery(experimentDO.getId(), 2);
                query.setPrecursorMzStart(rang.getMzStart());
                query.setPrecursorMzEnd(rang.getMzEnd());
                List<ScanIndexDO> indexes = scanIndexService.getAll(query);
                TreeMap<Float, MzIntensityPairs> rtMap = new TreeMap<>();
                for (ScanIndexDO index : indexes) {
                    MzIntensityPairs mzIntensityPairs = mzXMLParser.parseValue(rafRead, index.getStart(), index.getEnd(), experimentDO.getCompressionType(), experimentDO.getPrecision());
                    rtMap.put(index.getRt(), mzIntensityPairs);
                }
            }

        }catch (IOException e){
            logger.error(e.getMessage());
            return ResultDO.buildError(ResultCode.CREATE_FILE_FAILED);
        }


        return null;
    }
}
