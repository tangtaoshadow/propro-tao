package com.westlake.air.propro.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.westlake.air.propro.async.LumsTask;
import com.westlake.air.propro.compressor.AirdCompressor;
import com.westlake.air.propro.constants.*;
import com.westlake.air.propro.dao.ScanIndexDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.propro.domain.bean.analyse.WindowRange;
import com.westlake.air.propro.domain.bean.score.SlopeIntercept;
import com.westlake.air.propro.domain.db.*;
import com.westlake.air.propro.domain.params.Exp;
import com.westlake.air.propro.domain.params.ExpVO;
import com.westlake.air.propro.domain.params.LumsParams;
import com.westlake.air.propro.domain.query.ExperimentQuery;
import com.westlake.air.propro.domain.query.ScanIndexQuery;
import com.westlake.air.propro.parser.MzXMLParser;
import com.westlake.air.propro.service.*;
import com.westlake.air.propro.utils.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.*;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-04 10:00
 */
@Controller
@RequestMapping("experiment")
public class ExperimentController extends BaseController {

    @Autowired
    LibraryService libraryService;
    @Autowired
    PeptideService peptideService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    MzXMLParser mzXMLParser;
    @Autowired
    ScanIndexService scanIndexService;
    @Autowired
    AirdCompressor airdCompressor;
    @Autowired
    LumsTask lumsTask;
    @Autowired
    ProjectService projectService;
    @Autowired
    ScanIndexDAO scanIndexDAO;

    @RequestMapping(value = "/list")
    String list(Model model,
                @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                @RequestParam(value = "pageSize", required = false, defaultValue = "50") Integer pageSize,
                @RequestParam(value = "projectName", required = false) String projectName,
                @RequestParam(value = "batchName", required = false) String batchName,
                @RequestParam(value = "expName", required = false) String expName) {
        model.addAttribute("expName", expName);
        model.addAttribute("projectName", projectName);
        model.addAttribute("batchName", batchName);
        model.addAttribute("pageSize", pageSize);
        ExperimentQuery query = new ExperimentQuery();
        if (expName != null && !expName.isEmpty()) {
            query.setName(expName);
        }
        if(projectName != null && !projectName.isEmpty()){
            query.setProjectName(projectName);
        }
        if(batchName != null && !batchName.isEmpty()){
            query.setBatchName(batchName);
        }
        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        ResultDO<List<ExperimentDO>> resultDO = experimentService.getList(query);
        HashMap<String, AnalyseOverviewDO> analyseOverviewDOMap = new HashMap<>();
        for (ExperimentDO experimentDO: resultDO.getModel()){
            AnalyseOverviewDO analyseOverviewDO = analyseOverviewService.getAllByExpId(experimentDO.getId()).get(0);
            analyseOverviewDOMap.put(experimentDO.getId(), analyseOverviewDO);
        }

        model.addAttribute("experiments", resultDO.getModel());
        model.addAttribute("analyseOverviewDOMap", analyseOverviewDOMap);
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);
        return "experiment/list";
    }

    @RequestMapping(value = "/create")
    String create(Model model) {
        return "experiment/create";
    }

    @RequestMapping(value = "/batchcreate")
    String batchCreate(Model model,
                       @RequestParam(value = "projectName", required = false)String projectName,
                       RedirectAttributes redirectAttributes) {

        ResultDO<ProjectDO> result = projectService.getByName(projectName);
        if(result.isFailed()){
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.PROJECT_NOT_EXISTED.getMessage());
            return "redirect:/project/list";
        }
        model.addAttribute("projectName", projectName);
        model.addAttribute("repository",result.getModel().getRepository());
        return "experiment/batchcreate";
    }

    @RequestMapping(value = "/add")
    String add(Model model,
               @RequestParam(value = "name", required = true) String name,
               @RequestParam(value = "projectName", required = true) String projectName,
               @RequestParam(value = "batchName", required = false) String batchName,
               @RequestParam(value = "filePath", required = true) String filePath,
               @RequestParam(value = "description", required = false) String description,
               @RequestParam(value = "overlap", required = false) Float overlap,
               RedirectAttributes redirectAttributes) {

        model.addAttribute("overlap", overlap);
        model.addAttribute("name", name);
        model.addAttribute("description", description);

        ResultDO<ProjectDO> projectResult = projectService.getByName(projectName);
        model.addAttribute("projectName", projectName);
        if(projectResult.isFailed()){
            model.addAttribute(ERROR_MSG, ResultCode.PROJECT_NOT_EXISTED.getMessage());
            return "experiment/create";
        }
        //Check Params Start
        if (filePath == null || filePath.isEmpty()) {
            model.addAttribute(ERROR_MSG, ResultCode.FILE_LOCATION_CANNOT_BE_EMPTY.getMessage());
            return "experiment/create";
        }
        model.addAttribute("filePath", filePath);
        File file = new File(filePath);

        if (!file.exists()) {
            model.addAttribute(ERROR_MSG, ResultCode.FILE_NOT_EXISTED.getMessage());
            return "experiment/create";
        }

        ExperimentDO experimentDO = new ExperimentDO();
        experimentDO.setName(name);
        experimentDO.setDescription(description);
        experimentDO.setOverlap(overlap);
        experimentDO.setFilePath(filePath);
        experimentDO.setProjectName(projectName);
        experimentDO.setBatchName(batchName);
        experimentDO.setType(projectResult.getModel().getType());

        ResultDO result = experimentService.insert(experimentDO);
        if (result.isFailed()) {
            model.addAttribute(ERROR_MSG, result.getMsgInfo());
            return "experiment/create";
        }//Check Params End

        TaskDO taskDO = new TaskDO(TaskTemplate.UPLOAD_EXPERIMENT_FILE, experimentDO.getName());
        taskService.insert(taskDO);

        if(FileUtil.isMzXMLFile(filePath)){
            experimentTask.saveExperimentTask(experimentDO, file, taskDO);
        }else{
            experimentTask.saveAirdTask(experimentDO, file.getPath(), taskDO);
        }

        return "redirect:/task/detail/" + taskDO.getId();
    }

    @RequestMapping(value = "/batchadd", method = {RequestMethod.POST})
    String batchAdd(Model model,
                    @RequestParam(value = "projectName", required = false) String projectName,
                    @RequestParam(value = "batchName", required = false) String batchName,
                    ExpVO exps,
                    RedirectAttributes redirectAttributes) {

        String errorInfo = "";
        List<Exp> expList = exps.getExps();
        for (Exp exp : expList) {
            if (exp.getFilePath() == null || exp.getFilePath().isEmpty()) {
                errorInfo += ResultCode.FILE_LOCATION_CANNOT_BE_EMPTY.getMessage() + ":" + exp.getName() + "\r\n";
            }
            File file = new File(exp.getFilePath());

            if (!file.exists()) {
                errorInfo += ResultCode.FILE_NOT_EXISTED.getMessage() + ":" + exp.getFilePath() + "\r\n";
                continue;
            }

            ResultDO<ProjectDO> projectResult = projectService.getByName(projectName);
            ExperimentDO experimentDO = new ExperimentDO();
            experimentDO.setProjectName(projectName);
            experimentDO.setBatchName(batchName);
            experimentDO.setName(exp.getName());
            experimentDO.setDescription(exp.getDescription());
            experimentDO.setOverlap(exp.getOverlap());
            experimentDO.setFilePath(exp.getFilePath());
            experimentDO.setType(projectResult.getModel().getType());

            ResultDO result = experimentService.insert(experimentDO);
            if (result.isFailed()) {
                errorInfo += result.getMsgInfo() + ":" + exp.getName() + "\r\n";
            }

            TaskDO taskDO = new TaskDO(TaskTemplate.UPLOAD_EXPERIMENT_FILE, experimentDO.getName());
            if (!errorInfo.isEmpty()) {
                taskDO.addLog(errorInfo);
                taskDO.finish(TaskStatus.FAILED.getName());
            } else {
                taskService.insert(taskDO);
                if(FileUtil.isMzXMLFile(file.getPath())){
                    experimentTask.saveExperimentTask(experimentDO, file, taskDO);
                }else{
                    experimentTask.saveAirdTask(experimentDO, file.getPath(), taskDO);
                }
            }
        }

        if (!errorInfo.isEmpty()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, errorInfo);
        }
        return "redirect:/task/list?taskTemplate=" + TaskTemplate.UPLOAD_EXPERIMENT_FILE.getName();
    }

    @RequestMapping(value = "/edit/{id}")
    String edit(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {

        ResultDO<ExperimentDO> resultDO = experimentService.getById(id);
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/experiment/list";
        } else {
            model.addAttribute("experiment", resultDO.getModel());
            return "experiment/edit";
        }
    }

    @RequestMapping(value = "/detail/{id}")
    String detail(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        ResultDO<ExperimentDO> resultDO = experimentService.getById(id);
        if (resultDO.isSuccess()) {
            model.addAttribute("experiment", resultDO.getModel());
            return "experiment/detail";
        } else {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/experiment/list";
        }
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    String update(Model model,
                  @RequestParam(value = "id", required = true) String id,
                  @RequestParam(value = "name") String name,
                  @RequestParam(value = "type") String type,
                  @RequestParam(value = "iRtLibraryId") String iRtLibraryId,
                  @RequestParam(value = "slope") Double slope,
                  @RequestParam(value = "intercept") Double intercept,
                  @RequestParam(value = "filePath") String filePath,
                  @RequestParam(value = "airdPath") String airdPath,
                  @RequestParam(value = "airdIndexPath") String airdIndexPath,
                  @RequestParam(value = "description") String description,
                  @RequestParam(value = "compressionType") String compressionType,
                  @RequestParam(value = "precision") String precision,
                  @RequestParam(value = "projectName") String projectName,
                  @RequestParam(value = "batchName") String batchName,
                  RedirectAttributes redirectAttributes) {

        ResultDO<ExperimentDO> resultDO = experimentService.getById(id);
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/experiment/list";
        }
        ExperimentDO experimentDO = resultDO.getModel();

        experimentDO.setName(name);
        experimentDO.setType(type);
        experimentDO.setProjectName(projectName);
        experimentDO.setBatchName(batchName);
        experimentDO.setFilePath(filePath);
        experimentDO.setAirdPath(airdPath);
        experimentDO.setAirdIndexPath(airdIndexPath);
        experimentDO.setDescription(description);
        experimentDO.setIRtLibraryId(iRtLibraryId);
        experimentDO.setSlope(slope);
        experimentDO.setIntercept(intercept);
        experimentDO.setCompressionType(compressionType);
        experimentDO.setPrecision(precision);
        ResultDO result = experimentService.update(experimentDO);
        if (result.isFailed()) {
            model.addAttribute(ERROR_MSG, result.getMsgInfo());
            return "experiment/create";
        }
        return "redirect:/experiment/list";

    }

    @RequestMapping(value = "/delete/{id}")
    String delete(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        ResultDO<ExperimentDO> exp = experimentService.getById(id);
        experimentService.delete(id);
        scanIndexService.deleteAllByExperimentId(id);
        List<AnalyseOverviewDO> overviewDOList = analyseOverviewService.getAllByExpId(id);
        for (AnalyseOverviewDO overviewDO : overviewDOList) {
            analyseDataService.deleteAllByOverviewId(overviewDO.getId());
        }

        analyseOverviewService.deleteAllByExpId(id);

        redirectAttributes.addFlashAttribute("projectName", exp.getModel().getProjectName());
        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.DELETE_SUCCESS);
        return "redirect:/experiment/list";

    }
    @RequestMapping(value = "/deleteAll/{id}")
    String deleteAll(Model model, @PathVariable("id") String id,
                     RedirectAttributes redirectAttributes) {
        ResultDO<ExperimentDO> exp = experimentService.getById(id);
        List<AnalyseOverviewDO> overviewDOList = analyseOverviewService.getAllByExpId(id);
        for (AnalyseOverviewDO overviewDO : overviewDOList) {
            analyseDataService.deleteAllByOverviewId(overviewDO.getId());
        }
        analyseOverviewService.deleteAllByExpId(id);
        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.DELETE_SUCCESS);
        return "redirect:/experiment/list";

    }

    @RequestMapping(value = "/swath")
    String swath(Model model) {
        List<LibraryDO> slist = getLibraryList(0);
        List<LibraryDO> iRtlist = getLibraryList(1);
        List<ExperimentDO> experimentList = getExperimentList();
        model.addAttribute("libraries", slist);
        model.addAttribute("iRtLibraries", iRtlist);
        model.addAttribute("experiments", experimentList);
        return "experiment/swath";
    }

    @RequestMapping(value = "/doswath")
    String doSwath(Model model,
                   @RequestParam(value = "libraryId", required = false) String libraryId,
                   @RequestParam(value = "iRtLibraryId", required = false) String iRtLibraryId,
                   @RequestParam(value = "expId", required = false) String expId,
                   @RequestParam(value = "rtExtractWindow", defaultValue = "1200") Float rtExtractWindow,
                   @RequestParam(value = "mzExtractWindow", defaultValue = "0.05") Float mzExtractWindow,
                   @RequestParam(value = "sigma", defaultValue = "6.25") Float sigma,
                   @RequestParam(value = "spacing", defaultValue = "0.01") Float spacing
    ) {
        if (libraryId != null) {
            model.addAttribute("libraryId", libraryId);
        }
        if (iRtLibraryId != null) {
            model.addAttribute("iRtLibraryId", iRtLibraryId);
        }
        if (expId != null) {
            model.addAttribute("expId", expId);
        }
        model.addAttribute("rtExtractWindow", rtExtractWindow);
        model.addAttribute("mzExtractWindow", mzExtractWindow);
        model.addAttribute("sigma", sigma);
        model.addAttribute("spacing", spacing);

        List<LibraryDO> slist = getLibraryList(0);
        List<LibraryDO> iRtlist = getLibraryList(1);
        List<ExperimentDO> experimentList = getExperimentList();
        model.addAttribute("libraries", slist);
        model.addAttribute("iRtLibraries", iRtlist);
        model.addAttribute("experiments", experimentList);

        ResultDO<ExperimentDO> expResult = experimentService.getById(expId);
        if (expResult.isFailed()) {
            return "experiment/swath";
        }

        ExperimentDO exp = expResult.getModel();

        TaskDO taskDO = new TaskDO(TaskTemplate.SWATH_WORKFLOW, "expId:" + expId + ";libId:" + libraryId + ";iRtLib:" + iRtLibraryId);
        taskService.insert(taskDO);

        LumsParams lumsParams = new LumsParams();
        lumsParams.setExperimentDO(exp);
        lumsParams.setIRtLibraryId(iRtLibraryId);
        lumsParams.setLibraryId(libraryId);
        lumsParams.setCreator("Admin");
        lumsParams.setRtExtractWindow(rtExtractWindow);
        lumsParams.setMzExtractWindow(mzExtractWindow);
        lumsParams.setSigmaSpacing(new SigmaSpacing(sigma, spacing));

        lumsTask.swath(lumsParams, taskDO);
        return "redirect:/task/detail/" + taskDO.getId();
    }

    @RequestMapping(value = "/extractor")
    String extractor(Model model,
                     @RequestParam(value = "id", required = true) String id,
                     @RequestParam(value = "libraryId", required = false) String libraryId,
                     RedirectAttributes redirectAttributes) {
        model.addAttribute("libraryId", libraryId);

        ResultDO<ExperimentDO> resultDO = experimentService.getById(id);
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.OBJECT_NOT_EXISTED);
            return "redirect:/experiment/list";
        }
//        List<ScoreType> scoreType = ScoreType.getShownTypes();
//        if(resultDO.getModel().getType().equals("1")){
//            HashSet<String> prm
//        }
        model.addAttribute("useEpps", true);
//        model.addAttribute("uniqueOnly", false);
        model.addAttribute("libraries", getLibraryList(0));
        model.addAttribute("experiment", resultDO.getModel());
        model.addAttribute("scoreTypes", ScoreType.getShownTypes());

        return "experiment/extractor";
    }

    @RequestMapping(value = "/doextract")
    String doExtract(Model model,
                     @RequestParam(value = "id", required = true) String id,
                     @RequestParam(value = "creator", required = false) String creator,
                     @RequestParam(value = "libraryId", required = true) String libraryId,
                     @RequestParam(value = "rtExtractWindow", required = true, defaultValue = "600") Float rtExtractWindow,
                     @RequestParam(value = "mzExtractWindow", required = true, defaultValue = "0.05") Float mzExtractWindow,
                     @RequestParam(value = "slope", required = false) Double slope,
                     @RequestParam(value = "intercept", required = false) Double intercept,
                     @RequestParam(value = "note", required = false) String note,
                     //打分相关的入参
                     @RequestParam(value = "sigma", required = false, defaultValue = "3.75") Float sigma,
                     @RequestParam(value = "spacing", required = false, defaultValue = "0.01") Float spacing,
                     @RequestParam(value = "shapeScoreThreshold", required = false, defaultValue = "0.6") Float shapeScoreThreshold,
                     @RequestParam(value = "shapeWeightScoreThreshold", required = false, defaultValue = "0.8") Float shapeWeightScoreThreshold,
                     @RequestParam(value = "useEpps", required = false, defaultValue = "true") Boolean useEpps,
                     @RequestParam(value = "uniqueOnly", required = false, defaultValue = "false") Boolean uniqueOnly,
                     HttpServletRequest request,
                     RedirectAttributes redirectAttributes) {

        ResultDO<ExperimentDO> resultDO = experimentService.getById(id);
        if (resultDO.isFailed()) {
            return "redirect:/extractor?id=" + id;
        }

        ResultDO<LibraryDO> libResult = libraryService.getById(libraryId);
        if (libResult.isFailed()) {
            return "redirect:/extractor?id=" + id;
        }

        HashSet<String> scoreTypes = new HashSet<>();
        for (ScoreType type : ScoreType.values()) {
            String typeParam = request.getParameter(type.getTypeName());
            if (typeParam != null && typeParam.equals("on")) {
                scoreTypes.add(type.getTypeName());
            }
        }

        TaskDO taskDO = new TaskDO(useEpps ? TaskTemplate.EXTRACT_PEAKPICK_SCORE : TaskTemplate.EXTRACTOR, resultDO.getModel().getName() + ":" + libResult.getModel().getName() + "(" + libraryId + ")");
        taskService.insert(taskDO);
        SlopeIntercept si = SlopeIntercept.create();
        if (slope != null && intercept != null) {
            si.setSlope(slope);
            si.setIntercept(intercept);
        }
        SigmaSpacing ss = new SigmaSpacing(sigma, spacing);

        LumsParams input = new LumsParams();
        input.setExperimentDO(resultDO.getModel());
        input.setLibraryId(libraryId);
        input.setSlopeIntercept(si);
        input.setNote(note);
        input.setCreator(creator);
        input.setRtExtractWindow(rtExtractWindow);
        input.setMzExtractWindow(mzExtractWindow);
        input.setUseEpps(useEpps);
        input.setUniqueOnly(uniqueOnly);
        input.setScoreTypes(scoreTypes);
        input.setSigmaSpacing(ss);
        input.setXcorrShapeThreshold(shapeScoreThreshold);
        input.setXcorrShapeWeightThreshold(shapeWeightScoreThreshold);

        experimentTask.extract(input, taskDO);

        return "redirect:/task/detail/" + taskDO.getId();
    }

    @RequestMapping(value = "/doextractone")
    @ResponseBody
    String doExtractOne(Model model,
                        @RequestParam(value = "expId", required = true) String expId,
                        @RequestParam(value = "libraryId", required = true) String libraryId,
                        @RequestParam(value = "peptideRef", required = true) String peptideRef) {

        ResultDO<ExperimentDO> resultDO = experimentService.getById(expId);
        PeptideDO peptide = peptideService.getByLibraryIdAndPeptideRefAndIsDecoy(libraryId, peptideRef, false);


        ResultDO<AnalyseDataDO> analyseData = experimentService.extractOne(resultDO.getModel(), peptide, -1f, 0.05f);

        return JSON.toJSONString(analyseData);
    }

    @RequestMapping(value = "/irt")
    String irt(Model model,
               @RequestParam(value = "id", required = true) String id,
               @RequestParam(value = "iRtLibraryId", required = false) String iRtLibraryId,
               RedirectAttributes redirectAttributes) {
        model.addAttribute("iRtLibraryId", iRtLibraryId);

        ResultDO<ExperimentDO> resultDO = experimentService.getById(id);
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.OBJECT_NOT_EXISTED);
            return "redirect:/experiment/list";
        }

        model.addAttribute("libraries", getLibraryList(1));
        model.addAttribute("experiment", resultDO.getModel());
        return "experiment/irt";
    }

    @RequestMapping(value = "/doirt")
    String doIrt(Model model,
                 @RequestParam(value = "id", required = true) String id,
                 @RequestParam(value = "iRtLibraryId", required = true) String iRtLibraryId,
                 @RequestParam(value = "sigma", required = true, defaultValue = "3.75") Float sigma,
                 @RequestParam(value = "spacing", required = true, defaultValue = "0.01") Float spacing,
                 @RequestParam(value = "mzExtractWindow", required = true, defaultValue = "0.05") Float mzExtractWindow,
                 RedirectAttributes redirectAttributes) {

        ResultDO<ExperimentDO> resultDO = experimentService.getById(id);
        if (resultDO.isFailed()) {
            return "redirect:/irt/" + id;
        }

        TaskDO taskDO = new TaskDO(TaskTemplate.IRT, resultDO.getModel().getName() + ":" + iRtLibraryId);
        taskService.insert(taskDO);

        SigmaSpacing sigmaSpacing = new SigmaSpacing(sigma, spacing);
        experimentTask.convAndIrt(resultDO.getModel(), iRtLibraryId, mzExtractWindow, sigmaSpacing, taskDO);

        return "redirect:/task/detail/" + taskDO.getId();
    }

    @RequestMapping(value = "/getWindows")
    @ResponseBody
    ResultDO<JSONObject> getWindows(Model model, @RequestParam(value = "expId", required = true) String expId) {
        List<WindowRange> rangs = experimentService.getWindows(expId);
        ResultDO<JSONObject> resultDO = new ResultDO<>(true);

        JSONObject res = new JSONObject();
        JSONArray indexArray = new JSONArray();
        JSONArray mzStartArray = new JSONArray();
        JSONArray mzRangArray = new JSONArray();
        for (int i = 0; i < rangs.size(); i++) {
            indexArray.add((int) (rangs.get(i).getInterval() * 1000) + "ms");
            mzStartArray.add(rangs.get(i).getStart());
            mzRangArray.add((rangs.get(i).getEnd() - rangs.get(i).getStart()));
        }
        res.put("indexes", indexArray);
        res.put("starts", mzStartArray);
        res.put("rangs", mzRangArray);
        res.put("min", rangs.get(0).getStart());
        res.put("max", rangs.get(rangs.size() - 1).getEnd());
        resultDO.setModel(res);
        return resultDO;
    }
    @RequestMapping(value = "/getPrmWindows")
    @ResponseBody
    ResultDO<JSONObject> getPrmWindows(Model model, @RequestParam(value = "expId", required = true) String expId){

        ScanIndexQuery query = new ScanIndexQuery();
        query.setExperimentId(expId);
        query.setMsLevel(2);
        List<ScanIndexDO> msAllIndexes = scanIndexDAO.getAll(query);
        HashMap<Float, Float[]> peptideMap = new HashMap<>();
        for(ScanIndexDO scanIndexDO: msAllIndexes){
            if (!peptideMap.containsKey(scanIndexDO.getPrecursorMz())) {
                peptideMap.put(scanIndexDO.getPrecursorMz(), new Float[]{Float.MAX_VALUE, Float.MIN_VALUE});
            }
            if(scanIndexDO.getRt()>peptideMap.get(scanIndexDO.getPrecursorMz())[1]){
                peptideMap.get(scanIndexDO.getPrecursorMz())[1] = scanIndexDO.getRt();
            }
            if(scanIndexDO.getRt()<peptideMap.get(scanIndexDO.getPrecursorMz())[0]){
                peptideMap.get(scanIndexDO.getPrecursorMz())[0] = scanIndexDO.getRt();
            }
        }
        JSONArray peptideMs1List = new JSONArray();
        for(Float precursorMz: peptideMap.keySet()){
            JSONArray peptide = new JSONArray();
            peptide.add(new Float[]{peptideMap.get(precursorMz)[0] ,precursorMz});
            peptide.add(new Float[]{peptideMap.get(precursorMz)[1] ,precursorMz});
            peptideMs1List.add(peptide);
        }
        JSONObject res = new JSONObject();
        res.put("peptideList",peptideMs1List);
        ResultDO<JSONObject> resultDO = new ResultDO<>(true);
        resultDO.setModel(res);
        return resultDO;
    }

    @RequestMapping(value = "/getPrmDensity")
    @ResponseBody
    ResultDO<JSONObject> getPrmDensity(Model model, @RequestParam(value = "expId", required = true) String expId){
        ResultDO<JSONObject> resultDO = new ResultDO<>(true);
        JSONArray ms2Density = new JSONArray();
        ScanIndexQuery query = new ScanIndexQuery();
        query.setExperimentId(expId);

        query.setMsLevel(1);
        List<ScanIndexDO> ms1Indexs = scanIndexDAO.getAll(query);
        List<Float> ms1RtList = new ArrayList<>();
        for(ScanIndexDO ms1: ms1Indexs){
            ms1RtList.add(ms1.getRt());
        }
        Collections.sort(ms1RtList);

        query.setMsLevel(2);
        List<ScanIndexDO> ms2Indexs = scanIndexDAO.getAll(query);
        List<Float> ms2RtList = new ArrayList<>();
        for(ScanIndexDO ms2: ms2Indexs){
            ms2RtList.add(ms2.getRt());
        }
        Collections.sort(ms2RtList);
        int ms2Index = ms2Indexs.size() - 1;
        int max = Integer.MIN_VALUE;
        for(int ms1Index = ms1RtList.size() - 1; ms1Index >=0; ms1Index--){
            int count = 0;
            for(; ms2Index >= 0; ms2Index--){
                if(ms2Index-count>=0 && ms2RtList.get(ms2Index - count) > ms1RtList.get(ms1Index)){
                    count ++;
                }else {
                    break;
                }
            }
            ms2Density.add(new Float[]{ms1RtList.get(ms1Index), (float)count});
            if(count > max){
                max = count;
            }
        }

        JSONObject res = new JSONObject();
        res.put("ms2Density", ms2Density);
        res.put("max", (int)Math.ceil(max/10d)*10d);
        resultDO.setModel(res);
        return resultDO;
    }

    @RequestMapping(value = "/compress")
    String compress(Model model,
                    @RequestParam(value = "expId", required = true) String expId,
                    RedirectAttributes redirectAttributes) {

        ResultDO<ExperimentDO> resultDO = experimentService.getById(expId);
        if (resultDO.isFailed()) {
            redirectAttributes.addAttribute(ERROR_MSG, ResultCode.EXPERIMENT_NOT_EXISTED.getMessage());
            return "redirect:/experiment/list";
        }
        ExperimentDO experimentDO = resultDO.getModel();
        if(experimentDO.getType().equals(Constants.EXP_TYPE_DIA_SWATH)){
            redirectAttributes.addAttribute(ERROR_MSG, ResultCode.NO_AIRD_COMPRESSION_FOR_DIA_SWATH.getMessage());
            return "redirect:/experiment/list";
        }
        TaskDO taskDO = new TaskDO(TaskTemplate.COMPRESSOR_AND_SORT, experimentDO.getName() + ":" + expId);
        taskService.insert(taskDO);
        experimentTask.compress(experimentDO, taskDO);

        return "redirect:/task/detail/" + taskDO.getId();
    }
}
