package com.westlake.air.propro.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.westlake.air.propro.algorithm.extract.Extractor;
import com.westlake.air.propro.algorithm.feature.ChromatographicScorer;
import com.westlake.air.propro.algorithm.feature.LibraryScorer;
import com.westlake.air.propro.algorithm.formula.FragmentFactory;
import com.westlake.air.propro.algorithm.peak.FeatureExtractor;
import com.westlake.air.propro.algorithm.peak.GaussFilter;
import com.westlake.air.propro.algorithm.peak.SignalToNoiseEstimator;
import com.westlake.air.propro.async.ScoreTask;
import com.westlake.air.propro.constants.*;
import com.westlake.air.propro.dao.ConfigDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.analyse.ComparisonResult;
import com.westlake.air.propro.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.propro.domain.bean.score.FeatureScores;
import com.westlake.air.propro.domain.bean.score.PeakGroup;
import com.westlake.air.propro.domain.bean.score.PeptideFeature;
import com.westlake.air.propro.domain.bean.score.SlopeIntercept;
import com.westlake.air.propro.domain.db.*;
import com.westlake.air.propro.domain.db.simple.TargetPeptide;
import com.westlake.air.propro.domain.params.LumsParams;
import com.westlake.air.propro.domain.query.AnalyseDataQuery;
import com.westlake.air.propro.domain.query.AnalyseOverviewQuery;
import com.westlake.air.propro.service.*;
import com.westlake.air.propro.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import scala.tools.nsc.backend.jvm.BTypes;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

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
    PeptideService peptideService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    ProjectService projectService;
    @Autowired
    ScoreService scoreService;
    @Autowired
    ScoreTask scoreTask;
    @Autowired
    GaussFilter gaussFilter;
    @Autowired
    SignalToNoiseEstimator signalToNoiseEstimator;
    @Autowired
    ConfigDAO configDAO;
    @Autowired
    FragmentFactory fragmentFactory;
    @Autowired
    FeatureExtractor featureExtractor;
    @Autowired
    ChromatographicScorer chromatographicScorer;
    @Autowired
    LibraryScorer libraryScorer;
    @Autowired
    Extractor extractor;

    @RequestMapping(value = "/overview/list")
    String overviewList(Model model,
                        @RequestParam(value = "expId", required = false) String expId,
                        @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                        @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {

        model.addAttribute("pageSize", pageSize);
        model.addAttribute("expId", expId);

        if (StringUtils.isNotEmpty(expId)) {
            ResultDO<ExperimentDO> expResult = experimentService.getById(expId);
            if (expResult.isFailed()) {
                model.addAttribute(ERROR_MSG, ResultCode.EXPERIMENT_NOT_EXISTED);
                return "analyse/overview/list";
            }
            PermissionUtil.check(expResult.getModel());
            model.addAttribute("experiment", expResult.getModel());
        }

        AnalyseOverviewQuery query = new AnalyseOverviewQuery();
        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        if (StringUtils.isNotEmpty(expId)) {
            query.setExpId(expId);
        }
        if(!isAdmin()){
            query.setOwnerName(getCurrentUsername());
        }
        query.setOrderBy(Sort.Direction.DESC);
        query.setSortColumn("createDate");
        ResultDO<List<AnalyseOverviewDO>> resultDO = analyseOverviewService.getList(query);
        model.addAttribute("overviews", resultDO.getModel());
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("scores", ScoreType.getUsedTypes());
        return "analyse/overview/list";
    }

    @RequestMapping(value = "/overview/detail/{id}")
    String overviewDetail(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {

        ResultDO<AnalyseOverviewDO> resultDO = analyseOverviewService.getById(id);

        if (resultDO.isSuccess()) {
            PermissionUtil.check(resultDO.getModel());
            model.addAttribute("overview", resultDO.getModel());
            model.addAttribute("slopeIntercept", resultDO.getModel().getSlope() + "/" + resultDO.getModel().getIntercept());
            return "analyse/overview/detail";
        } else {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/analyse/overview/list";
        }
    }

    @RequestMapping(value = "/overview/export/{id}")
    String overviewExport(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) throws IOException {

        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(id);

        if (overviewResult.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.ANALYSE_OVERVIEW_NOT_EXISTED);
            return "redirect:/analyse/overview/list";
        }
        PermissionUtil.check(overviewResult.getModel());
        ResultDO<ExperimentDO> experimentResult = experimentService.getById(overviewResult.getModel().getExpId());
        if (experimentResult.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.EXPERIMENT_NOT_EXISTED);
            return "redirect:/analyse/overview/list";
        }
        ProjectDO project = projectService.getByName(experimentResult.getModel().getProjectName());
        if (project == null) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.PROJECT_NOT_EXISTED);
            return "redirect:/analyse/overview/list";
        }
        String exportPath = project.getExportPath();

        int pageSize = 1000;
        AnalyseDataQuery query = new AnalyseDataQuery(id);
        query.setIsDecoy(false);
        query.setFdrEnd(0.01);
        query.setSortColumn("fdr");
        query.setOrderBy(Sort.Direction.ASC);
        int count = analyseDataService.count(query).intValue();
        int totalPage = count % pageSize == 0 ? count / pageSize : (count / pageSize + 1);

        File file = new File(exportPath + overviewResult.getModel().getName() + "[" + overviewResult.getModel().getId() + "].txt");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }

        String header = "protein,peptideRef,intensity";
        String content = header + "\n";

        OutputStream os = new FileOutputStream(file);
        query.setPageSize(pageSize);
        for (int i = 1; i <= totalPage; i++) {
            query.setPageNo(i);
            ResultDO<List<AnalyseDataDO>> dataListRes = analyseDataService.getList(query);
            for (AnalyseDataDO analyseData : dataListRes.getModel()) {
                String line = analyseData.getProteinName() + "," + analyseData.getPeptideRef() + "," + analyseData.getIntensitySum().longValue() + "\n";
                content += line;
            }
            byte[] b = content.getBytes();
            int l = b.length;

            os.write(b, 0, l);
            logger.info("打印第" + i + "/" + totalPage + "页,本页长度:" + l + ";");
        }
        os.close();
        return "redirect:/analyse/overview/list";

    }

    @RequestMapping(value = "/overview/delete/{id}")
    String overviewDelete(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {

        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(id);
        PermissionUtil.check(overviewResult.getModel());

        analyseOverviewService.delete(id);
        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.DELETE_SUCCESS);
        return "redirect:/analyse/overview/list";
    }

    @RequestMapping(value = "/overview/select")
    String overviewSelect(Model model, RedirectAttributes redirectAttributes) {
        return "analyse/overview/select";
    }

    @RequestMapping(value = "/overview/comparison")
    String overviewComparison(Model model,
                              @RequestParam(value = "overviewIdA", required = false) String overviewIdA,
                              @RequestParam(value = "overviewIdB", required = false) String overviewIdB,
                              @RequestParam(value = "overviewIdC", required = false) String overviewIdC,
                              @RequestParam(value = "overviewIdD", required = false) String overviewIdD,
                              RedirectAttributes redirectAttributes) {

        HashSet<String> overviewIds = new HashSet<>();
        if (StringUtils.isNotEmpty(overviewIdA)) {
            model.addAttribute("overviewIdA", overviewIdA);
            overviewIds.add(overviewIdA);
        }
        if (StringUtils.isNotEmpty(overviewIdB)) {
            model.addAttribute("overviewIdB", overviewIdB);
            overviewIds.add(overviewIdB);
        }
        if (StringUtils.isNotEmpty(overviewIdC)) {
            model.addAttribute("overviewIdC", overviewIdC);
            overviewIds.add(overviewIdC);
        }
        if (StringUtils.isNotEmpty(overviewIdD)) {
            model.addAttribute("overviewIdD", overviewIdD);
            overviewIds.add(overviewIdD);
        }

        if (overviewIds.size() <= 1) {
            Map<String, Object> map = model.asMap();
            for (String key : map.keySet()) {
                redirectAttributes.addFlashAttribute(key, map.get(key));
                redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.COMPARISON_OVERVIEW_IDS_MUST_BIGGER_THAN_TWO.getMessage());
            }
            return "redirect:/analyse/overview/select";
        }

        List<AnalyseOverviewDO> overviews = new ArrayList<>();
        for (String overviewId : overviewIds) {
            ResultDO<AnalyseOverviewDO> tempResult = analyseOverviewService.getById(overviewId);
            PermissionUtil.check(tempResult.getModel());
            overviews.add(tempResult.getModel());
        }

        ComparisonResult result = analyseOverviewService.comparison(overviews);
        model.addAttribute("samePeptides", result.getSamePeptides());
        model.addAttribute("diffPeptides", result.getDiffPeptides());
        model.addAttribute("identifiesMap", result.getIdentifiesMap());
        return "analyse/overview/comparison";
    }

    @RequestMapping(value = "/data/list")
    String dataList(Model model,
                    @RequestParam(value = "overviewId", required = true) String overviewId,
                    @RequestParam(value = "peptideRef", required = false) String peptideRef,
                    @RequestParam(value = "msLevel", required = false) String msLevel,
                    @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                    @RequestParam(value = "pageSize", required = false, defaultValue = "50") Integer pageSize,
                    RedirectAttributes redirectAttributes) {

        model.addAttribute("pageSize", pageSize);
        model.addAttribute("overviewId", overviewId);
        model.addAttribute("msLevel", msLevel);
        model.addAttribute("peptideRef", peptideRef);

        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);

        PermissionUtil.check(overviewResult.getModel());
        model.addAttribute("overview", overviewResult.getModel());

        AnalyseDataQuery query = new AnalyseDataQuery();
        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        if (msLevel != null && !msLevel.equals("All")) {
            try {
                query.setMsLevel(Integer.parseInt(msLevel));
            } catch (Exception e) {
                logger.error("msLevel必须为1或者2");
            }
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

        return "analyse/data/list";
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
        PermissionUtil.check(overviewDO);
        model.addAttribute("overview", overviewDO);
        model.addAttribute("slope", overviewDO.getSlope());
        model.addAttribute("intercept", overviewDO.getIntercept());
        model.addAttribute("scoreTypes", ScoreType.getShownTypes());
        return "analyse/overview/score";
    }

    @RequestMapping(value = "/overview/doscore")
    String doscore(Model model,
                   @RequestParam(value = "overviewId", required = true) String overviewId,
                   @RequestParam(value = "slope", required = false) Double slope,
                   @RequestParam(value = "intercept", required = false) Double intercept,
                   @RequestParam(value = "sigma", required = false) Float sigma,
                   @RequestParam(value = "spacing", required = false) Float spacing,
                   HttpServletRequest request,
                   RedirectAttributes redirectAttributes) {

        model.addAttribute("overviewId", overviewId);
        model.addAttribute("sigma", sigma);
        model.addAttribute("spacing", spacing);
        model.addAttribute("slope", slope);
        model.addAttribute("intercept", intercept);

        List<String> scoreTypes = ScoreUtil.getScoreTypes(request);
        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);
        if (overviewResult.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.ANALYSE_OVERVIEW_NOT_EXISTED.getMessage());
            return "redirect:/analyse/overview/list";
        }

        AnalyseOverviewDO overviewDO = overviewResult.getModel();
        PermissionUtil.check(overviewDO);
        model.addAttribute("overview", overviewDO);

        ResultDO<ExperimentDO> expResult = experimentService.getById(overviewDO.getExpId());
        if (expResult.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.EXPERIMENT_NOT_EXISTED.getMessage());
            return "redirect:/experiment/list";
        }

        TaskDO taskDO = new TaskDO(TaskTemplate.SCORE, overviewDO.getName());
        taskService.insert(taskDO);

        SlopeIntercept si = new SlopeIntercept();
        if (slope == null || intercept == null) {
            si = SlopeIntercept.create();
        } else {
            si.setSlope(slope);
            si.setIntercept(intercept);
        }

        scoreTask.score(overviewId, expResult.getModel(), si, overviewDO.getLibraryId(), new SigmaSpacing(sigma, spacing), scoreTypes, taskDO);
        return "redirect:/task/detail/" + taskDO.getId();
    }

    @RequestMapping(value = "/view")
    @ResponseBody
    ResultDO<JSONObject> view(Model model,
                              @RequestParam(value = "dataId", required = false, defaultValue = "") String dataId,
                              @RequestParam(value = "cutInfo", required = false) String cutInfo) {
        ResultDO<AnalyseDataDO> dataResult = null;
        ResultDO<JSONObject> resultDO = new ResultDO<>(true);
        if (dataId != null && !dataId.isEmpty() && !dataId.equals("null")) {
            dataResult = analyseDataService.getById(dataId);
        } else {
            resultDO.setErrorResult(ResultCode.ANALYSE_DATA_ID_CANNOT_BE_EMPTY);
            return resultDO;
        }

        if (dataResult.isFailed()) {
            resultDO.setErrorResult(ResultCode.ANALYSE_DATA_NOT_EXISTED);
            return resultDO;
        }

        AnalyseDataDO dataDO = dataResult.getModel();

        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(dataDO.getOverviewId());
        PermissionUtil.check(overviewResult.getModel());

        JSONObject res = new JSONObject();
        JSONArray rtArray = new JSONArray();
        JSONArray intensityArray = new JSONArray();

        AnalyseUtil.decompress(dataDO);
        Float[] pairRtArray = dataDO.getRtArray();
        Float[] pairIntensityArray = dataDO.getIntensityMap().get(cutInfo);

        for (int n = 0; n < pairRtArray.length; n++) {
            rtArray.add(pairRtArray[n]);
            if (pairIntensityArray != null) {
                intensityArray.add(pairIntensityArray[n]);
            }
        }

        res.put("rt", rtArray);
        res.put("intensity", intensityArray);
        res.put("peptideRef", dataDO.getPeptideRef());
        resultDO.setModel(res);
        return resultDO;
    }

    @RequestMapping(value = "/viewGroup")
    @ResponseBody
    ResultDO<JSONObject> viewGroup(Model model,
                                   @RequestParam(value = "dataId", required = false) String dataId,
                                   @RequestParam(value = "isGaussFilter", required = false, defaultValue = "false") Boolean isGaussFilter,
                                   @RequestParam(value = "useNoise1000", required = false, defaultValue = "false") Boolean useNoise1000) {
        ResultDO<AnalyseDataDO> dataResult = null;
        if (dataId != null && !dataId.isEmpty() && !dataId.equals("null")) {
            dataResult = analyseDataService.getById(dataId);
        }

        ResultDO<JSONObject> resultDO = new ResultDO<>(true);
        if (dataResult == null || dataResult.isFailed()) {
            resultDO.setErrorResult(ResultCode.ANALYSE_DATA_NOT_EXISTED);
            return resultDO;
        }

        AnalyseDataDO data = dataResult.getModel();
        ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(data.getOverviewId());
        PermissionUtil.check(overviewResult.getModel());

        JSONObject res = new JSONObject();
        JSONArray rtArray = new JSONArray();
        JSONArray intensityArrays = new JSONArray();
        JSONArray cutInfoArray = new JSONArray();

        //同一组的rt坐标是相同的
        AnalyseUtil.decompress(data);
        Float[] pairRtArray = data.getRtArray();
        for (String cutInfo : data.getIntensityMap().keySet()) {
            if (data.getIntensityMap().get(cutInfo) == null) {
                continue;
            }

            Float[] pairIntensityArray = data.getIntensityMap().get(cutInfo);
            if (isGaussFilter) {
                AnalyseOverviewDO overview = overviewResult.getModel();
                pairIntensityArray = gaussFilter.filterForFloat(pairRtArray, cutInfo, pairIntensityArray,new SigmaSpacing(overview.getSigma(), overview.getSpacing()));
            }

            cutInfoArray.add(cutInfo);
            JSONArray intensityArray = new JSONArray();
            if (useNoise1000) {
                intensityArray = noise(pairRtArray, pairIntensityArray);
                intensityArrays.add(intensityArray);
            } else {
                intensityArray.addAll(Arrays.asList(pairIntensityArray));
                intensityArrays.add(intensityArray);
            }
        }

        if (pairRtArray != null) {
            rtArray.addAll(Arrays.asList(pairRtArray));
        } else {
            logger.error("No AnalyseData Has RtArray!!!");
        }

        JSONArray leftRtList = new JSONArray();
        JSONArray rightRtList = new JSONArray();
        for(FeatureScores scores :data.getFeatureScoresList()){
            Pair<Double, Double> rtRange = FeatureUtil.toDoublePair(scores.getRtRangeFeature());
            leftRtList.add(rtRange.getLeft());
            rightRtList.add(rtRange.getRight());
        }
        res.put("leftRtList", leftRtList);
        res.put("rightRtList", rightRtList);
        res.put("rt", rtArray);
        res.put("peptideRef", data.getPeptideRef());
        res.put("cutInfoArray", cutInfoArray);
        res.put("intensityArrays", intensityArrays);
        if (data.getBestRt() != null) {
            res.put("bestRt", (double) Math.round(data.getBestRt() * 100) / 100);
        }

        resultDO.setModel(res);
        return resultDO;
    }

    @RequestMapping(value = "/consultation")
    String consultation(Model model,
                        @RequestParam(value = "dataId", required = false) String dataId,
                        @RequestParam(value = "peptideRef", required = false) String peptideRef,
                        @RequestParam(value = "expId", required = false) String expId,
                        @RequestParam(value = "libraryId", required = false) String libraryId,
                        @RequestParam(value = "useGaussFilter", required = false, defaultValue = "false") Boolean useGaussFilter,
                        @RequestParam(value = "sigma", required = false, defaultValue = "3.75") Float sigma,
                        @RequestParam(value = "spacing", required = false, defaultValue = "0.01") Float spacing,
                        @RequestParam(value = "useNoise", required = false, defaultValue = "false") Boolean useNoise,
                        @RequestParam(value = "noise", required = false, defaultValue = "1000") Integer noise,
                        @RequestParam(value = "mzExtractWindow", required = false, defaultValue = "0.05") Float mzExtractWindow,
                        @RequestParam(value = "rtExtractWindow", required = false, defaultValue = "800") Float rtExtractWindow,
                        @RequestParam(value = "allCutInfo", required = false, defaultValue = "false") Boolean allCutInfo,
                        @RequestParam(value = "noUseForFill", required = false, defaultValue = "false") Boolean noUseForFill,
                        @RequestParam(value = "noUseForLib", required = false, defaultValue = "false") Boolean noUseForLib,
                        @RequestParam(value = "limitLength", required = false, defaultValue = "3") Integer limitLength,
                        HttpServletRequest request) {

        model.addAttribute("dataId", dataId);
        model.addAttribute("peptideRef", peptideRef);
        model.addAttribute("expId", expId);
        model.addAttribute("libraryId", libraryId);
        model.addAttribute("useGaussFilter", useGaussFilter);
        model.addAttribute("sigma", sigma);
        model.addAttribute("spacing", spacing);
        model.addAttribute("useNoise", useNoise);
        model.addAttribute("noise", noise);
        model.addAttribute("mzExtractWindow", mzExtractWindow);
        model.addAttribute("rtExtractWindow", rtExtractWindow);
        model.addAttribute("allCutInfo", allCutInfo);
        model.addAttribute("noUseForFill", noUseForFill);
        model.addAttribute("noUseForLib", noUseForLib);
        model.addAttribute("limitLength", limitLength);
        AnalyseDataDO data = null;
        ResultDO<AnalyseOverviewDO> overviewResult = null;
        ResultDO<ExperimentDO> experimentResult = null;
        LibraryDO library = null;
        HashSet<String> usedCutInfos = new HashSet<>();

        String targetExpId = null;
        String targetLibraryId = null;
        String targetPeptideRef = null;
        //如果dataId不为空,则优先使用dataId作为查询参数
        if (dataId != null && !dataId.isEmpty()) {
            ResultDO<AnalyseDataDO> dataResult = analyseDataService.getById(dataId);
            if (dataResult.isFailed()) {
                model.addAttribute(ERROR_MSG, ResultCode.ANALYSE_DATA_NOT_EXISTED.getMessage());
                return "analyse/data/consultation";
            }
            data = dataResult.getModel();
            overviewResult = analyseOverviewService.getById(data.getOverviewId());
            if (overviewResult.isFailed()) {
                model.addAttribute(ERROR_MSG, ResultCode.ANALYSE_OVERVIEW_NOT_EXISTED.getMessage());
                return "analyse/data/consultation";
            }
            //校验权限
            PermissionUtil.check(overviewResult.getModel());
            targetExpId = overviewResult.getModel().getExpId();
            targetLibraryId = overviewResult.getModel().getLibraryId();
            targetPeptideRef = data.getPeptideRef();
            model.addAttribute("libraryId", overviewResult.getModel().getLibraryId());
            model.addAttribute("expId", overviewResult.getModel().getExpId());
        } else if (StringUtils.isNotEmpty(peptideRef) && StringUtils.isNotEmpty(expId) && StringUtils.isNotEmpty(libraryId)) {
            //如果dataId为空,则使用peptideRef,expId,libraryId作为查询条件
            targetExpId = expId;
            targetLibraryId = libraryId;
            targetPeptideRef = peptideRef;
        } else {
            model.addAttribute(ERROR_MSG, ResultCode.PARAMS_NOT_ENOUGH.getMessage());
            return "analyse/data/consultation";
        }

        //检测原始实验是否已经被转化为Aird压缩文件,是否执行了IRT计算
        experimentResult = experimentService.getById(targetExpId);
        if (experimentResult.isFailed()) {
            model.addAttribute(ERROR_MSG, ResultCode.EXPERIMENT_NOT_EXISTED.getMessage());
            return "analyse/data/consultation";
        }
        if (experimentResult.getModel().getIrtResult() == null) {
            model.addAttribute(ERROR_MSG, ResultCode.IRT_FIRST.getMessage());
            return "analyse/data/consultation";
        }
        if (experimentResult.getModel().getAirdPath() == null || experimentResult.getModel().getAirdPath().isEmpty()) {
            model.addAttribute(ERROR_MSG, ResultCode.AIRD_COMPRESSION_FIRST.getMessage());
            return "analyse/data/consultation";
        }
        //校验权限
        PermissionUtil.check(experimentResult.getModel());

        library = libraryService.getById(targetLibraryId);
        if (library == null) {
            model.addAttribute(ERROR_MSG, ResultCode.LIBRARY_NOT_EXISTED.getMessage());
            return "analyse/data/consultation";
        }

        //覆盖之前的参数
        model.addAttribute("peptideRef", targetPeptideRef);
        model.addAttribute("expId", targetExpId);
        model.addAttribute("libraryId", targetLibraryId);
        model.addAttribute("libraryName", library.getName());

        model.addAttribute("exp", experimentResult.getModel());
        if (!allCutInfo) {
            for (String cutInfoOri : request.getParameterMap().keySet()) {
                if (cutInfoOri.contains(Constants.CUTINFO_PREFIX) && request.getParameter(cutInfoOri).equals("on")) {
                    usedCutInfos.add(cutInfoOri.replace(Constants.CUTINFO_PREFIX, ""));
                }
            }
        }

        ExperimentDO experimentDO = experimentResult.getModel();
        PeptideDO peptide = peptideService.getByLibraryIdAndPeptideRefAndIsDecoy(targetLibraryId, targetPeptideRef, false);
        model.addAttribute("peptide", peptide);
        List<String> cutInfoFromGuess = new ArrayList<>();
        List<String> cutInfoFromGuessAndHit = new ArrayList<>();
        List<Float[]> intensitiesList = new ArrayList<Float[]>();
        List<String> cutInfoFromDic = new ArrayList<>(peptide.getFragmentMap().keySet());
        //准备该肽段的其他互补离子
        if (!noUseForFill) {
            HashMap<String, Double> bySeriesMap = fragmentFactory.getBYSeriesMap(peptide, limitLength);
            if (bySeriesMap == null) {
                model.addAttribute(ERROR_MSG, ResultCode.FRAGMENT_LENGTH_IS_TOO_LONG.getMessage());
                return "analyse/data/consultation";
            }
            if (noUseForLib) {
                peptide.getFragmentMap().clear();
            }
            for (String cutInfo : bySeriesMap.keySet()) {
                if (peptide.getFragmentMap().get(cutInfo) == null) {
                    peptide.getFragmentMap().put(cutInfo, new FragmentInfo(cutInfo, bySeriesMap.get(cutInfo), 0d, cutInfo.contains("^") ? Integer.parseInt(cutInfo.split("\\^")[1]) : 1));
                }
                cutInfoFromGuess.add(cutInfo);
            }
        }

        ResultDO<AnalyseDataDO> dataRealResult = extractor.extractOne(experimentDO, peptide, rtExtractWindow, mzExtractWindow);
        if (dataRealResult.isFailed()) {
            model.addAttribute(ERROR_MSG, ResultCode.CONVOLUTION_DATA_NOT_EXISTED.getMessage());
            return "analyse/data/consultation";
        }
        AnalyseDataDO newDataDO = dataRealResult.getModel();
        HashMap<String, Float> mzMap = newDataDO.getMzMap();

        List<String> existedCutInfoList = Lists.newArrayList(peptide.getFragmentMap().keySet());
        for (String key : existedCutInfoList) {
            if (!mzMap.containsKey(key)) {
                peptide.removeFragment(key);
            }
        }
        //获取标准库中对应的PeptideRef组
        HashMap<String, Float> intensityMap = TargetPeptide.buildIntensityMap(peptide);

        //重要步骤,"或许是目前整个工程最重要的核心算法--选峰算法."--陆妙善
        PeptideFeature peptideFeature = featureExtractor.getExperimentFeature(newDataDO, intensityMap, new SigmaSpacing(sigma, spacing));
        List<String> defaultScoreTypes = new LumsParams().getScoreTypes();
        if (peptideFeature.isFeatureFound()) {
            TreeMap<Double, Double> rtShapeScoreMap = new TreeMap<>();
            for (PeakGroup peakGroupFeature : peptideFeature.getPeakGroupList()) {
                FeatureScores featureScores = new FeatureScores(defaultScoreTypes.size());
                chromatographicScorer.calculateChromatographicScores(peakGroupFeature, peptideFeature.getNormedLibIntMap(), featureScores, defaultScoreTypes);
                libraryScorer.calculateLibraryScores(peakGroupFeature, peptideFeature.getNormedLibIntMap(), featureScores, defaultScoreTypes);
                rtShapeScoreMap.put(peakGroupFeature.getApexRt(), featureScores.get(ScoreType.XcorrShapeWeighted.getTypeName(), defaultScoreTypes));
            }
            model.addAttribute("rtShapeScoreMap", rtShapeScoreMap);
        } else {
            logger.info("未发现好信号");
        }


        //同一组的rt坐标是相同的
        Float[] rtArray = newDataDO.getRtArray();
        List<String> totalCutInfoList = new ArrayList<>();
        for (String cutInfo : newDataDO.getIntensityMap().keySet()) {
            if (newDataDO.getIntensityMap().get(cutInfo) == null || (usedCutInfos.size() != 0 && !usedCutInfos.contains(cutInfo))) {
                continue;
            }

            Float[] intensityArray = newDataDO.getIntensityMap().get(cutInfo);
            //先降噪后高斯平滑
            if (useNoise) {
                intensityArray = noise(rtArray, intensityArray, noise);
            }
            if (useGaussFilter) {
                intensityArray = gaussFilter.filterForFloat(rtArray, cutInfo, intensityArray, new SigmaSpacing(sigma, spacing));
            }
            intensitiesList.add(intensityArray);
            if (!cutInfoFromDic.contains(cutInfo)) {
                cutInfoFromGuess.remove(cutInfo);
                cutInfoFromGuessAndHit.add(cutInfo);
            }
            totalCutInfoList.add(cutInfo);
        }

        if (allCutInfo) {
            usedCutInfos.addAll(cutInfoFromDic);
            usedCutInfos.addAll(cutInfoFromGuessAndHit);
        }
        model.addAttribute("rt", rtArray);
        model.addAttribute("cutInfoFromDic", cutInfoFromDic);
        model.addAttribute("cutInfoFromGuess", cutInfoFromGuess);
        model.addAttribute("cutInfoFromGuessAndHit", cutInfoFromGuessAndHit);
        model.addAttribute("usedCutInfos", usedCutInfos);
        model.addAttribute("intensitiesList", intensitiesList);
        model.addAttribute("totalCutInfos", totalCutInfoList);

        return "analyse/data/consultation";
    }

    @RequestMapping(value = "/viewMultiGroup")
    @ResponseBody
    ResultDO<JSONObject> viewMultiGroup(Model model,
                                        @RequestParam(value = "overviewIds", required = true) String overviewIds,
                                        @RequestParam(value = "peptideRef", required = true) String peptideRef,
                                        @RequestParam(value = "isGaussFilter", required = false, defaultValue = "false") Boolean isGaussFilter,
                                        @RequestParam(value = "useNoise1000", required = false, defaultValue = "false") Boolean useNoise1000) {

        ResultDO resultDO = new ResultDO(true);
        JSONArray groups = new JSONArray();

        String[] analyseOverviewIdArray = overviewIds.split(",");
        for (String overviewId : analyseOverviewIdArray) {
            JSONObject group = new JSONObject();
            if (overviewId == null || overviewId.isEmpty()) {
                continue;
            }
            //校验权限
            ResultDO<AnalyseOverviewDO> overviewResult = analyseOverviewService.getById(overviewId);
            PermissionUtil.check(overviewResult.getModel());

            AnalyseDataDO dataForId = analyseDataService.getByOverviewIdAndPeptideRefAndIsDecoy(overviewId, peptideRef, false);
            if (dataForId == null) {
                continue;
            }
            ResultDO<AnalyseDataDO> dataResult = analyseDataService.getById(dataForId.getId());
            if (dataResult.isFailed() || dataResult.getModel() == null) {
                continue;
            }
            AnalyseDataDO data = dataResult.getModel();
            JSONArray rtArray = new JSONArray();
            JSONArray intensityArrays = new JSONArray();
            JSONArray cutInfoArray = new JSONArray();

            //同一组的rt坐标是相同的
            Float[] pairRtArray = CompressUtil.transToFloat(CompressUtil.zlibDecompress(data.getConvRtArray()));

            for (String cutInfo : data.getConvIntensityMap().keySet()) {
                if (data.getConvIntensityMap().get(cutInfo) == null) {
                    continue;
                }
                Float[] pairIntensityArray = CompressUtil.transToFloat(CompressUtil.zlibDecompress(data.getConvIntensityMap().get(cutInfo)));
                if (isGaussFilter) {
                    AnalyseOverviewDO overview = overviewResult.getModel();
                    pairIntensityArray = gaussFilter.filterForFloat(pairRtArray, cutInfo, pairIntensityArray,new SigmaSpacing(overview.getSigma(), overview.getSpacing()));
                }
                cutInfoArray.add(cutInfo);
                JSONArray intensityArray = new JSONArray();
                if (useNoise1000) {
                    intensityArray = noise(pairRtArray, pairIntensityArray);
                    intensityArrays.add(intensityArray);
                } else {

                    intensityArray.addAll(Arrays.asList(pairIntensityArray));
                    intensityArrays.add(intensityArray);
                }
            }

            if (pairRtArray != null) {
                rtArray.addAll(Arrays.asList(pairRtArray));
            } else {
                logger.error("No AnalyseData Has RtArray!!!");
            }
            JSONArray leftRtList = new JSONArray();
            JSONArray rightRtList = new JSONArray();
            for(FeatureScores scores :data.getFeatureScoresList()){
                Pair<Double, Double> rtRange = FeatureUtil.toDoublePair(scores.getRtRangeFeature());
                leftRtList.add(rtRange.getLeft());
                rightRtList.add(rtRange.getRight());
            }
            group.put("leftRtList", leftRtList);
            group.put("rightRtList", rightRtList);
            group.put("overviewId", overviewId);
            group.put("rt", rtArray);
            group.put("peptideRef", data.getPeptideRef());
            group.put("cutInfoArray", cutInfoArray);
            group.put("intensityArrays", intensityArrays);
            if (data.getBestRt() != null) {
                group.put("bestRt", (double) Math.round(data.getBestRt() * 100) / 100);
            }
            groups.add(group);
        }

        resultDO.setModel(groups);
        return resultDO;
    }

    private JSONArray noise(Float[] pairRtArray, Float[] pairIntensityArray) {

        Double[] rts = new Double[pairRtArray.length];
        Double[] ints = new Double[pairIntensityArray.length];
        for (int i = 0; i < rts.length; i++) {
            rts[i] = Double.parseDouble(pairRtArray[i].toString());
            ints[i] = Double.parseDouble(pairIntensityArray[i].toString());
        }
        double[] noisePairIntensityArray = signalToNoiseEstimator.computeSTN(rts, ints, 1000, 30);
        JSONArray noiseIntensityArray = new JSONArray();
        for (int i = 0; i < noisePairIntensityArray.length; i++) {
            if (noisePairIntensityArray[i] >= Constants.SIGNAL_TO_NOISE_LIMIT) {
                noiseIntensityArray.add(pairIntensityArray[i]);
            } else {
                noiseIntensityArray.add(0);
            }
        }
        JSONArray intensityArrays = new JSONArray();
        intensityArrays.addAll(noiseIntensityArray);
        return intensityArrays;
    }

    private Float[] noise(Float[] pairRtArray, Float[] pairIntensityArray, Integer noise) {

        Double[] rts = new Double[pairRtArray.length];
        Double[] ints = new Double[pairIntensityArray.length];
        for (int i = 0; i < rts.length; i++) {
            rts[i] = Double.parseDouble(pairRtArray[i].toString());
            ints[i] = Double.parseDouble(pairIntensityArray[i].toString());
        }
        double[] noisePairIntensityArray = signalToNoiseEstimator.computeSTN(rts, ints, noise, 30);
        Float[] noiseIntensityArray = new Float[noisePairIntensityArray.length];
        for (int i = 0; i < noisePairIntensityArray.length; i++) {
            if (noisePairIntensityArray[i] >= Constants.SIGNAL_TO_NOISE_LIMIT) {
                noiseIntensityArray[i] = pairIntensityArray[i];
            } else {
                noiseIntensityArray[i] = 0f;
            }
        }

        return noiseIntensityArray;
    }
}
