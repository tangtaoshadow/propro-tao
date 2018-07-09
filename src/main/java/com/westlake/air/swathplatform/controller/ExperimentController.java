package com.westlake.air.swathplatform.controller;

import com.westlake.air.swathplatform.constants.SuccessMsg;
import com.westlake.air.swathplatform.domain.ResultDO;
import com.westlake.air.swathplatform.domain.db.ExperimentDO;
import com.westlake.air.swathplatform.domain.db.LibraryDO;
import com.westlake.air.swathplatform.domain.db.TransitionDO;
import com.westlake.air.swathplatform.domain.query.ExperimentQuery;
import com.westlake.air.swathplatform.parser.MzXmlParser;
import com.westlake.air.swathplatform.parser.indexer.Indexer;
import com.westlake.air.swathplatform.parser.model.mzxml.ScanIndex;
import com.westlake.air.swathplatform.service.ExperimentService;
import com.westlake.air.swathplatform.service.LibraryService;
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
    Indexer lmsIndexer;

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
               @RequestParam(value = "libraryId", required = true) String libraryId,
               RedirectAttributes redirectAttributes) {

        File file = new File("H:\\data\\weissto_i170508_005-SWLYPB125.mzXML");
//        File file = new File(getClass().getClassLoader().getResource("data/MzXMLFile_1_compressed.mzXML").getPath());
//        File file = new File("D:\\data\\wlym5.mzXML");
//        File file = new File("D:\\testdata\\testfile.mzXML");

        try {
            List<TransitionDO> simpleList = transitionService.getSimpleAllByLibraryId(libraryId);
            System.out.println(simpleList.size());
            //建立索引
            List<ScanIndex> indexList = lmsIndexer.index(file);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "experiment/list";
    }

    @RequestMapping(value = "/edit/{id}")
    String edit(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {
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
                  RedirectAttributes redirectAttributes) {


        return "redirect:/experiment/list";

    }

    @RequestMapping(value = "/delete/{id}")
    String delete(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        ResultDO resultDO = experimentService.delete(id);

        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.DELETE_LIBRARY_SUCCESS);
        return "redirect:/experiment/list";

    }
}
