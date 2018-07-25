package com.westlake.air.pecs.controller;

import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.constants.SuccessMsg;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.domain.db.ScanIndexDO;
import com.westlake.air.pecs.domain.query.ExperimentQuery;
import com.westlake.air.pecs.domain.query.ScanIndexQuery;
import com.westlake.air.pecs.parser.MzXmlParser;
import com.westlake.air.pecs.parser.indexer.LmsIndexer;
import com.westlake.air.pecs.service.ExperimentService;
import com.westlake.air.pecs.service.LibraryService;
import com.westlake.air.pecs.service.ScanIndexService;
import com.westlake.air.pecs.service.TransitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
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
    MzXmlParser mzXmlParser;

    @Autowired
    LmsIndexer lmsIndexer;

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
               @RequestParam(value = "libraryId", required = false) String libraryId,
               RedirectAttributes redirectAttributes) {

        model.addAttribute("libraries", getLibraryList());

        if (name != null && !name.isEmpty()) {
            model.addAttribute("name", name);
        }
        if (description != null && !description.isEmpty()) {
            model.addAttribute("description", description);
        }
        if (libraryId != null && !libraryId.isEmpty()) {
            model.addAttribute("libraryId", libraryId);
        }

        if (fileLocation == null || fileLocation.isEmpty()) {
            model.addAttribute(ERROR_MSG, ResultCode.FILE_LOCATION_CANNOT_BE_EMPTY);
            return "experiment/create";
        }

        File file = new File(fileLocation);

        if (!file.exists()) {
            model.addAttribute(ERROR_MSG, ResultCode.FILE_NOT_EXISTED);
            return "experiment/create";
        }

//      File file = new File("H:\\data\\weissto_i170508_005-SWLYPB125.mzXML");
//      File file = new File(getClass().getClassLoader().getResource("data/MzXMLFile_1_compressed.mzXML").getPath());
//      File file = new File("D:\\data\\wlym5.mzXML");
//      File file = new File("D:\\testdata\\testfile.mzXML");

        ExperimentDO experimentDO = new ExperimentDO();
        experimentDO.setName(name);
        experimentDO.setDescription(description);
        experimentDO.setFileLocation(fileLocation);

        ResultDO<LibraryDO> resultLib = libraryService.getById(libraryId);
        if (resultLib.isSuccess()) {
            experimentDO.setLibraryId(libraryId);
            experimentDO.setLibraryName(resultLib.getModel().getName());
        }

        ResultDO result = experimentService.insert(experimentDO);
        if (result.isFailured()) {
            model.addAttribute(ERROR_MSG, result.getMsgInfo());
            return "experiment/create";
        }

        try {
            long start = System.currentTimeMillis();
            //建立索引
            logger.info("开始构建索引");
            List<ScanIndexDO> indexList = lmsIndexer.index(file, experimentDO.getId());
            logger.info("索引构建完毕,开始存储索引");
            ResultDO resultDO = scanIndexService.insertAll(indexList, true);
            logger.info("索引存储完毕");

            if (resultDO.isFailured()) {
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

        model.addAttribute("libraries", getLibraryList());
        ResultDO<ExperimentDO> resultDO = experimentService.getById(id);
        if (resultDO.isFailured()) {
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
                  @RequestParam(value = "description") String description,
                  @RequestParam(value = "libraryId") String libraryId,
                  RedirectAttributes redirectAttributes) {

        ResultDO<ExperimentDO> resultDO = experimentService.getById(id);
        if (resultDO.isFailured()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/experiment/list";
        }
        ResultDO<LibraryDO> resultLib = libraryService.getById(libraryId);
        ExperimentDO experimentDO = resultDO.getModel();
        if (resultLib.isSuccess()) {
            experimentDO.setLibraryId(libraryId);
            experimentDO.setLibraryName(resultLib.getModel().getName());
        }
        experimentDO.setDescription(description);

        ResultDO result = experimentService.update(experimentDO);
        if (result.isFailured()) {
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
        try {
            ResultDO resultDO = experimentService.extract(id, rtExtractWindow, mzExtractWindow, buildType);
        } catch (IOException e) {
            logger.error("卷积报错了:", e);
            e.printStackTrace();
        }

        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.EXTRACT_DATA_SUCCESS);
        return "redirect:/experiment/detail/" + id;

    }

    @RequestMapping(value = "/quickscan")
    String quickScan(Model model, RedirectAttributes redirectAttributes) {

        File file = new File("H:\\data\\weissto_i170508_005-SWLYPB125.mzXML");
//    File file = new File("D:\\data\\wlym5.mzXML");
//    File file = new File("D:\\testdata\\testfile.mzXML");

        try {
            Long time = System.currentTimeMillis();
            List<ScanIndexDO> indexList = lmsIndexer.index(file);
            System.out.println("Cost:" + (System.currentTimeMillis() - time));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "experiment/list";
    }
}
