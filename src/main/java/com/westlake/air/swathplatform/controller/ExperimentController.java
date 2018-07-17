package com.westlake.air.swathplatform.controller;

import com.westlake.air.swathplatform.constants.ResultCode;
import com.westlake.air.swathplatform.constants.SuccessMsg;
import com.westlake.air.swathplatform.domain.ResultDO;
import com.westlake.air.swathplatform.domain.db.ExperimentDO;
import com.westlake.air.swathplatform.domain.db.LibraryDO;
import com.westlake.air.swathplatform.domain.db.ScanIndexDO;
import com.westlake.air.swathplatform.domain.query.ExperimentQuery;
import com.westlake.air.swathplatform.parser.MzXmlParser;
import com.westlake.air.swathplatform.parser.indexer.LmsIndexer;
import com.westlake.air.swathplatform.service.ExperimentService;
import com.westlake.air.swathplatform.service.LibraryService;
import com.westlake.air.swathplatform.service.ScanIndexService;
import com.westlake.air.swathplatform.service.TransitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
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

        ResultDO result = experimentService.save(experimentDO);
        if (result.isFailured()) {
            model.addAttribute(ERROR_MSG, result.getMsgInfo());
            return "experiment/create";
        }

        try {
            //建立索引
            Long time = System.currentTimeMillis();
            List<ScanIndexDO> indexList = lmsIndexer.index(file, experimentDO.getId());
            ResultDO resultDO = scanIndexService.insertAll(indexList, true);
            System.out.println("Cost:" + (System.currentTimeMillis() - time));
            if(resultDO.isFailured()){
                experimentService.delete(experimentDO.getId());
                model.addAttribute(ERROR_MSG, result.getMsgInfo());
                return "experiment/create";
            }else{
                redirectAttributes.addAttribute(SUCCESS_MSG, SuccessMsg.CREATE_EXPERIMENT_AND_INDEX_SUCCESS);
                return "redirect:/experiment/list";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "experiment/list";
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
        if (resultDO.isSuccess()) {
            model.addAttribute("experiment", resultDO.getModel());
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
        if(resultDO.isFailured()){
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
