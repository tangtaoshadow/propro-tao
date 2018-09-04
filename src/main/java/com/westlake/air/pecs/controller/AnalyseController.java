package com.westlake.air.pecs.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.westlake.air.pecs.async.ScoreTask;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.constants.SuccessMsg;
import com.westlake.air.pecs.constants.TaskTemplate;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.*;
import com.westlake.air.pecs.domain.db.simple.TransitionGroup;
import com.westlake.air.pecs.domain.query.AnalyseDataQuery;
import com.westlake.air.pecs.domain.query.AnalyseOverviewQuery;
import com.westlake.air.pecs.domain.query.PageQuery;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
    ScoreTask scoreTask;

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
            ResultDO<LibraryDO> resLib = libraryService.getById(resultDO.getModel().getLibraryId());
            if (resLib.isFailed()) {
                redirectAttributes.addFlashAttribute(ERROR_MSG, resLib.getMsgInfo());
                return "redirect:/analyse/overview/list";
            }
            model.addAttribute("rate", count + "/" + resLib.getModel().getTotalTargetCount());

            model.addAttribute("slopeIntercept", resultDO.getModel().getSlope() + "/" + resultDO.getModel().getIntercept());
            return "/analyse/overview/detail";
        } else {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/analyse/overview/list";
        }
    }

    @RequestMapping(value = "/overview/export/{id}")
    String overviewExport(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) throws IOException {

        int pageSize = 100;
        AnalyseDataQuery query = new AnalyseDataQuery(id, 2);
        int count = analyseDataService.count(query).intValue();
        int totalPage = count % pageSize == 0 ? count / pageSize : (count / pageSize + 1);
        File file = new File("D://test.json");
        if (!file.exists()) {
            file.createNewFile();
        }
        OutputStream os = new FileOutputStream(file);

        byte[] changeLine = "\n".getBytes();
        query.setPageSize(pageSize);
        query.setOverviewId(id);
        for (int i = 1; i <= totalPage; i++) {
            query.setPageNo(i);
            ResultDO<List<AnalyseDataDO>> dataListRes = analyseDataService.getList(query);

            String content = JSONArray.toJSONString(dataListRes.getModel());
            byte[] b = content.getBytes();
            int l = b.length;

            os.write(b, 0, l);
            logger.info("打印第"+i+"/"+totalPage+"行,本行长度:"+l+";");
            os.write(changeLine,0,changeLine.length);
        }
        os.close();
        return "redirect:/analyse/overview/list";

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
                     @RequestParam(value = "libraryId", required = true) String libraryId,
                     @RequestParam(value = "isIrt", required = true) Boolean isIrt,
                     RedirectAttributes redirectAttributes) {

        model.addAttribute("overviewId", overviewId);
        model.addAttribute("libraryId", libraryId);
        model.addAttribute("isIrt", isIrt);

        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);
        if (overviewResult.isSuccess()) {
            model.addAttribute("overview", overviewResult.getModel());
        }

        List<TransitionGroup> groups = null;
        if(isIrt){
            groups = analyseDataService.getIrtTransitionGroup(overviewId,libraryId);
        }else{
            groups = analyseDataService.getTransitionGroup(overviewResult.getModel());
            groups = groups.subList(0,100);
        }

        model.addAttribute("groups", groups);

        return "/analyse/data/group";
    }

    @RequestMapping(value = "/overview/score")
    String score(Model model,
                 @RequestParam(value = "overviewId", required = true) String overviewId,
                 @RequestParam(value = "iRtLibraryId", required = false) String iRtLibraryId,
                 @RequestParam(value = "sigma", required = false) Float sigma,
                 @RequestParam(value = "spacing", required = false) Float spacing,
                 RedirectAttributes redirectAttributes) {

        model.addAttribute("sigma", sigma);
        model.addAttribute("spacing", spacing);
        model.addAttribute("iRtLibraryId", iRtLibraryId);

        ResultDO<AnalyseOverviewDO> resultDO = analyseOverviewService.getById(overviewId);
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.ANALYSE_OVERVIEW_NOT_EXISTED.getMessage());
            return "redirect:/analyse/overview/list";
        }

        model.addAttribute("iRtLibraries", getLibraryList(1));
        model.addAttribute("overview", resultDO.getModel());

        return "/analyse/overview/score";
    }

    @RequestMapping(value = "/overview/doscore")
    String doscore(Model model,
                   @RequestParam(value = "overviewId", required = true) String overviewId,
                   @RequestParam(value = "iRtLibraryId", required = false) String iRtLibraryId,
                   @RequestParam(value = "sigma", required = false) Float sigma,
                   @RequestParam(value = "spacing", required = false) Float spacing,
                   RedirectAttributes redirectAttributes) {

        model.addAttribute("overviewId", overviewId);
        model.addAttribute("iRtLibraryId", iRtLibraryId);
        model.addAttribute("sigma", sigma);
        model.addAttribute("spacing", spacing);

        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);
        if (overviewResult.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.ANALYSE_OVERVIEW_NOT_EXISTED.getMessage());
            return "redirect:/analyse/overview/list";
        }
        ResultDO<LibraryDO> iRtLibRes = libraryService.getById(iRtLibraryId);
        if (iRtLibRes.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.LIBRARY_NOT_EXISTED.getMessage());
            return "redirect:/analyse/overview/list";
        }

        AnalyseOverviewDO overviewDO = overviewResult.getModel();
        overviewDO.setIRtLibraryId(iRtLibRes.getModel().getId());
        overviewDO.setIRtLibraryName(iRtLibRes.getModel().getName());
        analyseOverviewService.update(overviewDO);
        model.addAttribute("overview", overviewDO);

        TaskDO taskDO = new TaskDO(TaskTemplate.SCORE, overviewDO.getName());
        taskService.insert(taskDO);

        scoreTask.score(overviewId, sigma, spacing, taskDO);

        return "redirect:/task/detail/" + taskDO.getId();
    }

    @RequestMapping(value = "/data/irtliblist")
    String iRtLibList(Model model,
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

        List<TransitionDO> transitionDOList = transitionService.getAllByLibraryId(overview.getIRtLibraryId());
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
}
