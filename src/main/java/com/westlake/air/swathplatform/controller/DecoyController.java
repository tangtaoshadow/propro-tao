package com.westlake.air.swathplatform.controller;

import com.westlake.air.swathplatform.algorithm.FragmentCalculator;
import com.westlake.air.swathplatform.domain.bean.FragmentResult;
import com.westlake.air.swathplatform.domain.bean.MzResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-19 16:03
 */
@Controller
@RequestMapping("decoy")
public class DecoyController extends BaseController {

    @Autowired
    FragmentCalculator fragmentCalculator;

    @RequestMapping(value = "/overview/{id}")
    String overview(Model model, @PathVariable("id") String id) {
        FragmentResult result = fragmentCalculator.decoyOverview(id);

        model.addAttribute(SUCCESS_MSG, result.getMsgInfo());
        model.addAttribute("overlapList", result.getOverlapList());
        model.addAttribute("decoyList", result.getDecoyList());
        model.addAttribute("targetList", result.getTargetList());
        return "/decoy/overview";
    }

    @RequestMapping(value = "/check")
    String check(Model model,
                 @RequestParam(value = "id", required = true) String id,
                 @RequestParam(value = "isDecoy", required = false) boolean isDecoy) {
        List<MzResult> result = fragmentCalculator.check(id, 0.1, isDecoy);
        model.addAttribute("resultList", result.size() > 100 ? result.subList(0, 100) : result);
        return "/decoy/check";
    }
}
