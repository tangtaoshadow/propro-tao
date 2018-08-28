package com.westlake.air.pecs.controller;

import com.alibaba.fastjson.JSON;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.service.ExperimentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-07 13:30
 */
@Controller
@RequestMapping("test")
public class TestController {

    @Autowired
    ExperimentService experimentService;

    @RequestMapping("test")
    @ResponseBody
    String test(Model model, RedirectAttributes redirectAttributes) {

        ExperimentDO experimentDO = experimentService.getById("5b738f19e63cc81c44325169").getModel();
        return JSON.toJSONString(experimentService.convAndComputeIrt(experimentDO, "5b67136d2ada5f15749a0140", 0.05f, 30f, 0.01f));
    }
}
