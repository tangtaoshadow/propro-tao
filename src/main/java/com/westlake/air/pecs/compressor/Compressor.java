package com.westlake.air.pecs.compressor;

import com.alibaba.fastjson.JSON;
import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.constants.PositionType;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.dao.ConfigDAO;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.analyse.WindowRang;
import com.westlake.air.pecs.domain.bean.compressor.AirInfo;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import com.westlake.air.pecs.domain.query.ScanIndexQuery;
import com.westlake.air.pecs.parser.MzXMLParser;
import com.westlake.air.pecs.service.ExperimentService;
import com.westlake.air.pecs.service.ScanIndexService;
import com.westlake.air.pecs.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Component("compressor")
public class Compressor {

    public final Logger logger = LoggerFactory.getLogger(Compressor.class);

    @Autowired
    ScanIndexService scanIndexService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    MzXMLParser mzXMLParser;
    @Autowired
    ConfigDAO configDAO;

    /**
     * @param experimentDO
     * @param isBinary     输出文件是否为二进制文件
     * @return
     */
    public ResultDO doCompress(ExperimentDO experimentDO, boolean isBinary) {
        String filePath = experimentDO.getFilePath();
        File file = new File(filePath);

        String configAirdPath = configDAO.getConfig().getAirdFilePath();
        String fileParent = "";
        if (configAirdPath != null && !configAirdPath.isEmpty()) {
            fileParent = configAirdPath;
        } else {
            fileParent = file.getParent();
        }
        String fileNameWithoutSuffix = file.getName().replace(".mzXML", "").replace(".mzxml", "");

        String airdFilePath;

        if (isBinary) {
            airdFilePath = fileParent + "/" + fileNameWithoutSuffix + Constants.SUFFIX_AIRUS_DATA_BIN;
        } else {
            airdFilePath = fileParent + "/" + fileNameWithoutSuffix + Constants.SUFFIX_AIRUS_DATA;
        }
        String airdIndexPath = fileParent + "/" + fileNameWithoutSuffix + Constants.SUFFIX_AIRUS_INFO;

        File airiFile = new File(airdIndexPath);
        File airdFile = new File(airdFilePath);

        List<WindowRang> windowRangs = experimentService.getWindows(experimentDO.getId());

        AirInfo airInfo = new AirInfo();
        airInfo.setWindowRangs(windowRangs);
        List<ScanIndexDO> swathIndexes = new ArrayList<>();

        RandomAccessFile rafRead = null;
        FileWriter fwInfo = null;
        BufferedWriter bwInfo = null;
        FileWriter fwData = null;
        BufferedWriter bwData = null;

        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        try {
            if (!airiFile.exists()) {
                airiFile.createNewFile();
            }
            if (!airdFile.exists()) {
                airdFile.createNewFile();
            }
            rafRead = new RandomAccessFile(file, "r");

            //所有的索引数据均以JSON文件保存
            fwInfo = new FileWriter(airiFile.getAbsoluteFile());
            bwInfo = new BufferedWriter(fwInfo);

            if (isBinary) {
                fos = new FileOutputStream(airdFile.getAbsoluteFile());
                bos = new BufferedOutputStream(fos);
            } else {
                fwData = new FileWriter(airdFile.getAbsoluteFile());
                bwData = new BufferedWriter(fwData);
            }

            long startAll = System.currentTimeMillis();
            int precision = Integer.parseInt(experimentDO.getPrecision());
            boolean isZlibCompression = "zlib".equalsIgnoreCase(experimentDO.getCompressionType());

            //在进行正式压缩之前,先把experiment的是否有AirusFile的状态置为false,以防在压缩错误的时候数据库的AirusFile状态仍然为可用
            if (isBinary) {
                if (experimentDO.getHasAirusBinFile() == null || experimentDO.getHasAirusBinFile()) {
                    experimentDO.setHasAirusBinFile(false);
                    experimentService.update(experimentDO);
                }
            } else {
                if (experimentDO.getHasAirusFile() == null || experimentDO.getHasAirusFile()) {
                    experimentDO.setHasAirusFile(false);
                    experimentService.update(experimentDO);
                }
            }

            Long start = 0L;

            //先写入所有的MS1
            List<ScanIndexDO> ms1IndexList = scanIndexService.getAll(new ScanIndexQuery(experimentDO.getId(), 1));
            logger.info("开始提取压缩MS1");
            for (ScanIndexDO index : ms1IndexList) {
                Long offset = null;
                if (isBinary) {
                    offset = processWithIndexForAirdBin(rafRead, bos, index, precision, isZlibCompression, start);
                } else {
                    offset = processWithIndexForAirdText(rafRead, fwData, index, precision, isZlibCompression, start);
                }
                start = start + offset;
            }
            List<ScanIndexDO> outputIndexList = new ArrayList<>(ms1IndexList);
            logger.info("压缩MS1完毕,开始提取并且压缩MS2");

            //再写入所有的MS2
            for (WindowRang rang : windowRangs) {
                ScanIndexDO swathIndex = new ScanIndexDO();
                swathIndex.setPosStart(PositionType.AIRD, start);
                swathIndex.setExperimentId(experimentDO.getId());
                swathIndex.setMsLevel(0);
                swathIndex.setPrecursorMzStart(rang.getMzStart());
                swathIndex.setPrecursorMzEnd(rang.getMzEnd());
                long startTime = System.currentTimeMillis();
                List<ScanIndexDO> indexes = scanIndexService.getAll(new ScanIndexQuery(experimentDO.getId(), 2, rang.getMzStart(), rang.getMzEnd()));
                List<Float> rts = new ArrayList<>();
                for (ScanIndexDO index : indexes) {
                    Long offset = null;
                    if (isBinary) {
                        offset = processWithIndexForAirdBin(rafRead, bos, index, precision, isZlibCompression, start);
                    } else {
                        offset = processWithIndexForAirdText(rafRead, fwData, index, precision, isZlibCompression, start);
                    }
                    start = start + offset;
                    rts.add(index.getRt());
                }
                swathIndex.setPosEnd(PositionType.AIRD, start);
                swathIndex.setRts(rts);
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

        } catch (IOException e) {
            logger.error(e.getMessage());
            return ResultDO.buildError(ResultCode.CREATE_FILE_FAILED);
        } finally {
            FileUtil.close(bwData);
            FileUtil.close(fwData);

            FileUtil.close(bwInfo);
            FileUtil.close(fwInfo);

            FileUtil.close(bos);
            FileUtil.close(fos);

            FileUtil.close(rafRead);
        }

        if (isBinary) {
            experimentDO.setHasAirusBinFile(true);
        } else {
            experimentDO.setHasAirusFile(true);
        }

        experimentDO.setAirdIndexPath(airdIndexPath);
        if (isBinary) {
            experimentDO.setAirdBinPath(airdFilePath);
        } else {
            experimentDO.setAirdPath(airdFilePath);
        }
        experimentDO.setWindowRangs(windowRangs);
        experimentService.update(experimentDO);

        //新增SwathBlock块的索引
        scanIndexService.deleteAllSwathIndexByExperimentId(experimentDO.getId());
        scanIndexService.insertAll(swathIndexes, false);
        return new ResultDO(true);
    }

    private void prepare() {

    }

    /**
     * 输出到文件前将一些没有作用的字符串删除
     *
     * @param index
     */
    private void readyToOutput(ScanIndexDO index) {
        index.setId(null);
        index.setExperimentId(null);
    }

    /**
     * 输出文件为二进制文件存储方案.
     *
     * @param raf
     * @param bos
     * @param index
     * @param precision
     * @param isZlibCompression
     * @param start
     * @return
     * @throws IOException
     */
    private Long processWithIndexForAirdBin(RandomAccessFile raf, BufferedOutputStream bos, ScanIndexDO index, int precision, boolean isZlibCompression, Long start) throws IOException {
        byte[] indexesByte = mzXMLParser.parseByteValueForAird(raf, index, start, precision, isZlibCompression, 0);
//        index.setPosStart(PositionType.AIRD, start);
//        index.setPosEnd(PositionType.AIRD, start + indexesByte.length);
        bos.write(indexesByte);
        scanIndexService.update(index);
        readyToOutput(index);

        return (long) indexesByte.length;
    }

    private Long processWithIndexForAirdText(RandomAccessFile raf, FileWriter fwData, ScanIndexDO index, int precision, boolean isZlibCompression, Long start) throws IOException {
        return processWithIndexForAirdText(raf, fwData, index, precision, isZlibCompression, start, 0);
    }

    private Long processWithIndexForAirdText(RandomAccessFile raf, FileWriter fwData, ScanIndexDO index, int precision, boolean isZlibCompression, Long start, int zeroCount) throws IOException {
        String indexesStr = mzXMLParser.parseValueForAird(raf, index, precision, isZlibCompression, zeroCount);
        index.setPosStart(PositionType.AIRD, start);
        index.setPosDelta(PositionType.AIRD, (long)indexesStr.length());
        fwData.write(indexesStr);
        scanIndexService.update(index);
        readyToOutput(index);

        return (long) indexesStr.length();
    }

}
