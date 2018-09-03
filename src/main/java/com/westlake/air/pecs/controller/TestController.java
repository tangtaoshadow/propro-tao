package com.westlake.air.pecs.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.mongodb.BasicDBObject;
import com.westlake.air.pecs.constants.TaskTemplate;
import com.westlake.air.pecs.dao.AnalyseDataDAO;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.SwathInput;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.AnalyseDataDO;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.service.AnalyseDataService;
import com.westlake.air.pecs.service.ExperimentService;
import com.westlake.air.pecs.service.TaskService;
import com.westlake.air.pecs.utils.FileUtil;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-07 13:30
 */
@Controller
@RequestMapping("test")
public class TestController extends BaseController{

    @Autowired
    ExperimentService experimentService;
    @Autowired
    TaskService taskService;
    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    AnalyseDataDAO analyseDataDAO;
    public static float MZ_EXTRACT_WINDOW = 0.05f;
    public static float RT_EXTRACT_WINDOW = 1200f;
    public static float SIGMA = 6.25f;
    public static float SPACING = 0.01f;

    @RequestMapping("test")
    @ResponseBody
    String test(Model model, RedirectAttributes redirectAttributes) {
        ExperimentDO experimentDO = experimentService.getById("5b89029258487f0e14a62b75").getModel();
        return JSON.toJSONString(experimentService.convAndComputeIrt(experimentDO, "5b88fece58487f13f0609019", MZ_EXTRACT_WINDOW, SIGMA, SPACING));
    }

    @RequestMapping("test2")
    @ResponseBody
    String test2(Model model, RedirectAttributes redirectAttributes) {
        ExperimentDO experimentDO = experimentService.getById("5b89029258487f0e14a62b75").getModel();
        ResultDO<SlopeIntercept> resultDO = experimentService.convAndComputeIrt(experimentDO, "5b88fece58487f13f0609019", MZ_EXTRACT_WINDOW, SIGMA, SPACING);
        if (resultDO.isFailed()) {
            return JSON.toJSONString(resultDO);
        }
        SlopeIntercept slopeIntercept = resultDO.getModel();
        long start = System.currentTimeMillis();
        SwathInput input = new SwathInput();
        input.setExperimentDO(experimentDO);
        input.setLibraryId("5b88feb758487f13f05f7083");
        input.setSlopeIntercept(slopeIntercept);
        input.setCreator("陆妙善");
        input.setRtExtractWindow(RT_EXTRACT_WINDOW);
        input.setMzExtractWindow(MZ_EXTRACT_WINDOW);
        input.setBuildType(2);
        ResultDO finalRes = experimentService.extract(input);
        logger.info("卷积耗时总计:"+(System.currentTimeMillis() - start));
        return JSON.toJSONString(finalRes);
    }

    @RequestMapping("test3")
    @ResponseBody
    String test3(Model model, RedirectAttributes redirectAttributes) {
        ExperimentDO experimentDO = experimentService.getById("5b738f19e63cc81c44325169").getModel();

        SwathInput input = new SwathInput();
        input.setExperimentDO(experimentDO);
        input.setIRtLibraryId("5b67136d2ada5f15749a0140");
        input.setLibraryId("5b84bc9c58487f1060fa0c23");
        input.setCreator("陆妙善");
        input.setRtExtractWindow(RT_EXTRACT_WINDOW);
        input.setMzExtractWindow(MZ_EXTRACT_WINDOW);
        input.setBuildType(2);

        TaskDO taskDO = new TaskDO(TaskTemplate.TEST, "LMS-TEMP2");
        taskService.insert(taskDO);
        experimentTask.swath(input, taskDO);
        return "OK";
    }

    @RequestMapping("test4")
    @ResponseBody
    String test4(Model model, RedirectAttributes redirectAttributes) throws IOException {
//        List<AnalyseDataDO> dataList = FileUtil.readAnalyseDataFromJsonFile("data/convStandard.json");
        String content = FileUtil.readFile("data/conv.json");
        List<AnalyseDataDO> dataList = JSONArray.parseArray(content, AnalyseDataDO.class);
        HashMap<String, Integer> countMap = new HashMap<>();
        for(AnalyseDataDO data : dataList){
            if(countMap.get(data.getPeptideRef()) != null){
                countMap.put(data.getPeptideRef(), countMap.get(data.getPeptideRef())+1);
            }else{
                countMap.put(data.getPeptideRef(), 1);
            }
        }
        logger.info(countMap.size()+"");
        for(String key : countMap.keySet()){
            logger.info(key+":"+countMap.get(key));

            if(countMap.get(key) > 6){
                logger.info(key);
            }
        }


//        analyseDataService.getTransitionGroup();
        return dataList.size() + "";
    }

    @RequestMapping("test5")
    @ResponseBody
    String test5(Model model, RedirectAttributes redirectAttributes) throws IOException {
//        List<BasicDBObject> result = analyseDataDAO.countSamePeptideRef("5b88d5bb58487f0b4437480d");
//        List<String> finalList = new ArrayList<>();
//        for(Object object : result){
//            Document doc = (Document) object;
//            if(doc.getInteger("count") > 6){
//                finalList.add(doc.getString("peptideRef"));
//            }
//        }
//        return JSON.toJSONString(finalList);
        return null;
    }
}
