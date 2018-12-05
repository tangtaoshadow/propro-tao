package com.westlake.air.pecs.controller;

import com.alibaba.fastjson.JSON;
import com.westlake.air.pecs.algorithm.Airus;
import com.westlake.air.pecs.constants.TaskTemplate;
import com.westlake.air.pecs.dao.AnalyseDataDAO;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.SwathParams;
import com.westlake.air.pecs.domain.bean.airus.AirusParams;
import com.westlake.air.pecs.domain.bean.airus.FinalResult;
import com.westlake.air.pecs.domain.bean.airus.TrainAndTest;
import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.*;
import com.westlake.air.pecs.domain.query.AnalyseDataQuery;
import com.westlake.air.pecs.domain.query.AnalyseOverviewQuery;
import com.westlake.air.pecs.service.*;
import com.westlake.air.pecs.utils.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
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
    ScoresService scoresService;
    @Autowired
    Airus airus;

    public static float MZ_EXTRACT_WINDOW = 0.05f;
    public static float RT_EXTRACT_WINDOW = 1200f;
    public static float SIGMA = 6.25f;
    public static float SPACING = 0.01f;

    @RequestMapping("test")
    @ResponseBody
    String test(Model model, RedirectAttributes redirectAttributes) {
        return "success";
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
        SwathParams input = new SwathParams();
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

        SwathParams input = new SwathParams();
        input.setExperimentDO(experimentDO);
        input.setIRtLibraryId("5b67136d2ada5f15749a0140");
        input.setLibraryId("5b84bc9c58487f1060fa0c23");
        input.setCreator("陆妙善");
        input.setRtExtractWindow(RT_EXTRACT_WINDOW);
        input.setMzExtractWindow(MZ_EXTRACT_WINDOW);

        TaskDO taskDO = new TaskDO(TaskTemplate.TEST, "LMS-TEMP2");
        taskService.insert(taskDO);
        experimentTask.swath(input, taskDO);
        return "OK";
    }

    @RequestMapping("test4")
    @ResponseBody
    String test4(Model model, RedirectAttributes redirectAttributes) throws IOException {
        List<AnalyseDataDO> dataList = FileUtil.readAnalyseDataFromJsonFile("D://convWithDecoy.json");
        long start = System.currentTimeMillis();
        SwathParams input = new SwathParams();
        input.setLibraryId("5b88feb758487f13f05f7083");
        input.setSlopeIntercept(new SlopeIntercept(0.0633584d, -64.7064d));
        input.setSigmaSpacing(SigmaSpacing.create());
        scoresService.score(dataList, input);
        logger.info("耗时:" + (System.currentTimeMillis() - start));
        return dataList.size() + "";
    }

    @RequestMapping("test6")
    @ResponseBody
    String test6(Model model, RedirectAttributes redirectAttributes) throws IOException {
        long start = System.currentTimeMillis();
        FinalResult finalResult = airus.doAirus("5b967e5fcbaa7e2940fc6537",new AirusParams());
        logger.info("打分耗时:" + (System.currentTimeMillis() - start));
        return JSON.toJSONString(finalResult);
    }

    @RequestMapping("test7")
    @ResponseBody
    String test7(Model model, RedirectAttributes redirectAttributes) throws IOException {
        String trainAndTestContent = FileUtil.readFile("D://trainAndTest.json");
        long start = System.currentTimeMillis();
        TrainAndTest tt = JSON.parseObject(trainAndTestContent, TrainAndTest.class);
        return tt.getTestData().length + "/" + tt.getTrainData().length + "/" + tt.getTestId().length + "/" + tt.getTrainId().length;
    }

    @RequestMapping("test8")
    @ResponseBody
    String test8(Model model, RedirectAttributes redirectAttributes) throws IOException {
        ResultDO<List<AnalyseOverviewDO>> resultDO = analyseOverviewService.getList(new AnalyseOverviewQuery());
        if(resultDO.isSuccess()){
            List<AnalyseOverviewDO> overviews = resultDO.getModel();
            for(AnalyseOverviewDO ov : overviews){
                AnalyseDataQuery query = new AnalyseDataQuery();
                query.setOverviewId(ov.getId());
                long count = analyseDataDAO.count(query);
                ov.setTotalPeptideCount((int)count);
                analyseOverviewService.update(ov);
            }
        }
        return "success";
    }
}
