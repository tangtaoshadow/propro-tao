package com.westlake.air.pecs.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.constants.SuccessMsg;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.*;
import com.westlake.air.pecs.domain.db.simple.TransitionGroup;
import com.westlake.air.pecs.domain.query.AnalyseDataQuery;
import com.westlake.air.pecs.domain.query.AnalyseOverviewQuery;
import com.westlake.air.pecs.service.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-19 16:50
 */
@Controller
@RequestMapping("analyse")
public class AnalyseController extends BaseController {

    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    TransitionService transitionService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    RTNormalizerService rtNormalizerService;

    @RequestMapping(value = "/overview/list")
    String overviewList(Model model,
                        @RequestParam(value = "expId", required = false) String expId,
                        @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                        @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {

        model.addAttribute("pageSize", pageSize);
        model.addAttribute("expId", expId);

        if (expId != null) {
            ResultDO<ExperimentDO> expResult = experimentService.getById(expId);
            if (expResult.isFailed()) {
                model.addAttribute(ERROR_MSG, ResultCode.EXPERIMENT_NOT_EXISTED);
                return "/analyse/overview/list";
            }
            model.addAttribute("experiment", expResult.getModel());
        }

        AnalyseOverviewQuery query = new AnalyseOverviewQuery();
        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        if (expId != null) {
            query.setExpId(expId);
        }
        ResultDO<List<AnalyseOverviewDO>> resultDO = analyseOverviewService.getList(query);
        model.addAttribute("overviews", resultDO.getModel());
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);

        return "/analyse/overview/list";
    }

    @RequestMapping(value = "/overview/detail/{id}")
    String overviewDetail(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {

        ResultDO<AnalyseOverviewDO> resultDO = analyseOverviewService.getById(id);

        if (resultDO.isSuccess()) {
            model.addAttribute("overview", resultDO.getModel());
            Long count = analyseDataService.count(new AnalyseDataQuery(id, 2));
            ResultDO<LibraryDO> resLib = libraryService.getById(resultDO.getModel().getSLibraryId());
            if(resLib.isFailed()){
                redirectAttributes.addFlashAttribute(ERROR_MSG, resLib.getMsgInfo());
                return "redirect:/analyse/overview/list";
            }
            model.addAttribute("rate",count+"/"+resLib.getModel().getTotalTargetCount());
            return "/analyse/overview/detail";
        } else {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/analyse/overview/list";
        }
    }

    @RequestMapping(value = "/overview/delete/{id}")
    String overviewDelete(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {

        analyseOverviewService.delete(id);
        analyseDataService.deleteAllByOverviewId(id);
        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.DELETE_SUCCESS);
        return "redirect:/analyse/overview/list";
    }

    @RequestMapping(value = "/data/list")
    String dataList(Model model,
                    @RequestParam(value = "overviewId", required = true) String overviewId,
                    @RequestParam(value = "peptideRef", required = false) String peptideRef,
                    @RequestParam(value = "msLevel", required = false) Integer msLevel,
                    @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                    @RequestParam(value = "pageSize", required = false, defaultValue = "50") Integer pageSize,
                    RedirectAttributes redirectAttributes) {

        model.addAttribute("pageSize", pageSize);
        model.addAttribute("overviewId", overviewId);
        model.addAttribute("msLevel", msLevel);
        model.addAttribute("peptideRef", peptideRef);

        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);
        if (overviewResult.isSuccess()) {
            model.addAttribute("overview", overviewResult.getModel());
        }
        AnalyseDataQuery query = new AnalyseDataQuery();
        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        if (msLevel != null) {
            query.setMsLevel(msLevel);
        }
        if (StringUtils.isNotEmpty(peptideRef)) {
            query.setPeptideRef(peptideRef);
        }
        query.setOverviewId(overviewId);
        ResultDO<List<AnalyseDataDO>> resultDO = analyseDataService.getList(query);
        List<AnalyseDataDO> datas = resultDO.getModel();
        model.addAttribute("datas", datas);
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalNum", resultDO.getTotalNum());

        return "/analyse/data/list";
    }

    @RequestMapping(value = "/data/group")
    String dataGroup(Model model,
                     @RequestParam(value = "overviewId", required = true) String overviewId,
                     @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                     @RequestParam(value = "pageSize", required = false, defaultValue = "50") Integer pageSize,
                     RedirectAttributes redirectAttributes) {

        model.addAttribute("pageSize", pageSize);
        model.addAttribute("overviewId", overviewId);

        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);
        if (overviewResult.isSuccess()) {
            model.addAttribute("overview", overviewResult.getModel());
        }

        ResultDO<List<TransitionGroup>> resultDO = analyseDataService.getTransitionGroup(overviewId, overviewResult.getModel().getVLibraryId(), null);
        List<TransitionGroup> groups = resultDO.getModel();
        model.addAttribute("groups", groups);
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalNum", resultDO.getTotalNum());

        return "/analyse/data/group";
    }

    @RequestMapping(value = "/data/compute")
    @ResponseBody
    String compute(Model model,
                   @RequestParam(value = "overviewId", required = true) String overviewId,
                   @RequestParam(value = "sigma", required = false) Float sigma,
                   @RequestParam(value = "spacing", required = false) Float spacing,
                   RedirectAttributes redirectAttributes) {

        model.addAttribute("overviewId", overviewId);

        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);
        if (overviewResult.isSuccess()) {
            model.addAttribute("overview", overviewResult.getModel());
        }

        AnalyseDataQuery query = new AnalyseDataQuery();
        query.setLibraryId(overviewResult.getModel().getVLibraryId());
        long start = System.currentTimeMillis();
        ResultDO rtResult = rtNormalizerService.compute(overviewId, sigma, spacing);
        System.out.println("Cost:" + (System.currentTimeMillis() - start));
        return JSONArray.toJSONString(rtResult);
    }

    @RequestMapping(value = "/data/vliblist")
    String vlibList(Model model,
                    @RequestParam(value = "overviewId", required = false) String overviewId,
                    @RequestParam(value = "expId", required = false) String expId,
                    RedirectAttributes redirectAttributes) {

        model.addAttribute("overviewId", overviewId);

        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);
        if (overviewResult.isFailed()) {
            model.addAttribute(ERROR_MSG, ResultCode.ANALYSE_OVERVIEW_NOT_EXISTED.getMessage());
            return "/analyse/overview/list?expId=" + expId;
        }

        AnalyseOverviewDO overview = overviewResult.getModel();

        List<TransitionDO> transitionDOList = transitionService.getAllByLibraryId(overview.getVLibraryId());
        List<AnalyseDataDO> datas = new ArrayList<>();
        for (TransitionDO transitionDO : transitionDOList) {
            AnalyseDataDO data = new AnalyseDataDO();
            data.setOverviewId(overviewId);
            data.setPeptideRef(transitionDO.getPeptideRef());
            data.setProteinName(transitionDO.getProteinName());
            data.setAnnotations(transitionDO.getAnnotations());
            data.setMsLevel(2);
            data.setCutInfo(transitionDO.getCutInfo());
            data.setMz(new Float(transitionDO.getProductMz()));

            datas.add(data);
        }
        model.addAttribute("overview", overview);
        model.addAttribute("overviewId", overview.getId());
        model.addAttribute("datas", datas);

        return "/analyse/data/list";
    }

    @RequestMapping(value = "/view")
    @ResponseBody
    ResultDO<JSONObject> view(Model model,
                              @RequestParam(value = "dataId", required = false, defaultValue = "") String dataId,
                              @RequestParam(value = "overviewId", required = false) String overviewId,
                              @RequestParam(value = "peptideRef", required = false) String peptideRef,
                              @RequestParam(value = "cutInfo", required = false) String cutInfo) {
        ResultDO<AnalyseDataDO> dataResult = null;
        if (dataId != null && !dataId.isEmpty() && !dataId.equals("null")) {
            dataResult = analyseDataService.getById(dataId);
        } else if (overviewId != null && peptideRef != null && cutInfo != null) {
            dataResult = analyseDataService.getMS2Data(overviewId, peptideRef, cutInfo);
        } else {
            return ResultDO.buildError(ResultCode.ANALYSE_DATA_NOT_EXISTED);
        }

        ResultDO<JSONObject> resultDO = new ResultDO<>(true);
        if (dataResult.isFailed()) {
            resultDO.setErrorResult(ResultCode.ANALYSE_DATA_NOT_EXISTED);
            return resultDO;
        }

        AnalyseDataDO dataDO = dataResult.getModel();

        JSONObject res = new JSONObject();
        JSONArray rtArray = new JSONArray();
        JSONArray intensityArray = new JSONArray();

        Float[] pairRtArray = dataDO.getRtArray();
        Float[] pairIntensityArray = dataDO.getIntensityArray();
        for (int n = 0; n < pairRtArray.length; n++) {
            rtArray.add(pairRtArray[n]);
            intensityArray.add(pairIntensityArray[n]);
        }

        res.put("rt", rtArray);
        res.put("intensity", intensityArray);
        resultDO.setModel(res);
        return resultDO;
    }

    @RequestMapping(value = "/data/bigview")
    String bigview(Model model,
                   @RequestParam(value = "overviewId", required = true) String overviewId) {
        model.addAttribute("overviewId", overviewId);
        return "analyse/overview/3DView";
    }

    @RequestMapping(value = "/getAllPoints")
    @ResponseBody
    String getAllPoints(Model model,
                        @RequestParam(value = "overviewId", required = true) String overviewId) {

        AnalyseDataQuery query = new AnalyseDataQuery();
        query.setPageSize(-1);
        query.setOverviewId(overviewId);
        query.setMsLevel(2);
        List<AnalyseDataDO> dataList = analyseDataService.getList(query).getModel();
        System.out.println(analyseDataService.count(query));
        List<Float[]> points = new ArrayList<>();
        for (AnalyseDataDO dataDO : dataList) {

            for (int i = 0; i < dataDO.getRtArray().length; i++) {
                Float[] point = new Float[3];
                if (dataDO.getMz() < 400 || dataDO.getMz() > 1600) {
                    continue;
                }
                point[0] = dataDO.getMz();
                point[1] = dataDO.getRtArray()[i];
                if (dataDO.getIntensityArray()[i] == 0) {
                    continue;
                } else {
                    if (dataDO.getIntensityArray()[i] > 5000000) {
                        point[2] = 500f;
                    } else {
                        point[2] = dataDO.getIntensityArray()[i] / 10000;
                    }
                }

                points.add(point);
            }
        }

        ResultDO resultDO = new ResultDO(true);
        resultDO.setModel(points);
        return JSON.toJSONString(resultDO);
    }
}
