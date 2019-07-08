package com.westlake.air.propro.algorithm.irt;

import com.westlake.air.propro.algorithm.extract.Extractor;
import com.westlake.air.propro.algorithm.parser.AirdFileParser;
import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.aird.Compressor;
import com.westlake.air.propro.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.propro.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.propro.domain.bean.irt.IrtResult;
import com.westlake.air.propro.domain.bean.score.SlopeIntercept;
import com.westlake.air.propro.domain.db.AnalyseDataDO;
import com.westlake.air.propro.domain.db.ExperimentDO;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.SwathIndexDO;
import com.westlake.air.propro.domain.db.simple.TargetPeptide;
import com.westlake.air.propro.domain.query.SwathIndexQuery;
import com.westlake.air.propro.service.PeptideService;
import com.westlake.air.propro.service.ScoreService;
import com.westlake.air.propro.service.SwathIndexService;
import com.westlake.air.propro.utils.ConvolutionUtil;
import com.westlake.air.propro.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

@Component("irt")
public class Irt {

    public final Logger logger = LoggerFactory.getLogger(Irt.class);

    @Autowired
    PeptideService peptideService;
    @Autowired
    AirdFileParser airdFileParser;
    @Autowired
    Extractor extractor;
    @Autowired
    ScoreService scoreService;
    @Autowired
    SwathIndexService swathIndexService;

    /**
     * 卷积iRT校准库的数据
     *
     * @param exp
     * @param library
     * @param mzExtractWindow
     * @return
     */
    public List<AnalyseDataDO> extractIrt(ExperimentDO exp, LibraryDO library, float mzExtractWindow) {

        ResultDO checkResult = ConvolutionUtil.checkExperiment(exp);
        if (checkResult.isFailed()) {
            logger.error(checkResult.getMsgInfo());
            return null;
        }

        File file = (File) checkResult.getModel();
        RandomAccessFile raf = null;

        List<AnalyseDataDO> finalList = new ArrayList<>();

        SwathIndexQuery query = new SwathIndexQuery(exp.getId(), 2);
        List<SwathIndexDO> swathList = swathIndexService.getAll(query);

        try {
            raf = new RandomAccessFile(file, "r");
            for (SwathIndexDO swathIndexDO : swathList) {
                //Step2.获取标准库的目标肽段片段的坐标
                //key为rt
                TreeMap<Float, MzIntensityPairs> rtMap;
                List<TargetPeptide> coordinates = peptideService.buildMS2Coordinates(library, SlopeIntercept.create(), -1, swathIndexDO.getRange(), null, exp.getType(), false, true);
                if (coordinates.size() == 0) {
                    logger.warn("No iRT Coordinates Found,Rang:" + swathIndexDO.getRange().getStart() + ":" + swathIndexDO.getRange().getEnd());
                    continue;
                }
                //Step3.提取指定原始谱图
                try {
                    rtMap = airdFileParser.parseSwathBlockValues(raf, swathIndexDO, exp.fetchCompressor(Compressor.TARGET_MZ), exp.fetchCompressor(Compressor.TARGET_INTENSITY));
                } catch (Exception e) {
                    logger.error("PrecursorMZStart:" + swathIndexDO.getRange().getStart());
                    throw e;
                }

                //Step4.卷积并且存储数据,如果传入的库是标准库,那么使用采样的方式进行卷积
                if(library.getType().equals(LibraryDO.TYPE_IRT)){
                    extractor.extractForIrt(finalList, coordinates, rtMap, null, mzExtractWindow, -1f);
                }else{
                    extractor.randomFetchForIrt(finalList, coordinates, rtMap, null, mzExtractWindow, -1f);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileUtil.close(raf);
        }

        return finalList;
    }

    /**
     * 卷积并且求出iRT
     *
     * @param experimentDO
     * @param library
     * @param mzExtractWindow
     * @param sigmaSpacing
     * @return
     */
    public ResultDO<IrtResult> convAndIrt(ExperimentDO experimentDO, LibraryDO library, Float mzExtractWindow, SigmaSpacing sigmaSpacing) {
        try {
            logger.info("开始卷积数据");
            long start = System.currentTimeMillis();
            List<AnalyseDataDO> dataList = extractIrt(experimentDO, library, mzExtractWindow);
            if (dataList == null) {
                return ResultDO.buildError(ResultCode.IRT_EXCEPTION);
            }
            logger.info("卷积完毕,耗时:" + (System.currentTimeMillis() - start));
            start = System.currentTimeMillis();
            ResultDO<IrtResult> resultDO = new ResultDO<>(false);
            try {
                resultDO = scoreService.computeIRt(dataList, library, sigmaSpacing);
            } catch (Exception e) {
                logger.error(e.getMessage());
                resultDO.setMsgInfo(e.getMessage());
            }
            logger.info("计算完毕,耗时:" + (System.currentTimeMillis() - start));

            if (resultDO.isFailed()) {
                return resultDO;
            }
            experimentDO.setIrtResult(resultDO.getModel());
            return resultDO;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
