package com.westlake.air.pecs.controller;

import com.westlake.air.pecs.test.algorithm.FragmentCalculator;
import com.westlake.air.pecs.decoy.generator.ShuffleGenerator;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.FragmentResult;
import com.westlake.air.pecs.domain.bean.MzResult;
import com.westlake.air.pecs.domain.db.TransitionDO;
import com.westlake.air.pecs.domain.query.TransitionQuery;
import com.westlake.air.pecs.service.TransitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import static com.westlake.air.pecs.constants.Constants.MAX_INSERT_RECORD_FOR_TRANSITION;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-19 16:03
 */
@Controller
@RequestMapping("decoy")
public class DecoyController extends BaseController {

    @Autowired
    FragmentCalculator fragmentCalculator;
    @Autowired
    ShuffleGenerator shuffleGenerator;
    @Autowired
    TransitionService transitionService;

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

    @RequestMapping(value = "/generate")
    String generate(Model model,
                    @RequestParam(value = "id", required = true) String id) {

        logger.info("正在删除原有伪肽段");
        //删除原有的伪肽段
        transitionService.deleteAllDecoyByLibraryId(id);
        logger.info("原有伪肽段删除完毕");
        //计算原始肽段数目
        TransitionQuery query = new TransitionQuery();
        query.setIsDecoy(false);
        query.setLibraryId(id);
        long totalCount = transitionService.count(query);
        int totalPage = (int) (totalCount / MAX_INSERT_RECORD_FOR_TRANSITION) + 1;
        query.setPageSize(MAX_INSERT_RECORD_FOR_TRANSITION);
        int countForInsert = 0;
        for (int i = 1; i <= totalPage; i++) {
            query.setPageNo(i);
            ResultDO<List<TransitionDO>> resultDO = transitionService.getList(query);
            List<TransitionDO> list = shuffleGenerator.generate(resultDO.getModel());
            ResultDO resultTmp = transitionService.insertAll(list, false);
            if (resultTmp.isSuccess()) {
                countForInsert += list.size();
                logger.info("插入新生成伪肽段" + countForInsert + "条");
            }
        }

//        model.addAttribute("resultList", result.size() > 100 ? result.subList(0, 100) : result);
        return "redirect:/transition/list?libraryId="+id+"&isDecoy=true";
    }
}
