package com.westlake.air.propro.compressor;

import com.alibaba.fastjson.JSON;
import com.westlake.air.propro.constants.Constants;
import com.westlake.air.propro.constants.PositionType;
import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.analyse.WindowRange;
import com.westlake.air.propro.domain.bean.compressor.AirdInfo;
import com.westlake.air.propro.domain.db.ExperimentDO;
import com.westlake.air.propro.domain.db.ProjectDO;
import com.westlake.air.propro.domain.db.ScanIndexDO;
import com.westlake.air.propro.domain.query.ScanIndexQuery;
import com.westlake.air.propro.parser.MzXMLParser;
import com.westlake.air.propro.service.ExperimentService;
import com.westlake.air.propro.service.ProjectService;
import com.westlake.air.propro.service.ScanIndexService;
import com.westlake.air.propro.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Component("airdCompressor")
public class AirdCompressor {

    public final Logger logger = LoggerFactory.getLogger(AirdCompressor.class);

    @Autowired
    ScanIndexService scanIndexService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    MzXMLParser mzXMLParser;
    @Autowired
    ProjectService projectService;

    /**
     * @param experimentDO
     * @return
     */
    public ResultDO compress(ExperimentDO experimentDO) {
        String filePath = experimentDO.getFilePath();
        File file = new File(filePath);

        ResultDO<ProjectDO> projectResult = projectService.getByName(experimentDO.getProjectName());
        String configAirdPath = projectResult.getModel().getAirdPath();
        String fileParent = "";
        if (configAirdPath != null && !configAirdPath.isEmpty()) {
            fileParent = configAirdPath;
        } else {
            fileParent = file.getParent();
        }
        String fileNameWithoutSuffix = file.getName().replace(".mzXML", "").replace(".mzxml", "");

        String airdFilePath = fileParent + fileNameWithoutSuffix + Constants.SUFFIX_AIRUS_DATA;
        String airdIndexPath = fileParent + fileNameWithoutSuffix + Constants.SUFFIX_AIRUS_DATA_INFO;

        File airiFile = new File(airdIndexPath);
        File airdFile = new File(airdFilePath);

        List<WindowRange> windowRanges = experimentService.getPrmWindows(experimentDO.getId());

        AirdInfo airdInfo = new AirdInfo();
        airdInfo.setRangeList(windowRanges);
        List<ScanIndexDO> swathIndexes = new ArrayList<>();

        RandomAccessFile rafRead = null;
        FileWriter fwInfo = null;

        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        try {
            if (!airiFile.exists()) {
                airiFile.getParentFile().mkdirs();
                airiFile.createNewFile();
            }
            if (!airdFile.exists()) {
                airdFile.getParentFile().mkdirs();
                airdFile.createNewFile();
            }
            rafRead = new RandomAccessFile(file, "r");

            //所有的索引数据均以JSON文件保存
            fwInfo = new FileWriter(airiFile.getAbsoluteFile());
            fos = new FileOutputStream(airdFile.getAbsoluteFile());
            bos = new BufferedOutputStream(fos);

            long startAll = System.currentTimeMillis();
            int precision = Integer.parseInt(experimentDO.getPrecision());
            boolean isZlibCompression = "zlib".equalsIgnoreCase(experimentDO.getCompressionType());

            //在进行正式压缩之前,先把experiment的是否有AirusFile的状态置为false,以防在压缩错误的时候数据库的AirusFile状态仍然为可用
            if (experimentDO.getHasAirusFile() == null || experimentDO.getHasAirusFile()) {
                experimentDO.setHasAirusFile(false);
                experimentService.update(experimentDO);
            }

            Long start = 0L;

            //先写入所有的MS1
            List<ScanIndexDO> ms1IndexList = scanIndexService.getAll(new ScanIndexQuery(experimentDO.getId(), 1));
            logger.info("开始提取压缩MS1");
            for (ScanIndexDO index : ms1IndexList) {
                Long offset = processWithIndex(rafRead, bos, index, precision, isZlibCompression, start);
                start = start + offset;
            }
            List<ScanIndexDO> outputIndexList = new ArrayList<>(ms1IndexList);
            logger.info("压缩MS1完毕,开始提取并且压缩MS2");

            //再写入所有的MS2
            for (WindowRange rang : windowRanges) {
                ScanIndexDO swathIndex = new ScanIndexDO();
                swathIndex.setPosStart(PositionType.SWATH, start);
                swathIndex.setExperimentId(experimentDO.getId());
                swathIndex.setMsLevel(0);
                swathIndex.setPrecursorMzStart(rang.getStart());
                swathIndex.setPrecursorMzEnd(rang.getEnd());
                long startTime = System.currentTimeMillis();
                List<ScanIndexDO> indexes = scanIndexService.getAll(new ScanIndexQuery(experimentDO.getId(), 2, rang.getStart(), rang.getEnd()));
                List<Float> rts = new ArrayList<>();
                List<Integer> blockSizes = new ArrayList<>();
                for (ScanIndexDO index : indexes) {
                    Long offset = processWithIndex(rafRead, bos, index, precision, isZlibCompression, start);
                    start = start + offset;
                    blockSizes.add(index.getPosDelta(PositionType.AIRD_MZ).intValue());
                    blockSizes.add(index.getPosDelta(PositionType.AIRD_INTENSITY).intValue());
                    rts.add(index.getRt());
                }
                swathIndex.setPosEnd(PositionType.SWATH, start);
                swathIndex.setRts(rts);
                swathIndex.setBlocks(blockSizes);
                swathIndexes.add(swathIndex);

                outputIndexList.addAll(indexes);
                logger.info("Rang:" + rang.getStart() + ":" + rang.getEnd() + " Finished,Time:" + (System.currentTimeMillis() - startTime));
            }

            //写入基本信息
            airdInfo.setScanIndexList(outputIndexList);
            airdInfo.setSwathIndexList(swathIndexes);
            airdInfo.setAirdPath(airdFilePath);
            airdInfo.setCreator(experimentDO.getCreator());
            airdInfo.setDescription(experimentDO.getDescription());
            airdInfo.setOverlap(experimentDO.getOverlap());
            String dataInfoStr = JSON.toJSONString(airdInfo);
            fwInfo.write(dataInfoStr);
            logger.info("Total Cost:" + (System.currentTimeMillis() - startAll));

        } catch (IOException e) {
            logger.error(e.getMessage());
            return ResultDO.buildError(ResultCode.CREATE_FILE_FAILED);
        } finally {
            FileUtil.close(fwInfo);
            FileUtil.close(bos);
            FileUtil.close(fos);
            FileUtil.close(rafRead);
        }

        experimentDO.setHasAirusFile(true);
        experimentDO.setAirdIndexPath(airdIndexPath);
        experimentDO.setAirdPath(airdFilePath);

        experimentDO.setWindowRanges(windowRanges);
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
    private Long processWithIndex(RandomAccessFile raf, BufferedOutputStream bos, ScanIndexDO index, int precision, boolean isZlibCompression, Long start) throws IOException {
        byte[] indexesByte = mzXMLParser.parseValueForAird(raf, index, start, precision, isZlibCompression, 0);
        bos.write(indexesByte);
        scanIndexService.update(index);
        readyToOutput(index);

        return (long) indexesByte.length;
    }

}
