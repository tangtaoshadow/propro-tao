package com.westlake.air.swathplatform.controller;

import com.westlake.air.swathplatform.constants.ResultCode;
import com.westlake.air.swathplatform.domain.ResultDO;
import com.westlake.air.swathplatform.domain.db.ExperimentDO;
import com.westlake.air.swathplatform.domain.db.ScanIndexDO;
import com.westlake.air.swathplatform.domain.query.ScanIndexQuery;
import com.westlake.air.swathplatform.service.ExperimentService;
import com.westlake.air.swathplatform.service.ScanIndexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-11 20:03
 */
@Controller
@RequestMapping("scanindex")
public class ScanIndexController extends BaseController {

    @Autowired
    ScanIndexService scanIndexService;

    @Autowired
    ExperimentService experimentService;

    @RequestMapping(value = "/list")
    String list(Model model,
                @RequestParam(value = "experimentId", required = false) String experimentId,
                @RequestParam(value = "msLevel", required = false) Integer msLevel,
                @RequestParam(value = "numStart", required = false) Integer numStart,
                @RequestParam(value = "numEnd", required = false) Integer numEnd,
                @RequestParam(value = "rtStart", required = false) Double rtStart,
                @RequestParam(value = "rtEnd", required = false) Double rtEnd,
                @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                @RequestParam(value = "pageSize", required = false, defaultValue = "30") Integer pageSize) {
        long startTime = System.currentTimeMillis();

        model.addAttribute("experimentId", experimentId);
        model.addAttribute("numStart", numStart);
        model.addAttribute("numEnd", numEnd);
        model.addAttribute("rtStart", rtStart);
        model.addAttribute("rtEnd", rtEnd);
        model.addAttribute("msLevel", msLevel);

        if (experimentId == null || experimentId.isEmpty()) {
            model.addAttribute(ERROR_MSG, ResultCode.SCAN_INDEX_LIST_MUST_BE_QUERY_WITH_EXPERIMENT_ID.getMessage());
            return "/scanindex/list";
        }

        ResultDO<ExperimentDO> expResult = experimentService.getById(experimentId);
        if (expResult.isFailured()) {
            model.addAttribute(ERROR_MSG, ResultCode.EXPERIMENT_NOT_EXISTED.getMessage());
            return "/scanindex/list";
        }
        model.addAttribute("experiment", expResult.getModel());
        ScanIndexQuery query = new ScanIndexQuery();
        query.setExperimentId(experimentId);
        if (msLevel != null) {
            query.setMsLevel(msLevel);
        }
        if (numStart != null) {
            query.setNumStart(numStart);
        }
        if (numEnd != null) {
            query.setNumEnd(numEnd);
        }
        if (rtStart != null) {
            query.setRtStart(rtStart);
        }
        if (rtEnd != null) {
            query.setRtEnd(rtEnd);
        }

        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        ResultDO<List<ScanIndexDO>> resultDO = scanIndexService.getList(query);

        model.addAttribute("scanIndexList", resultDO.getModel());
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);
        StringBuilder builder = new StringBuilder();
        builder.append("本次搜索耗时:").append(System.currentTimeMillis() - startTime).append("毫秒;包含搜索结果总计:")
                .append(resultDO.getTotalNum()).append("条");
        model.addAttribute("searchResult", builder.toString());
        return "scanindex/list";
    }

    @RequestMapping(value = "/detail/{id}")
    String detail(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        ResultDO<ScanIndexDO> resultDO = scanIndexService.getById(id);
        if (resultDO.isSuccess()) {
            model.addAttribute("scanIndex", resultDO.getModel());
            return "/scanindex/detail";
        } else {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/scanindex/list";
        }
    }
}
