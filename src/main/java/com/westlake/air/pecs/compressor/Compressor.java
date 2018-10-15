package com.westlake.air.pecs.compressor;

import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import com.westlake.air.pecs.domain.query.ScanIndexQuery;
import com.westlake.air.pecs.service.ScanIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Component("compressor")
public class Compressor {

    public final Logger logger = LoggerFactory.getLogger(Compressor.class);

    public static final String SUFFIX_AIRUS_INFO = ".airusInfo";
    public static final String SUFFIX_AIRUS_DATA = ".airusData";

    @Autowired
    ScanIndexService scanIndexService;

    public ResultDO doCompress(ExperimentDO experimentDO) {
        String fileLocation = experimentDO.getFileLocation();
        File file = new File(fileLocation);
        String fileParent = file.getParent();
        String fileNameWithoutSuffix = file.getName().replace("." + experimentDO.getFileType(), "");
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
        }catch (IOException e){
            logger.error(e.getMessage());
            return ResultDO.buildError(ResultCode.CREATE_FILE_FAILED);
        }

        ScanIndexQuery query = new ScanIndexQuery();
        query.setExperimentId(experimentDO.getId());
        List<ScanIndexDO> scanIndexList = scanIndexService.getAll(query);
        return null;
    }
}
