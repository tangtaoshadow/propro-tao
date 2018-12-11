package com.westlake.air.pecs.controller;

import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.constants.SuccessMsg;
import com.westlake.air.pecs.constants.TaskTemplate;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.LibraryDO;
import com.westlake.air.pecs.domain.db.PeptideDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.domain.query.LibraryQuery;
import com.westlake.air.pecs.parser.TraMLParser;
import com.westlake.air.pecs.parser.LibraryTsvParser;
import com.westlake.air.pecs.service.LibraryService;
import com.westlake.air.pecs.service.PeptideService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-05-31 09:53
 */
@Controller
@RequestMapping("library")
public class LibraryController extends BaseController {

    @Autowired
    LibraryTsvParser tsvParser;
    @Autowired
    TraMLParser traMLParser;
    @Autowired
    LibraryService libraryService;
    @Autowired
    PeptideService peptideService;

    @RequestMapping(value = "/listStandard")
    String listStandard(Model model,
                        @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                        @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize,
                        @RequestParam(value = "searchName", required = false) String searchName) {
        model.addAttribute("searchName", searchName);
        model.addAttribute("pageSize", pageSize);
        LibraryQuery query = new LibraryQuery();
        if (searchName != null && !searchName.isEmpty()) {
            query.setName(searchName);
        }
        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        query.setType(0);
        ResultDO<List<LibraryDO>> resultDO = libraryService.getList(query);

        model.addAttribute("libraryList", resultDO.getModel());
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);
        return "library/list";
    }

    @RequestMapping(value = "/listIrt")
    String listIrt(Model model,
                   @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                   @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize,
                   @RequestParam(value = "searchName", required = false) String searchName) {
        model.addAttribute("searchName", searchName);
        model.addAttribute("pageSize", pageSize);
        LibraryQuery query = new LibraryQuery();
        if (searchName != null && !searchName.isEmpty()) {
            query.setName(searchName);
        }
        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        query.setType(1);
        ResultDO<List<LibraryDO>> resultDO = libraryService.getList(query);

        model.addAttribute("libraryList", resultDO.getModel());
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);
        return "library/list";
    }

    @RequestMapping(value = "/create")
    String create(Model model) {
        return "library/create";
    }

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    String add(Model model,
               @RequestParam(value = "name", required = true) String name,
               @RequestParam(value = "instrument", required = false) String instrument,
               @RequestParam(value = "type", required = true) Integer type,
               @RequestParam(value = "description", required = false) String description,
               @RequestParam(value = "file") MultipartFile file,
               RedirectAttributes redirectAttributes) {

        if (file == null || file.getOriginalFilename() == null || file.getOriginalFilename().isEmpty()) {
            model.addAttribute(ERROR_MSG, ResultCode.FILE_NOT_EXISTED);
            return "library/create";
        }

        LibraryDO library = new LibraryDO();
        library.setName(name);
        library.setInstrument(instrument);
        library.setDescription(description);
        library.setType(type);
        ResultDO resultDO = libraryService.insert(library);
        if (resultDO.isFailed()) {
            logger.warn(resultDO.getMsgInfo());
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            redirectAttributes.addFlashAttribute("library", library);
            return "redirect:/library/create";
        }

        TaskDO taskDO = new TaskDO(TaskTemplate.UPLOAD_LIBRARY_FILE, library.getName());
        taskService.insert(taskDO);

        try {
            libraryTask.saveLibraryTask(library, file.getInputStream(), file.getOriginalFilename(), taskDO);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "redirect:/task/detail/" + taskDO.getId();
    }

    @RequestMapping(value = "/aggregate/{id}")
    String aggregate(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {

        ResultDO<LibraryDO> resultDO = libraryService.getById(id);
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/library/list";
        }

        LibraryDO library = resultDO.getModel();
        libraryService.countAndUpdateForLibrary(library);

        return "redirect:/library/detail/" + library.getId();
    }

    @RequestMapping(value = "/edit/{id}")
    String edit(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        ResultDO<LibraryDO> resultDO = libraryService.getById(id);
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/library/list";
        } else {
            model.addAttribute("library", resultDO.getModel());
            return "/library/edit";
        }
    }

    @RequestMapping(value = "/detail/{id}")
    String detail(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        ResultDO<LibraryDO> resultDO = libraryService.getById(id);

        if (resultDO.isSuccess()) {
            if (resultDO.getModel().getType().equals(LibraryDO.TYPE_IRT)) {
                Double[] range = peptideService.getRTRange(id);
                if (range != null && range.length == 2) {
                    model.addAttribute("minRt", range[0]);
                    model.addAttribute("maxRt", range[1]);
                }
            }
            model.addAttribute("library", resultDO.getModel());
            return "/library/detail";
        } else {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return "redirect:/library/listStandard";
        }
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    String update(Model model,
                  @RequestParam(value = "id", required = true) String id,
                  @RequestParam(value = "name") String name,
                  @RequestParam(value = "instrument") String instrument,
                  @RequestParam(value = "type") Integer type,
                  @RequestParam(value = "description") String description,
                  @RequestParam(value = "justReal", required = false) boolean justReal,
                  @RequestParam(value = "file") MultipartFile file,
                  RedirectAttributes redirectAttributes) {

        String redirectListUrl = null;
        if (type == 1) {
            redirectListUrl = "redirect:/library/listIrt";
        } else {
            redirectListUrl = "redirect:/library/listStandard";
        }

        ResultDO<LibraryDO> resultDO = libraryService.getById(id);
        if (resultDO.isFailed()) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return redirectListUrl;
        }

        LibraryDO library = resultDO.getModel();
        library.setDescription(description);
        library.setInstrument(instrument);
        library.setType(type);
        ResultDO updateResult = libraryService.update(library);
        if (updateResult.isFailed()) {
            redirectAttributes.addFlashAttribute(ResultCode.UPDATE_ERROR.getMessage(), updateResult.getMsgInfo());
            return redirectListUrl;
        }

        //如果没有更新源文件,那么直接返回标准库详情页面
        if (file == null || file.getOriginalFilename() == null || file.getOriginalFilename().isEmpty()) {
            return "redirect:/library/detail/" + library.getId();
        }

        TaskDO taskDO = new TaskDO(TaskTemplate.UPLOAD_LIBRARY_FILE, library.getName());
        taskService.insert(taskDO);

        try {
            libraryTask.saveLibraryTask(library, file.getInputStream(), file.getOriginalFilename(), taskDO);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "redirect:/task/detail/" + taskDO.getId();
    }

    @RequestMapping(value = "/delete/{id}")
    String delete(Model model, @PathVariable("id") String id,
                  @RequestParam(value = "type", required = false) Integer type,
                  RedirectAttributes redirectAttributes) {
        ResultDO resultDO = libraryService.delete(id);

        String redirectListUrl = null;
        if (type == 1) {
            redirectListUrl = "redirect:/library/listIrt";
        } else {
            redirectListUrl = "redirect:/library/listStandard";
        }
        peptideService.deleteAllByLibraryId(id);
        if (resultDO.isSuccess()) {
            redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.DELETE_LIBRARY_SUCCESS);
            return redirectListUrl;
        } else {
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            return redirectListUrl;
        }
    }

    @RequestMapping(value = "overview/{id}")
    @ResponseBody
    String overview(Model model, @PathVariable("id") String id) {
        List<PeptideDO> peptides = peptideService.getAllByLibraryId(id);
        int count = 0;
        for (PeptideDO peptide : peptides) {
            if (peptide.getFragmentMap().size() <= 3) {
                count++;
            }
        }
        return count+"个不符合要求的离子";
    }
}
