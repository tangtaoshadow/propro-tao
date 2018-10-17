package com.westlake.air.pecs.compressor;

import com.alibaba.fastjson.JSON;
import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.domain.ResultDO;
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
import java.nio.ByteOrder;
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

        DataInfo dataInfo = new DataInfo();
        dataInfo.setCompressionType(experimentDO.getCompressionType());
        dataInfo.setPrecision(experimentDO.getPrecision());

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

            List<WindowRang> windowRangs = experimentService.getWindows(experimentDO.getId());
            dataInfo.setWindowRangs(windowRangs);

            long startAll = System.currentTimeMillis();
            int precision = Integer.parseInt(experimentDO.getPrecision());
            boolean isZlibCompression = "zlib".equalsIgnoreCase(experimentDO.getCompressionType());
            List<ScanIndexDO> outputIndexList = new ArrayList<>();
            long start = 0;
            long end = 0;
            for (WindowRang rang : windowRangs) {

                long startTime = System.currentTimeMillis();
                ScanIndexQuery query = new ScanIndexQuery(experimentDO.getId(), 2);
                query.setPrecursorMzStart(rang.getMzStart());
                query.setPrecursorMzEnd(rang.getMzEnd());
                List<ScanIndexDO> indexes = scanIndexService.getAll(query);

                for (ScanIndexDO index : indexes) {

                    String value = mzXMLParser.parseValue(rafRead, index.getStart(), index.getEnd());
                    Float[] values = mzXMLParser.getValues(new Base64().decode(value), precision, isZlibCompression, ByteOrder.BIG_ENDIAN);

                    TreeMap<Float, Float> map = new TreeMap<>();
                    for (int i = 0; i < values.length - 1; i += 2) {
                        if (values[i + 1] == 0f) {
                            continue;
                        }
                        map.put(values[i], values[i + 1]);
                    }

                    int i = 0;
                    float[] mzArray = new float[map.keySet().size()];
                    float[] intensityArray = new float[map.keySet().size()];
                    for (Float key : map.keySet()) {
                        mzArray[i] = key;
                        intensityArray[i] = map.get(key);
                        i++;
                    }
                    String indexesStr = CompressUtil.transToString(mzArray) + Constants.CHANGE_LINE + CompressUtil.transToString(intensityArray) + Constants.CHANGE_LINE;

                    index.setStart2(start);
                    end = end + indexesStr.length();
                    index.setEnd2(end);
                    start = end;

                    fwData.write(indexesStr);

                    scanIndexService.update(index);
                    //精简掉一些字段,保持输出的index尽量小
                    readyToOutput(index);
                }

                outputIndexList.addAll(indexes);
                logger.info("Rang:" + rang.getMzStart() + ":" + rang.getMzEnd() + " Finished,Time:" + (System.currentTimeMillis() - startTime));
            }
            //写入基本信息
            dataInfo.setScanIndex(outputIndexList);
            String dataInfoStr = JSON.toJSONString(dataInfo);
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
        experimentService.update(experimentDO);
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

}
