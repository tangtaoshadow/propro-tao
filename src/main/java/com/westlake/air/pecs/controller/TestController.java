package com.westlake.air.pecs.controller;

import com.alibaba.fastjson.JSON;
import com.westlake.air.pecs.constants.TaskTemplate;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.service.ExperimentService;
import com.westlake.air.pecs.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.xml.crypto.Data;
import java.util.Date;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-07 13:30
 */
@Controller
@RequestMapping("test")
public class TestController {

    @Autowired
    ExperimentService experimentService;
    @Autowired
    TaskService taskService;

    @RequestMapping("test")
    @ResponseBody
    String test(Model model, RedirectAttributes redirectAttributes) {

        ExperimentDO experimentDO = experimentService.getById("5b738f19e63cc81c44325169").getModel();
        return JSON.toJSONString(experimentService.convAndComputeIrt(experimentDO, "5b67136d2ada5f15749a0140", 0.05f, 50f, 0.01f));
    }

    @RequestMapping("test2")
    @ResponseBody
    String test2(Model model, RedirectAttributes redirectAttributes) {

        ExperimentDO experimentDO = experimentService.getById("5b738f19e63cc81c44325169").getModel();
        ResultDO<SlopeIntercept> resultDO = experimentService.convAndComputeIrt(experimentDO, "5b67136d2ada5f15749a0140", 0.05f, 50f, 0.01f);
        if(resultDO.isFailed()){
            return JSON.toJSONString(resultDO);
        }
        SlopeIntercept slopeIntercept = resultDO.getModel();
        TaskDO taskDO = TaskDO.create(TaskTemplate.TEST, "LMS-Temp");
        taskService.insert(taskDO);
        ResultDO finalRes = experimentService.extract(experimentDO, "5b84bc9c58487f1060fa0c23", slopeIntercept, "陆妙善", 1200f, 0.05f, 2, taskDO);
        return JSON.toJSONString(finalRes);
    }
}
