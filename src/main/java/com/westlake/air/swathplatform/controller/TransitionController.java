package com.westlake.air.swathplatform.controller;

import com.fasterxml.jackson.databind.ser.Serializers;
import com.westlake.air.swathplatform.algorithm.FragmentCalculator;
import com.westlake.air.swathplatform.dao.TransitionDAO;
import com.westlake.air.swathplatform.domain.ResultDO;
import com.westlake.air.swathplatform.domain.bean.FragmentResult;
import com.westlake.air.swathplatform.domain.db.LibraryDO;
import com.westlake.air.swathplatform.domain.db.TransitionDO;
import com.westlake.air.swathplatform.domain.query.LibraryQuery;
import com.westlake.air.swathplatform.domain.query.TransitionQuery;
import com.westlake.air.swathplatform.service.TransitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-12 12:19
 */
@Controller
@RequestMapping("transition")
public class TransitionController extends BaseController {

    @Autowired
    FragmentCalculator fragmentCalculator;

    @Autowired
    TransitionService transitionService;

    @RequestMapping(value = "/list")
    String list(Model model,
                @RequestParam(value = "libraryId", required = false) String libraryId,
                @RequestParam(value = "proteinName", required = false) String proteinName,
                @RequestParam(value = "peptideSequence", required = false) String peptideSequence,
                @RequestParam(value = "transitionName", required = false) String transitionName,
                @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                @RequestParam(value = "pageSize", required = false, defaultValue = "100") Integer pageSize) {
        long startTime = System.currentTimeMillis();
        model.addAttribute("libraryId", libraryId);
        model.addAttribute("proteinName", proteinName);
        model.addAttribute("peptideSequence", peptideSequence);
        model.addAttribute("transitionName", transitionName);
        model.addAttribute("pageSize", pageSize);
        TransitionQuery query = new TransitionQuery();

        if (libraryId != null && !libraryId.isEmpty()) {
            query.setLibraryId(libraryId);
        }
        if (peptideSequence != null && !peptideSequence.isEmpty()) {
            query.setPeptideSequence(peptideSequence);
        }
        if (proteinName != null && !proteinName.isEmpty()) {
            query.setProteinName(proteinName);
        }
        if (transitionName != null && !transitionName.isEmpty()) {
            query.setTransitionName(transitionName);
        }

        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        ResultDO<List<TransitionDO>> resultDO = transitionService.getList(query);

        model.addAttribute("transitionList", resultDO.getModel());
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);
        StringBuilder builder = new StringBuilder();
        builder.append("本次搜索耗时:").append(System.currentTimeMillis()-startTime).append("毫秒;包含搜索结果总计:")
                .append(resultDO.getTotalNum()).append("条");
        model.addAttribute("searchResult",builder.toString());
        return "transition/list";
    }

    @RequestMapping(value = "/detail/{id}")
    String detail(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        ResultDO<TransitionDO> resultDO = transitionService.getById(id);
        if (resultDO.isSuccess()) {
            model.addAttribute("transition", resultDO.getModel());
            return "/transition/detail";
        } else {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/transition/list";
        }
    }

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    @ResponseBody
    FragmentResult test(Model model) {
        ResultDO<TransitionDO> resultDO = transitionService.getById("5b1f466547d23c17a07e5b98");
        return fragmentCalculator.getResult(resultDO.getModel());
    }
}
