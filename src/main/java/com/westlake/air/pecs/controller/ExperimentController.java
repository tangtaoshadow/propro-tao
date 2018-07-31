package com.westlake.air.pecs.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.westlake.air.pecs.constants.Constants;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.constants.SuccessMsg;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.bean.WindowRang;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import com.westlake.air.pecs.domain.query.ExperimentQuery;
import com.westlake.air.pecs.domain.query.ScanIndexQuery;
import com.westlake.air.pecs.parser.MzMLParser;
import com.westlake.air.pecs.parser.MzXMLParser;
import com.westlake.air.pecs.service.ExperimentService;
import com.westlake.air.pecs.service.LibraryService;
import com.westlake.air.pecs.service.ScanIndexService;
import com.westlake.air.pecs.service.TransitionService;
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
        List<LibraryDO> list = libraryService.getAll();
        model.addAttribute("libraries", list);
        return "experiment/create";
    }

    @RequestMapping(value = "/add")
    String add(Model model,
               @RequestParam(value = "name", required = true) String name,
               @RequestParam(value = "description", required = false) String description,
               @RequestParam(value = "fileLocation", required = true) String fileLocation,
               @RequestParam(value = "sLibraryId", required = false) String sLibraryId,
               @RequestParam(value = "vLibraryId", required = false) String vLibraryId,
               RedirectAttributes redirectAttributes) {

        model.addAttribute("sLibraries", getLibraryList(0));
        model.addAttribute("vLibraries", getLibraryList(1));

        model.addAttribute("name", name);
        model.addAttribute("description", description);
        model.addAttribute("sLibraryId", sLibraryId);
        model.addAttribute("vLibraryId", vLibraryId);

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

        ResultDO<LibraryDO> resultSLib = libraryService.getById(sLibraryId);
        if (resultSLib.isSuccess()) {
            experimentDO.setSLibraryId(sLibraryId);
            experimentDO.setSLibraryName(resultSLib.getModel().getName());
        }

        ResultDO<LibraryDO> resultVLib = libraryService.getById(vLibraryId);
        if (resultVLib.isSuccess()) {
            experimentDO.setSLibraryId(vLibraryId);
            experimentDO.setSLibraryName(resultVLib.getModel().getName());
        }

        ResultDO result = experimentService.insert(experimentDO);
        if (result.isFailed()) {
            model.addAttribute(ERROR_MSG, result.getMsgInfo());
            return "experiment/create";
        }

        try {
            long start = System.currentTimeMillis();
            //建立索引
            logger.info("开始构建索引");
            List<ScanIndexDO> indexList = null;
            //传入不同的文件类型会调用不同的解析层
            if (experimentDO.getFileType().equals(Constants.EXP_SUFFIX_MZXML)) {
                indexList = mzXMLParser.index(file, experimentDO.getId());
            } else if (experimentDO.getFileType().equals(Constants.EXP_SUFFIX_MZML)){
                indexList = mzMLParser.index(file, experimentDO.getId());
            }

            logger.info("索引构建完毕,开始存储索引");
            ResultDO resultDO = scanIndexService.insertAll(indexList, true);
            logger.info("索引存储完毕");

            if (resultDO.isFailed()) {
                logger.info("索引存储失败" + result.getMsgInfo());
                experimentService.delete(experimentDO.getId());
                model.addAttribute(ERROR_MSG, result.getMsgInfo());
                return "experiment/create";
            } else {
                redirectAttributes.addAttribute(SUCCESS_MSG, SuccessMsg.CREATE_EXPERIMENT_AND_INDEX_SUCCESS + ",耗时:" + (System.currentTimeMillis() - start) + "毫秒");
                return "redirect:/experiment/list";
            }

        } catch (Exception e) {
            logger.info("索引存储失败", e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute(ERROR_MSG, e.getMessage());
            return "redirect:/experiment/list";
        }
    }

    @RequestMapping(value = "/edit/{id}")
    String edit(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {

        model.addAttribute("sLibraries", getLibraryList(0));
        model.addAttribute("vLibraries", getLibraryList(1));
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
                  @RequestParam(value = "sLibraryId", required = false) String sLibraryId,
                  @RequestParam(value = "vLibraryId", required = false) String vLibraryId,
                  RedirectAttributes redirectAttributes) {

        ResultDO<ExperimentDO> resultDO = experimentService.getById(id);
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/experiment/list";
        }
        ExperimentDO experimentDO = resultDO.getModel();

        ResultDO<LibraryDO> resultSLib = libraryService.getById(sLibraryId);
        if (resultSLib.isSuccess()) {
            experimentDO.setSLibraryId(sLibraryId);
            experimentDO.setSLibraryName(resultSLib.getModel().getName());
        }

        ResultDO<LibraryDO> resultVLib = libraryService.getById(vLibraryId);
        if (resultVLib.isSuccess()) {
            experimentDO.setSLibraryId(vLibraryId);
            experimentDO.setSLibraryName(resultVLib.getModel().getName());
        }

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
        ResultDO resultDO = experimentService.delete(id);

        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.DELETE_LIBRARY_SUCCESS);
        return "redirect:/experiment/list";

    }

    @RequestMapping(value = "/extract")
    String extract(Model model,
                   @RequestParam(value = "id", required = true) String id,
                   @RequestParam(value = "buildType", required = true) int buildType,
                   @RequestParam(value = "creator", required = false) String creator,
                   @RequestParam(value = "rtExtractWindow", required = true, defaultValue = "1.0") Float rtExtractWindow,
                   @RequestParam(value = "mzExtractWindow", required = true, defaultValue = "0.05") Float mzExtractWindow,
                   RedirectAttributes redirectAttributes) {
        if (rtExtractWindow == null) {
            rtExtractWindow = 1.0f;
        }
        if (mzExtractWindow == null) {
            mzExtractWindow = 0.05f;
        }
        redirectAttributes.addFlashAttribute("rtExtractWindow", rtExtractWindow);
        redirectAttributes.addFlashAttribute("mzExtractWindow", mzExtractWindow);
        redirectAttributes.addFlashAttribute("buildType", buildType);
        redirectAttributes.addFlashAttribute("creator", creator);

        try {
            long start = System.currentTimeMillis();
            ResultDO resultDO = experimentService.extract(id, creator, rtExtractWindow, mzExtractWindow, buildType);
            if(resultDO.isFailed()){
                redirectAttributes.addFlashAttribute(ERROR_MSG,resultDO.getMsgInfo());
                return "redirect:/experiment/detail/"+id;
            }
            logger.info("全部卷积完成,总共耗时:" + (System.currentTimeMillis() - start));
        } catch (IOException e) {
            logger.error("卷积报错了:", e);
            e.printStackTrace();
        }

        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.EXTRACT_DATA_SUCCESS);
        return "redirect:/analyse/overview/list?expId=" + id;
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
        for(int i = 0 ;i<rangs.size();i++){
            indexArray.add((int)(rangs.get(i).getMs2Interval()*1000)+"ms");
            mzStartArray.add(rangs.get(i).getMzStart());
            mzRangArray.add((rangs.get(i).getMzEnd()-rangs.get(i).getMzStart()));
        }
        res.put("indexes", indexArray);
        res.put("starts", mzStartArray);
        res.put("rangs", mzRangArray);
        res.put("min", rangs.get(0).getMzStart());
        res.put("max", rangs.get(rangs.size()-1).getMzEnd());
        resultDO.setModel(res);
        return resultDO;
    }
}
