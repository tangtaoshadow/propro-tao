package com.westlake.air.propro.controller;

import com.alibaba.fastjson.JSON;
import com.westlake.air.propro.algorithm.Airus;
import com.westlake.air.propro.algorithm.FragmentFactory;
import com.westlake.air.propro.async.LumsTask;
import com.westlake.air.propro.constants.PositionType;
import com.westlake.air.propro.constants.ScoreType;
import com.westlake.air.propro.constants.TaskTemplate;
import com.westlake.air.propro.dao.AnalyseDataDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.analyse.MzIntensityPairs;
import com.westlake.air.propro.domain.bean.scanindex.Position;
import com.westlake.air.propro.domain.bean.score.FeatureScores;
import com.westlake.air.propro.domain.params.LumsParams;
import com.westlake.air.propro.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.propro.domain.bean.score.SlopeIntercept;
import com.westlake.air.propro.domain.db.*;
import com.westlake.air.propro.domain.query.AnalyseDataQuery;
import com.westlake.air.propro.parser.AirdFileParser;
import com.westlake.air.propro.service.*;
import com.westlake.air.propro.utils.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.*;
import java.nio.ByteOrder;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-07 13:30
 */
@Controller
@RequestMapping("test")
public class TestController extends BaseController {

    @Autowired
    ExperimentService experimentService;
    @Autowired
    ScanIndexService scanIndexService;
    @Autowired
    TaskService taskService;
    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    AnalyseDataDAO analyseDataDAO;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    ScoreService scoreService;
    @Autowired
    Airus airus;
    @Autowired
    PeptideService peptideService;
    @Autowired
    FragmentFactory fragmentFactory;
    @Autowired
    LumsTask lumsTask;
    @Autowired
    AirdFileParser airdFileParser;

    public static float MZ_EXTRACT_WINDOW = 0.05f;
    public static float RT_EXTRACT_WINDOW = 1200f;
    public static float SIGMA = 6.25f;
    public static float SPACING = 0.01f;

    @RequestMapping("test")
    @ResponseBody
    String test(Model model, RedirectAttributes redirectAttributes) {

        RandomAccessFile raf = null;
        try {
            File file = new File("D:\\data\\test\\little\\test.aird");
            raf = new RandomAccessFile(file, "r");
            MzIntensityPairs pairs = airdFileParser.parseValue(raf, new Position(64214080L, 4601L), new Position(64218681L, 528L), ByteOrder.BIG_ENDIAN);
            System.out.println(pairs.getIntensityArray().length);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            FileUtil.close(raf);
        }

        return null;
    }

    //计算iRT
    @RequestMapping("test2")
    @ResponseBody
    String test2(Model model, RedirectAttributes redirectAttributes) {
        ExperimentDO experimentDO = experimentService.getById("5b89029258487f0e14a62b75").getModel();
        ResultDO<SlopeIntercept> resultDO = experimentService.convAndIrt(experimentDO, "5b88fece58487f13f0609019", MZ_EXTRACT_WINDOW, SigmaSpacing.create());
        if (resultDO.isFailed()) {
            return JSON.toJSONString(resultDO);
        }
        SlopeIntercept slopeIntercept = resultDO.getModel();
        long start = System.currentTimeMillis();
        LumsParams input = new LumsParams();
        input.setExperimentDO(experimentDO);
        input.setLibraryId("5b88feb758487f13f05f7083");
        input.setSlopeIntercept(slopeIntercept);
        input.setCreator("陆妙善");
        input.setRtExtractWindow(RT_EXTRACT_WINDOW);
        input.setMzExtractWindow(MZ_EXTRACT_WINDOW);
        ResultDO finalRes = experimentService.extract(input);
        logger.info("卷积耗时总计:" + (System.currentTimeMillis() - start));
        return JSON.toJSONString(finalRes);
    }

    @RequestMapping("test3")
    @ResponseBody
    String test3(Model model, RedirectAttributes redirectAttributes) {
        ExperimentDO experimentDO = experimentService.getById("5b738f19e63cc81c44325169").getModel();

        LumsParams input = new LumsParams();
        input.setExperimentDO(experimentDO);
        input.setIRtLibraryId("5b67136d2ada5f15749a0140");
        input.setLibraryId("5b84bc9c58487f1060fa0c23");
        input.setCreator("陆妙善");
        input.setRtExtractWindow(RT_EXTRACT_WINDOW);
        input.setMzExtractWindow(MZ_EXTRACT_WINDOW);

        TaskDO taskDO = new TaskDO(TaskTemplate.TEST, "LMS-TEMP2");
        taskService.insert(taskDO);
        lumsTask.swath(input, taskDO);
        return "OK";
    }

    @RequestMapping("test6")
    @ResponseBody
    String test6(Model model, RedirectAttributes redirectAttributes) throws IOException {
//        AnalyseDataQuery query = new AnalyseDataQuery("5c1ba15dcb15b6e1c4f20c63");
        AnalyseDataQuery query = new AnalyseDataQuery("5c1c9a5acb15b6bb244d985e");
        query.setFdrEnd(0.01);
        query.setIsDecoy(false);
        List<AnalyseDataDO> dataList = analyseDataService.getAll(query);
        logger.info("总计识别肽段:" + dataList.size() + "个");
        int count = 0;
        for (AnalyseDataDO data : dataList) {
            for (FeatureScores featureScores : data.getFeatureScoresList()) {
                if (featureScores.getRt().equals(data.getBestRt())) {
                    if (featureScores.get(ScoreType.XcorrShapeWeighted) < 0.7 && featureScores.get(ScoreType.XcorrShape) < 0.7) {
                        logger.info("该肽段异常:" + data.getPeptideRef());
                        count++;
                    }
                    break;
                }
            }
        }

        return count + "";

    }

    @RequestMapping("test8")
    @ResponseBody
    String test8(Model model, RedirectAttributes redirectAttributes) throws IOException {

        AnalyseDataQuery query = new AnalyseDataQuery();
        query.setIsDecoy(false);
        query.setFdrEnd(0.01);
        query.setOverviewId("5c1c9a5acb15b6bb244d985e");
        query.setSortColumn("fdr");
        query.setPageSize(5000);
        ResultDO<List<AnalyseDataDO>> resultDO = analyseDataService.getList(query);


        File file = new File("D://MatchedPeptide.txt");
        file.createNewFile();
        StringBuilder content = new StringBuilder();
        for (AnalyseDataDO data : resultDO.getModel()) {
            content.append(data.getPeptideRef()).append(",").append(data.getBestRt()).append("\r\n");
        }

        byte[] b = content.toString().getBytes();
        int l = b.length;
        OutputStream os = new FileOutputStream(file);
        os.write(b, 0, l);
        os.close();
        return "success";
    }
}
