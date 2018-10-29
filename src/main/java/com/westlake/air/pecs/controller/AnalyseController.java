package com.westlake.air.pecs.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.westlake.air.pecs.algorithm.Airus;
import com.westlake.air.pecs.async.ScoreTask;
import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.constants.SuccessMsg;
import com.westlake.air.pecs.constants.TaskTemplate;
import com.westlake.air.pecs.dao.ConfigDAO;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.airus.FinalResult;
import com.westlake.air.pecs.domain.bean.analyse.RtIntensityPairsDouble;
import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.pecs.domain.bean.score.FeatureScores;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.*;
import com.westlake.air.pecs.domain.db.simple.TransitionGroup;
import com.westlake.air.pecs.domain.query.AnalyseDataQuery;
import com.westlake.air.pecs.domain.query.AnalyseOverviewQuery;
import com.westlake.air.pecs.feature.GaussFilter;
import com.westlake.air.pecs.feature.SignalToNoiseEstimator;
import com.westlake.air.pecs.service.AnalyseDataService;
import com.westlake.air.pecs.service.AnalyseOverviewService;
import com.westlake.air.pecs.service.ExperimentService;
import com.westlake.air.pecs.service.TransitionService;
import com.westlake.air.pecs.utils.AirusUtil;
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
    @Autowired
    Airus airus;
    @Autowired
    GaussFilter gaussFilter;
    @Autowired
    SignalToNoiseEstimator signalToNoiseEstimator;
    @Autowired
    ConfigDAO configDAO;

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
        model.addAttribute("scores", FeatureScores.ScoreType.getUsedTypes());
        return "/analyse/overview/list";
    }

    @RequestMapping(value = "/overview/detail/{id}")
    String overviewDetail(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {

        ResultDO<AnalyseOverviewDO> resultDO = analyseOverviewService.getById(id);

        if (resultDO.isSuccess()) {
            model.addAttribute("overview", resultDO.getModel());
            AnalyseDataQuery query = new AnalyseDataQuery(id, 2);
            query.setIsDecoy(true);
            Long decoyCount = analyseDataService.count(query);
            query.setIsDecoy(false);
            Long realCount = analyseDataService.count(query);
            ResultDO<LibraryDO> resLib = libraryService.getById(resultDO.getModel().getLibraryId());
            if (resLib.isFailed()) {
                model.addAttribute(ERROR_MSG, resLib.getMsgInfo());
                model.addAttribute("rate", decoyCount + "/" + realCount);
            }else{
                model.addAttribute("rate", decoyCount + "/" + realCount + "/" + resLib.getModel().getTotalTargetCount());
            }

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
            logger.info("打印第" + i + "/" + totalPage + "行,本行长度:" + l + ";");
            os.write(changeLine, 0, changeLine.length);
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
        if (isIrt) {
            groups = analyseDataService.getIrtTransitionGroup(overviewId, libraryId);
        } else {
            groups = analyseDataService.getTransitionGroup(overviewResult.getModel());
            groups = groups.subList(0, 100);
        }

        model.addAttribute("groups", groups);

        return "/analyse/data/group";
    }

    @RequestMapping(value = "/overview/score")
    String score(Model model,
                 @RequestParam(value = "overviewId", required = true) String overviewId,
                 @RequestParam(value = "sigma", required = false) Float sigma,
                 @RequestParam(value = "spacing", required = false) Float spacing,
                 RedirectAttributes redirectAttributes) {

        model.addAttribute("sigma", sigma);
        model.addAttribute("spacing", spacing);

        ResultDO<AnalyseOverviewDO> resultDO = analyseOverviewService.getById(overviewId);
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.ANALYSE_OVERVIEW_NOT_EXISTED.getMessage());
            return "redirect:/analyse/overview/list";
        }

        AnalyseOverviewDO overviewDO = resultDO.getModel();
        model.addAttribute("overview", overviewDO);
        model.addAttribute("slope", overviewDO.getSlope());
        model.addAttribute("intercept", overviewDO.getIntercept());
        return "/analyse/overview/score";
    }

    @RequestMapping(value = "/overview/doscore")
    String doscore(Model model,
                   @RequestParam(value = "overviewId", required = true) String overviewId,
                   @RequestParam(value = "slope", required = false) Float slope,
                   @RequestParam(value = "intercept", required = false) Float intercept,
                   @RequestParam(value = "sigma", required = false) Float sigma,
                   @RequestParam(value = "spacing", required = false) Float spacing,
                   RedirectAttributes redirectAttributes) {

        model.addAttribute("overviewId", overviewId);
        model.addAttribute("sigma", sigma);
        model.addAttribute("spacing", spacing);
        model.addAttribute("slope", slope);
        model.addAttribute("intercept", intercept);

        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);
        if (overviewResult.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.ANALYSE_OVERVIEW_NOT_EXISTED.getMessage());
            return "redirect:/analyse/overview/list";
        }

        AnalyseOverviewDO overviewDO = overviewResult.getModel();
        model.addAttribute("overview", overviewDO);

        TaskDO taskDO = new TaskDO(TaskTemplate.SCORE, overviewDO.getName());
        taskService.insert(taskDO);

        SlopeIntercept si = new SlopeIntercept();
        if (slope == null || intercept == null) {
            si = SlopeIntercept.create();
        } else {
            si.setSlope(slope);
            si.setIntercept(intercept);
        }
        scoreTask.score(overviewId, si, overviewDO.getLibraryId(), new SigmaSpacing(sigma, spacing), taskDO);
        return "redirect:/task/detail/" + taskDO.getId();
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

    @RequestMapping(value = "/viewGroup")
    @ResponseBody
    ResultDO<JSONObject> viewGroup(Model model,
                                   @RequestParam(value = "overviewId", required = false) String overviewId,
                                   @RequestParam(value = "isDecoy", required = false) Boolean isDecoy,
                                   @RequestParam(value = "peptideRef", required = false) String peptideRef,
                                   @RequestParam(value = "isGaussFilter", required = false, defaultValue = "false") Boolean isGaussFilter,
                                   @RequestParam(value = "useNoise1000", required = false, defaultValue = "false") Boolean useNoise1000
                                   ) {
        ResultDO<List<AnalyseDataDO>> dataResult = null;
        if (overviewId != null && peptideRef != null) {
            dataResult = analyseDataService.getMS2DataList(overviewId, peptideRef, isDecoy);
        } else {
            return ResultDO.buildError(ResultCode.ANALYSE_DATA_NOT_EXISTED);
        }

        ResultDO<JSONObject> resultDO = new ResultDO<>(true);
        if (dataResult.isFailed()) {
            resultDO.setErrorResult(ResultCode.ANALYSE_DATA_NOT_EXISTED);
            return resultDO;
        }

        List<AnalyseDataDO> dataList = dataResult.getModel();

        JSONObject res = new JSONObject();
        JSONArray rtArray = new JSONArray();
        JSONArray intensityArrays = new JSONArray();
        JSONArray cutInfoArray = new JSONArray();

        //同一组的rt坐标是相同的
        Float[] pairRtArray = null;

        for(AnalyseDataDO data : dataList){
            if(!data.getIsHit()){
                continue;
            }
            if(pairRtArray == null){
                pairRtArray = data.getRtArray();
            }
            Float[] pairIntensityArray = null;
            if(isGaussFilter){
                pairIntensityArray = gaussFilter.filterForFloat(data);
            }else{
                pairIntensityArray = data.getIntensityArray();
            }

            if(useNoise1000){
                double[] noisePairIntensityArray = signalToNoiseEstimator.computeSTN(new RtIntensityPairsDouble(data.getRtArray(), pairIntensityArray), 1000, 30);
                JSONArray noiseIntensityArray = new JSONArray();
                for (int i = 0; i < noisePairIntensityArray.length; i++) {
                    if(noisePairIntensityArray[i] >= Constants.SIGNAL_TO_NOISE_LIMIT){
                        noiseIntensityArray.add(pairIntensityArray[i]);
                    }else{
                        noiseIntensityArray.add(0);
                    }
                }
                cutInfoArray.add(data.getCutInfo());
                intensityArrays.add(noiseIntensityArray);
            }else{
                JSONArray intensityArray = new JSONArray();
                for (int i = 0; i < pairIntensityArray.length; i++) {
                    intensityArray.add(pairIntensityArray[i]);
                }
                cutInfoArray.add(data.getCutInfo());
                intensityArrays.add(intensityArray);
            }
        }

        if(pairRtArray != null){
            for (int n = 0; n < pairRtArray.length; n++) {
                rtArray.add(pairRtArray[n]);
            }
        }else{
            logger.error("No AnalyseData Has RtArray!!!");
        }
        res.put("rt", rtArray);
        res.put("cutInfoArray", cutInfoArray);
        res.put("intensityArrays", intensityArrays);
        resultDO.setModel(res);
        return resultDO;
    }

    @RequestMapping(value = "/overview/airus/{overviewId}")
    @ResponseBody
    String airus(Model model,
                 @PathVariable("overviewId") String overviewId) {

        long start = System.currentTimeMillis();
        FinalResult finalResult = airus.doAirus(overviewId);
        logger.info("打分耗时:" + (System.currentTimeMillis() - start));
        int count = AirusUtil.checkFdr(finalResult);

        logger.info(JSON.toJSONString(finalResult.getAllInfo().getStatMetrics().getFdr()));
        JSONObject object = new JSONObject();
        object.put("子分数种类",finalResult.getClassifierTable().size());
        object.put("权重",finalResult.getClassifierTable());
        object.put("识别肽段数目",count);
        return object.toJSONString();
    }
}
