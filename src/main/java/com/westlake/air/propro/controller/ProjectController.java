package com.westlake.air.propro.controller;

import com.westlake.air.propro.constants.ResultCode;
import com.westlake.air.propro.constants.ScoreType;
import com.westlake.air.propro.constants.SuccessMsg;
import com.westlake.air.propro.constants.TaskTemplate;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.propro.domain.bean.score.SlopeIntercept;
import com.westlake.air.propro.domain.db.*;
import com.westlake.air.propro.domain.params.LumsParams;
import com.westlake.air.propro.domain.query.ExperimentQuery;
import com.westlake.air.propro.domain.query.ProjectQuery;
import com.westlake.air.propro.service.*;
import com.westlake.air.propro.utils.FeatureUtil;
import com.westlake.air.propro.utils.PermissionUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.*;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-04 10:00
 */
@Controller
@RequestMapping("project")
public class ProjectController extends BaseController {

    @Autowired
    ProjectService projectService;
    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    PeptideService peptideService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    ScanIndexService scanIndexService;

    @RequestMapping(value = "/list")
    String list(Model model,
                @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                @RequestParam(value = "pageSize", required = false, defaultValue = "50") Integer pageSize,
                @RequestParam(value = "name", required = false) String name) {
        model.addAttribute("name", name);
        model.addAttribute("pageSize", pageSize);
        ProjectQuery query = new ProjectQuery();
        if (name != null && !name.isEmpty()) {
            query.setName(name);
        }

        if(!isAdmin()){
            query.setOwnerName(getCurrentUsername());
        }
        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        ResultDO<List<ProjectDO>> resultDO = projectService.getList(query);

        model.addAttribute("projectList", resultDO.getModel());
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);
        return "project/list";
    }

    @RequestMapping(value = "/create")
    String create(Model model) {
        return "project/create";
    }

    @RequestMapping(value = "/add")
    String add(Model model,
               @RequestParam(value = "name", required = true) String name,
               @RequestParam(value = "repository", required = true) String repository,
               @RequestParam(value = "description", required = false) String description,
               @RequestParam(value = "type", required = true) String type,
               RedirectAttributes redirectAttributes) {

        String ownerName = getCurrentUsername();

        model.addAttribute("repository", repository);
        model.addAttribute("ownerName", ownerName);
        model.addAttribute("name", name);
        model.addAttribute("description", description);
        model.addAttribute("type", type);

        File file = new File(repository);
        if (!file.exists()) {
            file.mkdirs();
        }

        ProjectDO projectDO = new ProjectDO();
        projectDO.setName(name);
        projectDO.setDescription(description);
        projectDO.setRepository(repository);
        projectDO.setOwnerName(ownerName);
        projectDO.setType(type);

        ResultDO result = projectService.insert(projectDO);
        if (result.isFailed()) {
            model.addAttribute(ERROR_MSG, result.getMsgInfo());
            return "project/create";
        }

        return "redirect:/project/list";
    }

    @RequestMapping(value = "/edit/{id}")
    String edit(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {

        ResultDO<ProjectDO> resultDO = projectService.getById(id);
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/project/list";
        } else {
            PermissionUtil.check(resultDO.getModel());
            model.addAttribute("project", resultDO.getModel());
            return "project/edit";
        }
    }

    @RequestMapping(value = "/delete/{id}")
    String delete(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {

        ResultDO<ProjectDO> resultDO = projectService.getById(id);
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/project/list";
        } else {
            PermissionUtil.check(resultDO.getModel());
            projectService.delete(id);
            redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.DELETE_SUCCESS);
            return "redirect:/project/list";
        }
    }

    @RequestMapping(value = "/irt")
    String irt(Model model,
               @RequestParam(value = "id", required = true) String id,
               @RequestParam(value = "iRtLibraryId", required = false) String iRtLibraryId,
               RedirectAttributes redirectAttributes) {

        ResultDO<ProjectDO> resultDO = projectService.getById(id);
        PermissionUtil.check(resultDO.getModel());
        List<ExperimentDO> expList = experimentService.getAllByProjectName(resultDO.getModel().getName());
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(SUCCESS_MSG, ResultCode.PROJECT_NOT_EXISTED);
            return "redirect:/project/list";
        }
        model.addAttribute("exps", expList);
        model.addAttribute("project", resultDO.getModel());
        model.addAttribute("iRtLibraryId", iRtLibraryId);
        model.addAttribute("libraries", getLibraryList(1, true));

        return "project/irt";
    }

    @RequestMapping(value = "/doirt")
    String doIrt(Model model,
                 @RequestParam(value = "id", required = true) String id,
                 @RequestParam(value = "iRtLibraryId", required = false) String iRtLibraryId,
                 @RequestParam(value = "sigma", required = true, defaultValue = "3.75") Float sigma,
                 @RequestParam(value = "spacing", required = true, defaultValue = "0.01") Float spacing,
                 @RequestParam(value = "mzExtractWindow", required = true, defaultValue = "0.05") Float mzExtractWindow,
                 RedirectAttributes redirectAttributes) {

        ResultDO<ProjectDO> resultDO = projectService.getById(id);
        PermissionUtil.check(resultDO.getModel());

        List<ExperimentDO> expList = getAllExperimentsByProjectId(id);
        if (expList == null) {
            redirectAttributes.addFlashAttribute(SUCCESS_MSG, ResultCode.NO_EXPERIMENT_UNDER_PROJECT);
            return "redirect:/project/list";
        }
        int count = 0;
        for (ExperimentDO exp : expList) {
//            if (exp.getSlope() == null || exp.getIntercept() == null) {
                TaskDO taskDO = new TaskDO(TaskTemplate.IRT, exp.getName() + ":" + iRtLibraryId);
                taskService.insert(taskDO);
                SigmaSpacing sigmaSpacing = new SigmaSpacing(sigma, spacing);
                experimentTask.convAndIrt(exp, iRtLibraryId, mzExtractWindow, sigmaSpacing, taskDO);
                count++;
//            }
        }
        if (count == 0) {
            redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.ALL_EXPERIMENTS_UNDER_THIS_PROJECT_ARE_ALREADY_COMPUTE_IRT);
            return "redirect:/project/list";
        } else {
            return "redirect:/task/list";
        }
    }

    @RequestMapping(value = "/setPublic/{id}")
    String setPublic(@PathVariable("id") String id,
                     RedirectAttributes redirectAttributes) {
        ResultDO<ProjectDO> resultDO = projectService.getById(id);
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/project/list";
        }

        ProjectDO project = resultDO.getModel();
        PermissionUtil.check(project);

        project.setDoPublic(true);
        projectService.update(project);

        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.SET_PUBLIC_SUCCESS);
        return "redirect:/project/list";
    }

    @RequestMapping(value = "/deleteirt/{id}")
    String deleteIrt(@PathVariable("id") String id,
                     RedirectAttributes redirectAttributes) {

        ResultDO<ProjectDO> resultDO = projectService.getById(id);
        PermissionUtil.check(resultDO.getModel());

        List<ExperimentDO> expList = getAllExperimentsByProjectId(id);
        if (expList == null) {
            redirectAttributes.addFlashAttribute(SUCCESS_MSG, ResultCode.NO_EXPERIMENT_UNDER_PROJECT);
            return "redirect:/project/list";
        }
        for (ExperimentDO experimentDO : expList) {
            experimentDO.setSlope(null);
            experimentDO.setIntercept(null);
            experimentService.update(experimentDO);
        }

        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.DELETE_SUCCESS);
        return "redirect:/project/list";
    }

    @RequestMapping(value = "/deleteAll/{id}")
    String deleteAll(@PathVariable("id") String id,
                     RedirectAttributes redirectAttributes) {
        ResultDO<ProjectDO> resultDO = projectService.getById(id);
        PermissionUtil.check(resultDO.getModel());

        String name = resultDO.getModel().getName();
        List<ExperimentDO> expList = experimentService.getAllByProjectName(name);
        for (ExperimentDO experimentDO : expList) {
            String expId = experimentDO.getId();
            experimentService.delete(expId);
            scanIndexService.deleteAllByExperimentId(expId);
            List<AnalyseOverviewDO> overviewDOList = analyseOverviewService.getAllByExpId(expId);
            for (AnalyseOverviewDO overviewDO : overviewDOList) {
                analyseDataService.deleteAllByOverviewId(overviewDO.getId());
            }
            analyseOverviewService.deleteAllByExpId(experimentDO.getId());
        }
        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.DELETE_SUCCESS);
        return "redirect:/project/list";
    }

    @RequestMapping(value = "/deleteAnalyse/{id}")
    String deleteAnalyse(@PathVariable("id") String id,
                         RedirectAttributes redirectAttributes) {

        ResultDO<ProjectDO> resultDO = projectService.getById(id);
        PermissionUtil.check(resultDO.getModel());

        String name = resultDO.getModel().getName();
        List<ExperimentDO> expList = experimentService.getAllByProjectName(name);
        for (ExperimentDO experimentDO : expList) {
            String expId = experimentDO.getId();
            List<AnalyseOverviewDO> overviewDOList = analyseOverviewService.getAllByExpId(expId);
            for (AnalyseOverviewDO overviewDO : overviewDOList) {
                analyseDataService.deleteAllByOverviewId(overviewDO.getId());
            }
            analyseOverviewService.deleteAllByExpId(expId);
        }
        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.DELETE_SUCCESS);
        return "redirect:/project/list";
    }

    @RequestMapping(value = "/extractor")
    String extractor(Model model,
                     @RequestParam(value = "id", required = true) String id,
                     @RequestParam(value = "libraryId", required = false) String libraryId,
                     @RequestParam(value = "iRtLibraryId", required = false) String iRtLibraryId,
                     RedirectAttributes redirectAttributes) {
        model.addAttribute("libraryId", libraryId);
        model.addAttribute("iRtLibraryId", iRtLibraryId);

        ResultDO<ProjectDO> resultDO = projectService.getById(id);
        PermissionUtil.check(resultDO.getModel());

        List<ExperimentDO> expList = experimentService.getAllByProjectName(resultDO.getModel().getName());
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.PROJECT_NOT_EXISTED);
            return "redirect:/project/list";
        }

        model.addAttribute("exps", expList);
        model.addAttribute("useEpps", true);
        model.addAttribute("libraries", getLibraryList(0, true));
        model.addAttribute("iRtLibraries", getLibraryList(1, true));
        model.addAttribute("project", resultDO.getModel());
        model.addAttribute("scoreTypes", ScoreType.getShownTypes());

        return "project/extractor";
    }

    @RequestMapping(value = "/doextract")
    String doExtract(Model model,
                     @RequestParam(value = "id", required = true) String id,
                     @RequestParam(value = "ownerName", required = false) String ownerName,
                     @RequestParam(value = "iRtLibraryId", required = false) String iRtLibraryId,
                     @RequestParam(value = "libraryId", required = true) String libraryId,
                     @RequestParam(value = "rtExtractWindow", required = true, defaultValue = "600") Float rtExtractWindow,
                     @RequestParam(value = "mzExtractWindow", required = true, defaultValue = "0.05") Float mzExtractWindow,
                     @RequestParam(value = "note", required = false) String note,
                     //打分相关的入参
                     @RequestParam(value = "sigma", required = false, defaultValue = "3.75") Float sigma,
                     @RequestParam(value = "spacing", required = false, defaultValue = "0.01") Float spacing,
                     @RequestParam(value = "shapeScoreThreshold", required = false, defaultValue = "0.5") Float shapeScoreThreshold,
                     @RequestParam(value = "shapeWeightScoreThreshold", required = false, defaultValue = "0.6") Float shapeWeightScoreThreshold,
                     @RequestParam(value = "useEpps", required = false, defaultValue = "false") Boolean useEpps,
                     HttpServletRequest request,
                     RedirectAttributes redirectAttributes) {

        ResultDO<ProjectDO> projectResult = projectService.getById(id);
        if (projectResult.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.PROJECT_NOT_EXISTED.getMessage());
            return "redirect:/project/extractor?id=" + id;
        }
        PermissionUtil.check(projectResult.getModel());

        ResultDO<LibraryDO> libResult = libraryService.getById(libraryId);
        if (libResult.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.LIBRARY_NOT_EXISTED.getMessage());
            return "redirect:/project/extractor?id=" + id;
        }

        boolean doIrt = false;
        if (iRtLibraryId != null && !iRtLibraryId.isEmpty()) {
            ResultDO<LibraryDO> iRtLibResult = libraryService.getById(iRtLibraryId);
            if (iRtLibResult.isFailed()) {
                redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.IRT_LIBRARY_NOT_EXISTED.getMessage());
                return "redirect:/project/extractor?id=" + id;
            }
            doIrt = true;
        }

        HashSet<String> scoreTypes = new HashSet<>();
        for (ScoreType type : ScoreType.values()) {
            String typeParam = request.getParameter(type.getTypeName());
            if (typeParam != null && typeParam.equals("on")) {
                scoreTypes.add(type.getTypeName());
            }
        }

        List<ExperimentDO> exps = getAllExperimentsByProjectId(id);
        if (exps == null) {
            redirectAttributes.addFlashAttribute(SUCCESS_MSG, ResultCode.NO_EXPERIMENT_UNDER_PROJECT.getMessage());
            return "redirect:/project/list";
        }
        String errorInfo = "";
        TaskTemplate template = null;
        if (doIrt && useEpps) {
            template = TaskTemplate.IRT_EXTRACT_PEAKPICK_SCORE;
        } else if (!doIrt && useEpps) {
            template = TaskTemplate.EXTRACT_PEAKPICK_SCORE;
        } else if (doIrt) {
            template = TaskTemplate.IRT_EXTRACTOR;
        } else {
            template = TaskTemplate.EXTRACTOR;
        }

        for (ExperimentDO exp : exps) {
            if (!doIrt && (exp.getSlope() == null || exp.getIntercept() == null)) {
                errorInfo = errorInfo + ResultCode.IRT_FIRST + ":" + exp.getName() + "(" + exp.getId() + ")";
                continue;
            }

            TaskDO taskDO = new TaskDO(template, exp.getName() + ":" + libResult.getModel().getName() + "(" + libraryId + ")");
            taskService.insert(taskDO);

            LumsParams input = new LumsParams();
            SigmaSpacing ss = new SigmaSpacing(sigma, spacing);
            input.setSigmaSpacing(ss);
            input.setExperimentDO(exp);
            if (doIrt) {
                input.setIRtLibraryId(iRtLibraryId);
            } else {
                SlopeIntercept si = new SlopeIntercept(exp.getSlope(), exp.getIntercept());
                input.setSlopeIntercept(si);
            }
            input.setLibraryId(libraryId);

            if (StringUtils.isEmpty(ownerName)) {
                ownerName = projectResult.getModel().getOwnerName();
            }
            input.setOwnerName(ownerName);
            input.setRtExtractWindow(rtExtractWindow);
            input.setMzExtractWindow(mzExtractWindow);
            input.setUseEpps(useEpps);
            input.setScoreTypes(scoreTypes);

            input.setXcorrShapeThreshold(shapeScoreThreshold);
            input.setXcorrShapeWeightThreshold(shapeWeightScoreThreshold);
            experimentTask.extract(input, taskDO);
        }

        if (StringUtils.isNotEmpty(errorInfo)) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, errorInfo);
        }
        return "redirect:/task/list";
    }

    @RequestMapping(value = "/portionSelector")
    String portionSelector(Model model,
                           @RequestParam(value = "id", required = true) String id,
                           RedirectAttributes redirectAttributes) {
        ResultDO<ProjectDO> resultDO = projectService.getById(id);
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.PROJECT_NOT_EXISTED);
            return "redirect:/project/list";
        }

        PermissionUtil.check(resultDO.getModel());

        List<ExperimentDO> expList = experimentService.getAllByProjectName(resultDO.getModel().getName());
        model.addAttribute("project", resultDO.getModel());
        model.addAttribute("expList", expList);
        return "project/portionSelector";
    }

    @RequestMapping(value = "/overview")
    String overview(Model model,
                    @RequestParam(value = "id", required = true) String projectId,
                    @RequestParam(value = "peptideRefInfo", required = false) String peptideRefInfo,
                    @RequestParam(value = "proteinNameInfo", required = false) String proteinNameInfo,
                    HttpServletRequest request,
                    RedirectAttributes redirectAttributes) {

        //get project name
        ResultDO<ProjectDO> resultDO = projectService.getById(projectId);
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.PROJECT_NOT_EXISTED);
            return "redirect:/project/list";
        }

        PermissionUtil.check(resultDO.getModel());
        String projectName = resultDO.getModel().getName() + "(" + resultDO.getModel().getId() + ")";
        //get corresponding experiments
        List<ExperimentDO> expList = experimentService.getAllByProjectName(resultDO.getModel().getName());
        String libraryId = "";
        String libName = "";
        List<String> analyseOverviewIdList = new ArrayList<>();
        List<String> expNameList = new ArrayList<>();
        for (ExperimentDO experimentDO : expList) {
            String checkState = request.getParameter(experimentDO.getId());
            if (checkState != null && checkState.equals("on")) {
                //analyse checked experiments
                AnalyseOverviewDO analyseOverviewDO = analyseOverviewService.getAllByExpId(experimentDO.getId()).get(0);
                libraryId = analyseOverviewDO.getLibraryId();
                libName = analyseOverviewDO.getLibraryName() + "(" + libraryId + ")";
                analyseOverviewIdList.add(analyseOverviewDO.getId());
                expNameList.add(experimentDO.getName());
            }
        }
        HashMap<String, PeptideDO> peptideDOMap = new HashMap<>();
        List<String> protNameList = new ArrayList<>();
        HashMap<String, HashMap<String, List<Integer>>> pepFragIntListMap = new HashMap<>();
        HashMap<String, String> intMap = new HashMap<>();
        if (!proteinNameInfo.isEmpty()) {
            for (String proteinName : proteinNameInfo.split(";")) {
                List<PeptideDO> peptideDOList = peptideService.getAllByLibraryIdAndProteinNameAndIsDecoy(libraryId, proteinName, false);
                for (PeptideDO peptideDO : peptideDOList) {
                    peptideDOMap.put(peptideDO.getPeptideRef(), peptideDO);
                }
            }
        }
        if (!peptideRefInfo.isEmpty()) {
            String[] peptideRefs = peptideRefInfo.split(";");
            for (String peptideRef : peptideRefs) {
                PeptideDO peptideDO = peptideService.getByLibraryIdAndPeptideRefAndIsDecoy(libraryId, peptideRef, false);
                peptideDOMap.put(peptideRef, peptideDO);
            }
        }

        HashMap<String, List<Boolean>> identifyMap = new HashMap<>();
        for (PeptideDO peptideDO : peptideDOMap.values()) {
            //protein name
            protNameList.add(peptideDO.getProteinName());
            //fragment cutInfo list
            Set<String> cutInfoSet = peptideDO.getFragmentMap().keySet();
            //experiments
            HashMap<String, List<Integer>> fragIntListMap = new HashMap<>();
            String intOverall = "";
            List<Boolean> identifyStatList = new ArrayList<>();
            for (String analyseOverviewId : analyseOverviewIdList) {
                //get fragment intensity map
                AnalyseDataDO analyseDataDO = analyseDataService.getByOverviewIdAndPeptideRefAndIsDecoy(analyseOverviewId, peptideDO.getPeptideRef(), false);
                Map<String, Double> fragIntMap;
                if (analyseDataDO == null) {
                    fragIntMap = new HashMap<>();
                    identifyStatList.add(false);
                } else {
                    if (analyseDataDO.getIdentifiedStatus() == 0) {
                        identifyStatList.add(true);
                    } else {
                        identifyStatList.add(false);
                    }
                    fragIntMap = FeatureUtil.toMap(analyseDataDO.getFragIntFeature());
                    intOverall += analyseDataDO.getIntensitySum() + ", ";
                }

                //get fragment intensity list map
                for (String cutInfo : cutInfoSet) {
                    if (fragIntListMap.get(cutInfo) == null) {
                        List<Integer> newList = new ArrayList<>();
                        newList.add(fragIntMap.get(cutInfo) == null ? 0 : (int) Math.round(fragIntMap.get(cutInfo)));
                        fragIntListMap.put(cutInfo, newList);
                    } else {
                        fragIntListMap.get(cutInfo).add(fragIntMap.get(cutInfo) == null ? 0 : (int) Math.round(fragIntMap.get(cutInfo)));
                    }
                }
            }
            if (intOverall.isEmpty()) {
                intOverall = "0";
            }
            identifyMap.put(peptideDO.getPeptideRef(), identifyStatList);
            intMap.put(peptideDO.getPeptideRef(), intOverall);
            pepFragIntListMap.put(peptideDO.getPeptideRef(), fragIntListMap);
        }

        //横坐标实验，纵坐标不同pep
        model.addAttribute("projectName", projectName);
        model.addAttribute("libraryId", libraryId);
        model.addAttribute("libName", libName);
//        model.addAttribute("peptideRefs", peptideRefs);
        model.addAttribute("protNameList", protNameList);
        model.addAttribute("pepFragIntListMap", pepFragIntListMap);
        model.addAttribute("expNameList", expNameList);
        model.addAttribute("intMap", intMap);
        model.addAttribute("identifyMap", identifyMap);

        return "project/overview";
    }

    @RequestMapping(value = "/writeToFile")
    String writeToFile(Model model,
                       @RequestParam(value = "id", required = true) String id,
                       RedirectAttributes redirectAttributes) {

        ProjectDO projectDO = projectService.getById(id).getModel();
        PermissionUtil.check(projectDO);
        List<ExperimentDO> experimentDOList = getAllExperimentsByProjectId(id);
        String defaultOutputPath = projectDO.getRepository() + "/Propro_" + projectDO.getName() + ".tsv";
        model.addAttribute("expList", experimentDOList);
        model.addAttribute("project", projectDO);
        model.addAttribute("defaultPathName", defaultOutputPath);
        model.addAttribute("outputAllPeptides", false);
        return "project/outputSelector";
    }

    @RequestMapping(value = "/doWriteToFile", method = RequestMethod.POST)
    String doWriteToFile(Model model,
                         @RequestParam(value = "projectId", required = true) String projectId,
                         @RequestParam(value = "pathName", required = true) String pathName,
                         @RequestParam(value = "outputAllPeptides", required = false, defaultValue = "false") Boolean outputAllPeptides,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes) {

        ProjectDO projectDO = projectService.getById(projectId).getModel();
        PermissionUtil.check(projectDO);

        List<ExperimentDO> experimentDOList = getAllExperimentsByProjectId(projectId);
        HashMap<String, HashMap<String, String>> intensityMap = new HashMap<>();
        HashMap<String, String> pepToProt = new HashMap<>();
        if (outputAllPeptides) {
            List<PeptideDO> peptideDOList = peptideService.getAllByLibraryIdAndIsDecoy(analyseOverviewService.getAllByExpId(experimentDOList.get(0).getId()).get(0).getLibraryId(), false);
            for (PeptideDO peptideDO : peptideDOList) {
                intensityMap.put(peptideDO.getPeptideRef(), new HashMap<>());
                pepToProt.put(peptideDO.getPeptideRef(), peptideDO.getProteinName());
            }
            for (ExperimentDO experimentDO : experimentDOList) {
                String checkState = request.getParameter(experimentDO.getId());
                if (checkState != null && checkState.equals("on")) {
                    List<AnalyseDataDO> analyseDataDOList = analyseDataService.getAllByOverviewId(analyseOverviewService.getAllByExpId(experimentDO.getId()).get(0).getId());
                    for (AnalyseDataDO analyseDataDO : analyseDataDOList) {
                        if (analyseDataDO.getIdentifiedStatus() == AnalyseDataDO.IDENTIFIED_STATUS_SUCCESS) {
                            intensityMap.get(analyseDataDO.getPeptideRef()).put(experimentDO.getName(), analyseDataDO.getIntensitySum().toString());
                        } else {
                            intensityMap.get(analyseDataDO.getPeptideRef()).put(experimentDO.getName(), "Unidentified");
                        }
                    }
                }
            }
        } else {
            for (ExperimentDO experimentDO : experimentDOList) {
                String checkState = request.getParameter(experimentDO.getId());
                if (checkState != null && checkState.equals("on")) {
                    List<AnalyseDataDO> analyseDataDOList = analyseDataService.getAllByOverviewId(analyseOverviewService.getAllByExpId(experimentDO.getId()).get(0).getId());
                    for (AnalyseDataDO analyseDataDO : analyseDataDOList) {
                        if (analyseDataDO.getIdentifiedStatus() == AnalyseDataDO.IDENTIFIED_STATUS_SUCCESS) {
                            if (intensityMap.containsKey(analyseDataDO.getPeptideRef())) {
                                intensityMap.get(analyseDataDO.getPeptideRef()).put(experimentDO.getName(), analyseDataDO.getIntensitySum().toString());
                            } else {
                                HashMap<String, String> map = new HashMap<>();
                                map.put(experimentDO.getName(), analyseDataDO.getIntensitySum().toString());
                                intensityMap.put(analyseDataDO.getPeptideRef(), map);
                                pepToProt.put(analyseDataDO.getPeptideRef(), analyseDataDO.getProteinName());
                            }
                        }
                    }
                }
            }
        }

        try {
            File file = new File(pathName);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            writer.append("ProteinName").append("\t").append("PeptideRef");
            for (ExperimentDO experimentDO : experimentDOList) {
                writer.append("\t").append(experimentDO.getName());
            }
            writer.append("\r");
            for (String peptideRef : intensityMap.keySet()) {
                writer.append(pepToProt.get(peptideRef)).append("\t").append(peptideRef);
                for (ExperimentDO experimentDO : experimentDOList) {
                    if (intensityMap.get(peptideRef).containsKey(experimentDO.getName())) {
                        writer.append("\t").append(intensityMap.get(peptideRef).get(experimentDO.getName()));
                    } else {
                        writer.append("\t");
                    }
                }
                writer.append("\r");
            }
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "redirect:/project/list";
    }

    private List<ExperimentDO> getAllExperimentsByProjectId(String projectId) {
        ExperimentQuery query = new ExperimentQuery();
        query.setProjectId(projectId);
        return experimentService.getAll(query);
    }
}
