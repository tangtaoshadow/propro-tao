package com.westlake.air.pecs.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.constants.SuccessMsg;
import com.westlake.air.pecs.constants.TaskTemplate;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.analyse.WindowRang;
import com.westlake.air.pecs.domain.bean.score.SlopeIntercept;
import com.westlake.air.pecs.domain.db.*;
import com.westlake.air.pecs.domain.query.ExperimentQuery;
import com.westlake.air.pecs.domain.query.ScanIndexQuery;
import com.westlake.air.pecs.parser.MzMLParser;
import com.westlake.air.pecs.parser.MzXMLParser;
import com.westlake.air.pecs.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
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
    TransitionService transitionService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    AnalyseDataService analyseDataService;
    @Autowired
    MzXMLParser mzXMLParser;
    @Autowired
    MzMLParser mzMLParser;
    @Autowired
    ScanIndexService scanIndexService;

    @RequestMapping(value = "/list")
    String list(Model model,
                @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize,
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
        List<LibraryDO> slist = getLibraryList(0);
        List<LibraryDO> iRtlist = getLibraryList(1);
        model.addAttribute("libraries", slist);
        model.addAttribute("iRtLibraries", iRtlist);
        return "experiment/create";
    }

    @RequestMapping(value = "/add")
    String add(Model model,
               @RequestParam(value = "name", required = true) String name,
               @RequestParam(value = "fileLocation", required = true) String fileLocation,
               @RequestParam(value = "description", required = false) String description,
               RedirectAttributes redirectAttributes) {

        model.addAttribute("libraries", getLibraryList(0));
        model.addAttribute("iRtLibraries", getLibraryList(1));

        model.addAttribute("name", name);
        model.addAttribute("description", description);

        //Check Params Start
        if (fileLocation == null || fileLocation.isEmpty()) {
            model.addAttribute(ERROR_MSG, ResultCode.FILE_LOCATION_CANNOT_BE_EMPTY.getMessage());
            return "experiment/create";
        }
        model.addAttribute("fileLocation", fileLocation);
        File file = new File(fileLocation);

        if (!file.exists()) {
            model.addAttribute(ERROR_MSG, ResultCode.FILE_NOT_EXISTED.getMessage());
            return "experiment/create";
        }

        ExperimentDO experimentDO = new ExperimentDO();
        experimentDO.setName(name);
        experimentDO.setFileType(fileLocation.substring(fileLocation.lastIndexOf(".") + 1).trim().toLowerCase());
        experimentDO.setDescription(description);
        experimentDO.setFileLocation(fileLocation);

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

    @RequestMapping(value = "/edit/{id}")
    String edit(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {

        model.addAttribute("libraries", getLibraryList(0));
        model.addAttribute("iRtLibraries", getLibraryList(1));
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
                  @RequestParam(value = "fileType") String fileType,
                  @RequestParam(value = "fileLocation") String fileLocation,
                  @RequestParam(value = "description") String description,
                  RedirectAttributes redirectAttributes) {

        ResultDO<ExperimentDO> resultDO = experimentService.getById(id);
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/experiment/list";
        }
        ExperimentDO experimentDO = resultDO.getModel();

        experimentDO.setName(name);
        experimentDO.setFileType(fileType);
        experimentDO.setFileLocation(fileLocation);
        experimentDO.setDescription(description);

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

        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.DELETE_LIBRARY_SUCCESS);
        return "redirect:/experiment/list";

    }

    @RequestMapping(value = "/extractor")
    String extractor(Model model,
                     @RequestParam(value = "id", required = true) String id,
                     @RequestParam(value = "libraryId", required = false) String libraryId,
                     RedirectAttributes redirectAttributes) {
        model.addAttribute("libraryId",libraryId);

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
                     @RequestParam(value = "buildType", required = true) Integer buildType,
                     @RequestParam(value = "creator", required = false) String creator,
                     @RequestParam(value = "libraryId", required = true) String libraryId,
                     @RequestParam(value = "rtExtractWindow", required = true, defaultValue = "600") Float rtExtractWindow,
                     @RequestParam(value = "mzExtractWindow", required = true, defaultValue = "0.05") Float mzExtractWindow,
                     RedirectAttributes redirectAttributes) {
        if (rtExtractWindow == null) {
            rtExtractWindow = 1200f;
        }
        if (mzExtractWindow == null) {
            mzExtractWindow = 0.05f;
        }

        ResultDO<ExperimentDO> resultDO = experimentService.getById(id);
        if (resultDO.isFailed()) {
            return "redirect:/extractor/" + id;
        }

        TaskDO taskDO = new TaskDO(TaskTemplate.SWATH_CONVOLUTION, resultDO.getModel().getName());
        taskService.insert(taskDO);
        experimentTask.extract(resultDO.getModel(), libraryId, new SlopeIntercept(), creator, rtExtractWindow, mzExtractWindow, buildType, taskDO);

        return "redirect:/task/detail/" + taskDO.getId();
    }

    @RequestMapping(value = "/getWindows")
    @ResponseBody
    ResultDO<JSONObject> getWindows(Model model,
                                    @RequestParam(value = "expId", required = false) String expId) {

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

    @RequestMapping(value = "/compressor")
    String compressor(Model model) {
        return "experiment/compressor";
    }
}
