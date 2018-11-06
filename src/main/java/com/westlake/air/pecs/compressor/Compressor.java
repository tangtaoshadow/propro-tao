package com.westlake.air.pecs.compressor;

import com.alibaba.fastjson.JSON;
import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.pecs.domain.bean.analyse.WindowRang;
import com.westlake.air.pecs.domain.bean.compressor.AirInfo;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import com.westlake.air.pecs.domain.query.ScanIndexQuery;
import com.westlake.air.pecs.parser.MzXMLParser;
import com.westlake.air.pecs.service.ExperimentService;
import com.westlake.air.pecs.service.ScanIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

@Component("compressor")
public class Compressor {

    public final Logger logger = LoggerFactory.getLogger(Compressor.class);

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
        String fileNameWithoutSuffix = file.getName().replace(".mzXML", "").replace(".mzxml", "");
        String airiFilePath = fileParent + "/" + fileNameWithoutSuffix + Constants.SUFFIX_AIRUS_INFO;
        String airdFilePath = fileParent + "/" + fileNameWithoutSuffix + Constants.SUFFIX_AIRUS_DATA;
        File airiFile = new File(airiFilePath);
        File airdFile = new File(airdFilePath);

        List<WindowRang> windowRangs = experimentService.getWindows(experimentDO.getId());

        AirInfo airInfo = new AirInfo();
        airInfo.setWindowRangs(windowRangs);
        List<ScanIndexDO> swathIndexes = new ArrayList<>();

        try {
            if (!airiFile.exists()) {
                airiFile.createNewFile();
            }
            if (!airdFile.exists()) {
                airdFile.createNewFile();
            }
            RandomAccessFile rafRead = new RandomAccessFile(file, "r");

            FileWriter fwInfo = new FileWriter(airiFile.getAbsoluteFile());
            BufferedWriter bwInfo = new BufferedWriter(fwInfo);

            FileWriter fwData = new FileWriter(airdFile.getAbsoluteFile());
            BufferedWriter bwData = new BufferedWriter(fwData);

            long startAll = System.currentTimeMillis();
            int precision = Integer.parseInt(experimentDO.getPrecision());
            boolean isZlibCompression = "zlib".equalsIgnoreCase(experimentDO.getCompressionType());
            List<ScanIndexDO> outputIndexList = new ArrayList<>();
            Long start = 0L;

            //先写入所有的MS1
            List<ScanIndexDO> ms1IndexList = scanIndexService.getAll(new ScanIndexQuery(experimentDO.getId(), 1));
            logger.info("开始提取压缩MS1");
            for (ScanIndexDO index : ms1IndexList) {
                Long offset = processWithIndex(rafRead, fwData, index, precision, isZlibCompression, start);
                start = start + offset;
            }
            outputIndexList.addAll(ms1IndexList);
            logger.info("压缩MS1完毕,开始提取并且压缩MS2");

            //再写入所有的MS2
            for (WindowRang rang : windowRangs) {
                ScanIndexDO swathIndex = new ScanIndexDO();
                swathIndex.setStart2(start);
                swathIndex.setExperimentId(experimentDO.getId());
                swathIndex.setMsLevel(0);
                swathIndex.setPrecursorMzStart(rang.getMzStart());
                swathIndex.setPrecursorMzEnd(rang.getMzEnd());
                long startTime = System.currentTimeMillis();
                List<ScanIndexDO> indexes = scanIndexService.getAll(new ScanIndexQuery(experimentDO.getId(), 2, rang.getMzStart(), rang.getMzEnd()));
                for (ScanIndexDO index : indexes) {
                    Long offset = processWithIndex(rafRead, fwData, index, precision, isZlibCompression, start);
                    start = start + offset;
                    swathIndex.getRts().add(index.getRt());
                }
                swathIndex.setEnd2(start);
                swathIndexes.add(swathIndex);
                outputIndexList.addAll(indexes);
                logger.info("Rang:" + rang.getMzStart() + ":" + rang.getMzEnd() + " Finished,Time:" + (System.currentTimeMillis() - startTime));
            }

            //写入基本信息
            airInfo.setScanIndex(outputIndexList);
            airInfo.setSwathIndexes(swathIndexes);
            String dataInfoStr = JSON.toJSONString(airInfo);
            fwInfo.write(dataInfoStr);
            logger.info("Total Cost:" + (System.currentTimeMillis() - startAll));

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

        experimentDO.setHasAirusFile(true);
        experimentDO.setAiriPath(airiFilePath);
        experimentDO.setAirdPath(airdFilePath);
        experimentDO.setWindowRangs(windowRangs);
        experimentService.update(experimentDO);

        //新增SwathBlock块的索引
        scanIndexService.deleteAllSwathIndexByExperimentId(experimentDO.getId());
        scanIndexService.insertAll(swathIndexes, false);
        return new ResultDO(true);
    }

    /**
     * 输出到文件前将一些没有作用的字符串删除
     *
     * @param index
     */
    private void readyToOutput(ScanIndexDO index) {
        index.setId(null);
        index.setExperimentId(null);
        index.setStart(null);
        index.setEnd(null);
    }

    private Long processWithIndex(RandomAccessFile raf, FileWriter fwData, ScanIndexDO index, int precision, boolean isZlibCompression, Long start) throws IOException {
        String indexesStr = mzXMLParser.parseValueForAird(raf, index, precision, isZlibCompression);
        index.setStart2(start);
        index.setEnd2(start + indexesStr.length());
        fwData.write(indexesStr);
        scanIndexService.update(index);
        readyToOutput(index);

        return (long) indexesStr.length();
    }

}
