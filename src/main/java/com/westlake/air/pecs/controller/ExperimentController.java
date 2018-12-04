package com.westlake.air.pecs.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.westlake.air.pecs.compressor.Compressor;
import com.westlake.air.pecs.constants.*;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.SwathInput;
import com.westlake.air.pecs.domain.bean.analyse.SigmaSpacing;
import com.westlake.air.pecs.domain.bean.analyse.WindowRang;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.*;
import com.westlake.air.pecs.domain.params.Exp;
import com.westlake.air.pecs.domain.params.ExpModel;
import com.westlake.air.pecs.domain.query.ExperimentQuery;
import com.westlake.air.pecs.domain.query.ScanIndexQuery;
import com.westlake.air.pecs.parser.MzXMLParser;
import com.westlake.air.pecs.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.util.List;

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
    Compressor compressor;

    @RequestMapping(value = "/list")
    String list(Model model,
                @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                @RequestParam(value = "pageSize", required = false, defaultValue = "50") Integer pageSize,
                @RequestParam(value = "searchName", required = false) String searchName) {
        model.addAttribute("searchName", searchName);
        model.addAttribute("pageSize", pageSize);
        ExperimentQuery query = new ExperimentQuery();
        if (searchName != null && !searchName.isEmpty()) {
            query.setName(searchName);
        }
        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        ResultDO<List<ExperimentDO>> resultDO = experimentService.getList(query);

        model.addAttribute("experiments", resultDO.getModel());
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);
        return "experiment/list";
    }

    @RequestMapping(value = "/create")
    String create(Model model) {
        return "experiment/create";
    }

    @RequestMapping(value = "/batchcreate")
    String batchCreate(Model model) {
        return "experiment/batchcreate";
    }

    @RequestMapping(value = "/add")
    String add(Model model,
               @RequestParam(value = "name", required = true) String name,
               @RequestParam(value = "filePath", required = true) String filePath,
               @RequestParam(value = "description", required = false) String description,
               @RequestParam(value = "overlap", required = false) Float overlap,
               RedirectAttributes redirectAttributes) {

        model.addAttribute("overlap", overlap);
        model.addAttribute("name", name);
        model.addAttribute("description", description);

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

        ResultDO result = experimentService.insert(experimentDO);
        if (result.isFailed()) {
            model.addAttribute(ERROR_MSG, result.getMsgInfo());
            return "experiment/create";
        }//Check Params End

        TaskDO taskDO = new TaskDO(TaskTemplate.UPLOAD_EXPERIMENT_FILE, experimentDO.getName());
        taskService.insert(taskDO);

        experimentTask.saveExperimentTask(experimentDO, file, taskDO);
        return "redirect:/task/detail/" + taskDO.getId();
    }

    @RequestMapping(value = "/batchadd", method = {RequestMethod.POST})
    String batchAdd(Model model, ExpModel exps, RedirectAttributes redirectAttributes) {

        String errorInfo = "";
        List<Exp> expList = exps.getExps();
        for (Exp exp : expList) {
            if (exp.getFilePath() == null || exp.getFilePath().isEmpty()) {
                errorInfo += ResultCode.FILE_LOCATION_CANNOT_BE_EMPTY.getMessage() + ":" + exp.getName() + "\r\n";
            }
            File file = new File(exp.getFilePath());

            if (!file.exists()) {
                errorInfo += ResultCode.FILE_NOT_EXISTED.getMessage() + ":" + exp.getFilePath() + "\r\n";
            }

            ExperimentDO experimentDO = new ExperimentDO();
            experimentDO.setName(exp.getName());
            experimentDO.setDescription(exp.getDescription());
            experimentDO.setOverlap(exp.getOverlap());
            experimentDO.setFilePath(exp.getFilePath());

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
                experimentTask.saveExperimentTask(experimentDO, file, taskDO);
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
            return "/experiment/edit";
        }
    }

    @RequestMapping(value = "/detail/{id}")
    String detail(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        ResultDO<ExperimentDO> resultDO = experimentService.getById(id);

        ScanIndexQuery query = new ScanIndexQuery();
        query.setExperimentId(id);
        query.setMsLevel(1);
        Long ms1Count = scanIndexService.count(query);
        query.setMsLevel(2);
        Long ms2Count = scanIndexService.count(query);
        if (resultDO.isSuccess()) {
            model.addAttribute("experiment", resultDO.getModel());
            model.addAttribute("ms1Count", ms1Count);
            model.addAttribute("ms2Count", ms2Count);
            return "/experiment/detail";
        } else {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/experiment/list";
        }
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    String update(Model model,
                  @RequestParam(value = "id", required = true) String id,
                  @RequestParam(value = "name") String name,
                  @RequestParam(value = "iRtLibraryId") String iRtLibraryId,
                  @RequestParam(value = "slope") Double slope,
                  @RequestParam(value = "intercept") Double intercept,
                  @RequestParam(value = "filePath") String filePath,
                  @RequestParam(value = "airdPath") String airdPath,
                  @RequestParam(value = "airdIndexPath") String airdIndexPath,
                  @RequestParam(value = "description") String description,
                  @RequestParam(value = "compressionType") String compressionType,
                  @RequestParam(value = "precision") String precision,
                  RedirectAttributes redirectAttributes) {

        ResultDO<ExperimentDO> resultDO = experimentService.getById(id);
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/experiment/list";
        }
        ExperimentDO experimentDO = resultDO.getModel();

        experimentDO.setName(name);
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
        experimentService.delete(id);
        scanIndexService.deleteAllByExperimentId(id);
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
            return "/experiment/swath";
        }

        ExperimentDO exp = expResult.getModel();

        TaskDO taskDO = new TaskDO(TaskTemplate.SWATH_WORKFLOW, "expId:" + expId + ";libId:" + libraryId + ";iRtLib:" + iRtLibraryId);
        taskService.insert(taskDO);

        SwathInput input = new SwathInput();
        input.setExperimentDO(exp);
        input.setIRtLibraryId(iRtLibraryId);
        input.setLibraryId(libraryId);
        input.setCreator("Admin");
        input.setRtExtractWindow(rtExtractWindow);
        input.setMzExtractWindow(mzExtractWindow);
        input.setBuildType(2);
        input.setSigmaSpacing(new SigmaSpacing(sigma, spacing));

        experimentTask.swath(input, taskDO);
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

        model.addAttribute("libraries", getLibraryList(0));
        model.addAttribute("experiment", resultDO.getModel());
        return "/experiment/extractor";
    }

    @RequestMapping(value = "/doextract")
    String doExtract(Model model,
                     @RequestParam(value = "id", required = true) String id,
                     @RequestParam(value = "creator", required = false) String creator,
                     @RequestParam(value = "libraryId", required = true) String libraryId,
                     @RequestParam(value = "rtExtractWindow", required = true, defaultValue = "1200") Float rtExtractWindow,
                     @RequestParam(value = "mzExtractWindow", required = true, defaultValue = "0.05") Float mzExtractWindow,
                     @RequestParam(value = "slope", required = false) Float slope,
                     @RequestParam(value = "intercept", required = false) Float intercept,
                     RedirectAttributes redirectAttributes) {

        ResultDO<ExperimentDO> resultDO = experimentService.getById(id);
        if (resultDO.isFailed()) {
            return "redirect:/extractor?id=" + id;
        }

        TaskDO taskDO = new TaskDO(TaskTemplate.EXTRACTOR, resultDO.getModel().getName() + ":" + libraryId);
        taskService.insert(taskDO);
        SlopeIntercept si = SlopeIntercept.create();
        if (slope != null && intercept != null) {
            si.setSlope(slope);
            si.setIntercept(intercept);
        }

        experimentTask.extract(resultDO.getModel(), libraryId, si, creator, rtExtractWindow, mzExtractWindow, taskDO);

        return "redirect:/task/detail/" + taskDO.getId();
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
        return "/experiment/irt";
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
    ResultDO<JSONObject> getWindows(Model model, @RequestParam(value = "expId", required = false) String expId) {
        List<WindowRang> rangs = experimentService.getWindows(expId);
        ResultDO<JSONObject> resultDO = new ResultDO<>(true);

        JSONObject res = new JSONObject();
        JSONArray indexArray = new JSONArray();
        JSONArray mzStartArray = new JSONArray();
        JSONArray mzRangArray = new JSONArray();
        for (int i = 0; i < rangs.size(); i++) {
            indexArray.add((int) (rangs.get(i).getMs2Interval() * 1000) + "ms");
            mzStartArray.add(rangs.get(i).getMzStart());
            mzRangArray.add((rangs.get(i).getMzEnd() - rangs.get(i).getMzStart()));
        }
        res.put("indexes", indexArray);
        res.put("starts", mzStartArray);
        res.put("rangs", mzRangArray);
        res.put("min", rangs.get(0).getMzStart());
        res.put("max", rangs.get(rangs.size() - 1).getMzEnd());
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
        TaskDO taskDO = new TaskDO(TaskTemplate.COMPRESSOR_AND_SORT, experimentDO.getName() + ":" + expId);
        taskService.insert(taskDO);
        experimentTask.compress(experimentDO, taskDO);

        return "redirect:/task/detail/" + taskDO.getId();
    }

    @RequestMapping(value = "/compressToLMS")
    String compressToLMS(Model model,
                    @RequestParam(value = "expId", required = true) String expId,
                    RedirectAttributes redirectAttributes) {

        ResultDO<ExperimentDO> resultDO = experimentService.getById(expId);
        if (resultDO.isFailed()) {
            redirectAttributes.addAttribute(ERROR_MSG, ResultCode.EXPERIMENT_NOT_EXISTED.getMessage());
            return "redirect:/experiment/list";
        }
        ExperimentDO experimentDO = resultDO.getModel();
        TaskDO taskDO = new TaskDO(TaskTemplate.COMPRESSOR_AND_SORT, experimentDO.getName() + ":" + expId);
        taskService.insert(taskDO);
        experimentTask.compressToLMS(experimentDO, taskDO);

        return "redirect:/task/detail/" + taskDO.getId();
    }
}
