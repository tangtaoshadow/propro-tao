package com.westlake.air.pecs.compressor;

import com.alibaba.fastjson.JSON;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.pecs.domain.bean.analyse.WindowRang;
import com.westlake.air.pecs.domain.bean.compressor.DataInfo;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import com.westlake.air.pecs.domain.query.ScanIndexQuery;
import com.westlake.air.pecs.parser.MzXMLParser;
import com.westlake.air.pecs.service.ExperimentService;
import com.westlake.air.pecs.service.ScanIndexService;
import com.westlake.air.pecs.utils.CompressUtil;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;

@Component("compressor")
public class Compressor {

    public final Logger logger = LoggerFactory.getLogger(Compressor.class);

    public static final String SUFFIX_AIRUS_INFO = ".airusInfo";
    public static final String SUFFIX_AIRUS_DATA = ".airusData";
    public static final String CHANGE_LINE = "\r\n";

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

        DataInfo dataInfo = new DataInfo();
        dataInfo.setCompressionType(experimentDO.getCompressionType());
        dataInfo.setPrecision(experimentDO.getPrecision());

        try {
            if (!targetInfoFile.exists()) {
                targetInfoFile.createNewFile();
            }
            if (!targetDataFile.exists()) {
                targetDataFile.createNewFile();
            }
            RandomAccessFile rafRead = new RandomAccessFile(file, "r");

            FileWriter fwData = new FileWriter(targetDataFile.getAbsoluteFile());
            BufferedWriter bwData = new BufferedWriter(fwData);

            FileWriter fwInfo = new FileWriter(targetInfoFile.getAbsoluteFile());
            BufferedWriter bwInfo = new BufferedWriter(fwInfo);

            List<WindowRang> windowRangs = experimentService.getWindows(experimentDO.getId());
            dataInfo.setWindowRangs(windowRangs);

            int count = 0;
            long startAll = System.currentTimeMillis();
            for (WindowRang rang : windowRangs) {

                long start = System.currentTimeMillis();
                ScanIndexQuery query = new ScanIndexQuery(experimentDO.getId(), 2);
                query.setPrecursorMzStart(rang.getMzStart());
                query.setPrecursorMzEnd(rang.getMzEnd());
                List<ScanIndexDO> indexes = scanIndexService.getAll(query);
                for (ScanIndexDO index : indexes) {
                    MzIntensityPairs mzIntensityPairs = mzXMLParser.parseValue(rafRead, index.getStart(), index.getEnd(), experimentDO.getCompressionType(), experimentDO.getPrecision());

                    Float[] mzArray = mzIntensityPairs.getMzArray();
                    Float[] intensityArray = mzIntensityPairs.getIntensityArray();

//                    float[] mzIntensityArray = new float[mzArray.length * 2];
//                    for (int i = 0; i < mzArray.length; i++) {
//                        mzIntensityArray[i*2] = mzArray[i];
//                        mzIntensityArray[i*2 + 1] = intensityArray[i];
//                    }
//                    bwData.write(CompressUtil.transToString(mzIntensityArray) + CHANGE_LINE);
                    float[] fMzArray = new float[mzArray.length];
                    float[] fIntensityArray = new float[intensityArray.length];
                    for (int i = 0; i < mzArray.length; i++) {
                        fMzArray[i] = mzArray[i];
                        fIntensityArray[i] = intensityArray[i];
                    }

                    fwData.write(CompressUtil.transToString(fMzArray) + CHANGE_LINE);
                    fwData.write(CompressUtil.transToString(fIntensityArray) + CHANGE_LINE);
                }

                logger.info("Rang:"+rang.getMzStart()+":"+rang.getMzEnd()+" Finished,Time:"+(System.currentTimeMillis() - start));
            }

            logger.info("Total Cost:"+(System.currentTimeMillis() - startAll));
            bwData.flush();
            bwData.close();
            fwData.close();

            bwInfo.flush();
            bwInfo.close();
            fwInfo.close();
            rafRead.close();

        } catch (IOException e) {
            logger.error(e.getMessage());
            return ResultDO.buildError(ResultCode.CREATE_FILE_FAILED);
        }


        return null;
    }

    /**
     * 输出到文件前将一些没有作用的字符串删除
     *
     * @param indexList
     */
    private void readyToOutput(List<ScanIndexDO> indexList) {

    }

}
