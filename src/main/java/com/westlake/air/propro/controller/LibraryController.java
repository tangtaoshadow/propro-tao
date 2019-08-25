package com.westlake.air.propro.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.westlake.air.propro.algorithm.parser.LibraryTsvParser;
import com.westlake.air.propro.algorithm.parser.TraMLParser;
import com.westlake.air.propro.constants.Constants;
import com.westlake.air.propro.constants.enums.ResultCode;
import com.westlake.air.propro.constants.SuccessMsg;
import com.westlake.air.propro.constants.enums.TaskTemplate;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.bean.aird.WindowRange;
import com.westlake.air.propro.domain.db.ExperimentDO;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.PeptideDO;
import com.westlake.air.propro.domain.db.TaskDO;
import com.westlake.air.propro.domain.query.LibraryQuery;
import com.westlake.air.propro.domain.query.PeptideQuery;
import com.westlake.air.propro.service.LibraryService;
import com.westlake.air.propro.service.PeptideService;
import com.westlake.air.propro.utils.PermissionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @RequestMapping(value = "/list")
    String list(Model model,
                @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize,
                @RequestParam(value = "searchName", required = false) String searchName) {
        model.addAttribute("searchName", searchName);
        model.addAttribute("pageSize", pageSize);
        LibraryQuery query = new LibraryQuery();
        if (searchName != null && !searchName.isEmpty()) {
            query.setName(searchName);
        }

        if (!isAdmin()) {
            query.setCreator(getCurrentUsername());
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

        if (!isAdmin()) {
            query.setCreator(getCurrentUsername());
        }
        if (!isAdmin()) {
            query.setCreator(getCurrentUsername());
        }
        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        query.setType(1);
        ResultDO<List<LibraryDO>> resultDO = libraryService.getList(query);

        model.addAttribute("libraryList", resultDO.getModel());
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);
        return "library/listIrt";
    }


    // 公共标准库
    @RequestMapping(value = "/listPublic")
    @ResponseBody
    String listPublic(@RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                      @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize,
                      @RequestParam(value = "searchName", required = false) String searchName) {


        // 返回状态
        Map<String, Object> map = new HashMap<String, Object>();
        // 检查参数 username
        int status = -1;

        do {
            // 验证 token
            String username = getCurrentUsername();

            if (username == null) {
                // token 不正常
                status = -2;
                break;
            }

            LibraryQuery query = new LibraryQuery();
            if (searchName != null && !searchName.isEmpty()) {
                query.setName(searchName);
            }
            query.setDoPublic(true);
            query.setPageSize(pageSize);
            query.setPageNo(currentPage);
            query.setType(0);
            ResultDO<List<LibraryDO>> resultDO = libraryService.getList(query);

            map.put("libraryList", resultDO.getModel());
            map.put("totalPage", resultDO.getTotalPage());
            map.put("currentPage", currentPage);

            // 最后标记成功
            status = 0;
        } while (false);


        // 返回状态结果
        map.put("status", status);

        // 返回数据
        JSONObject json = new JSONObject(map);

        return json.toString();

    }

    @RequestMapping(value = "/listPublicIrt")
    String listPublicIrt(Model model,
                         @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                         @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize,
                         @RequestParam(value = "searchName", required = false) String searchName) {
        model.addAttribute("searchName", searchName);
        model.addAttribute("pageSize", pageSize);
        LibraryQuery query = new LibraryQuery();
        if (searchName != null && !searchName.isEmpty()) {
            query.setName(searchName);
        }
        query.setDoPublic(true);
        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        query.setType(1);
        ResultDO<List<LibraryDO>> resultDO = libraryService.getList(query);

        model.addAttribute("libraryList", resultDO.getModel());
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);
        return "library/listPublicIrt";
    }

    @RequestMapping(value = "/create")
    String create(Model model) {
        return "library/create";
    }

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    String add(Model model,
               @RequestParam(value = "name", required = true) String name,
               @RequestParam(value = "type", required = true) Integer type,
               @RequestParam(value = "description", required = false) String description,
               @RequestParam(value = "libFile", required = true) MultipartFile libFile,
               @RequestParam(value = "prmFile", required = false) MultipartFile prmFile,
               RedirectAttributes redirectAttributes) {

        if (libFile == null || libFile.getOriginalFilename() == null || libFile.getOriginalFilename().isEmpty()) {
            model.addAttribute(ERROR_MSG, ResultCode.FILE_NOT_EXISTED);
            return "library/create";
        }
        LibraryDO library = new LibraryDO();
        library.setName(name);
        library.setDescription(description);
        library.setType(type);
        library.setCreator(getCurrentUsername());
        ResultDO resultDO = libraryService.insert(library);
        TaskDO taskDO = new TaskDO(TaskTemplate.UPLOAD_LIBRARY_FILE, library.getName());
        taskService.insert(taskDO);
        if (resultDO.isFailed()) {
            logger.warn(resultDO.getMsgInfo());
            redirectAttributes.addFlashAttribute(ERROR_MSG, resultDO.getMsgInfo());
            redirectAttributes.addFlashAttribute("library", library);
            return "redirect://library/create";
        }
        try {
            InputStream libFileStream = libFile.getInputStream();
            InputStream prmFileStream = null;
            if (!prmFile.isEmpty()) {
                prmFileStream = prmFile.getInputStream();
            }

            libraryTask.saveLibraryTask(library, libFileStream, libFile.getOriginalFilename(), prmFileStream, taskDO);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "redirect:/task/detail/" + taskDO.getId();
    }

    @RequestMapping(value = "/aggregate/{id}")
    String aggregate(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {

        LibraryDO library = libraryService.getById(id);
        PermissionUtil.check(library);
        if (library == null) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.LIBRARY_NOT_EXISTED.getMessage());
            return "redirect:/library/list";
        }

        libraryService.countAndUpdateForLibrary(library);

        return "redirect:/library/detail/" + library.getId();
    }

    @RequestMapping(value = "/edit/{id}")
    String edit(Model model, @PathVariable("id") String id,
                RedirectAttributes redirectAttributes) {

        LibraryDO library = libraryService.getById(id);
        if (library == null) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.LIBRARY_NOT_EXISTED.getMessage());
            return "redirect:/library/list";
        } else {
            PermissionUtil.check(library);
            model.addAttribute("library", library);
            return "library/edit";
        }
    }

    @RequestMapping(value = "/detail11/{id}")
    String detail(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        LibraryDO library = libraryService.getById(id);

        if (library == null) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.LIBRARY_NOT_EXISTED.getMessage());
            return "redirect:/library/list";
        } else {
            PermissionUtil.check(library);
            if (library.getType().equals(LibraryDO.TYPE_IRT)) {
                Double[] range = peptideService.getRTRange(id);
                if (range != null && range.length == 2) {
                    model.addAttribute("minRt", range[0]);
                    model.addAttribute("maxRt", range[1]);
                }
            }
            model.addAttribute("library", library);
            return "library/detail";
        }
    }


    @RequestMapping(value = "/detail", method = RequestMethod.POST)
    @ResponseBody
    String libraryListIdDetail(@RequestParam(value = "id") String id
    ) {
        System.out.println("===libraryListIdDetail==========");

        // 返回状态
        Map<String, Object> map = new HashMap<String, Object>();

        // 状态标记
        int status = -1;

        System.out.println(id);
        LibraryDO library = libraryService.getById(id);
        do {

            // // 验证 token
            // String username = getCurrentUsername();
            //
            // if (username == null) {
            //     // token 不正常
            //     status = -2;
            //     break;
            // } else {
            //     // pass
            // }
            System.out.println(library);
            System.out.println(library == null);

            // if (library == null) {
            //     // 查询库为空  严重错误
            //     status = -3;
            //     break;
            // } else

            {

                // ***  临时注释  ***
                PermissionUtil.check(library);

                // IRT 库
                if (library.getType().equals(LibraryDO.TYPE_IRT)) {

                    System.out.println("--==----");
                    Double[] range = peptideService.getRTRange(id);
                    System.out.println(range);
                    if (range != null && range.length == 2) {
                        map.put("minRt", range[0]);
                        map.put("maxRt", range[1]);
                    }
                }
                map.put("data", library);
                status = 0;
            }
        } while (false);


        // 返回状态结果
        map.put("status", status);

        // 返回数据
        JSONObject json = new JSONObject(map);

        System.out.println(json.toString());
        return json.toString();

    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @ResponseBody
    String update(
            @RequestParam(value = "id", required = true) String id,
            @RequestParam(value = "name") String name,
            @RequestParam(value = "libType") String type,
            @RequestParam(value = "description") String description,
            @RequestParam(value = "libFile") MultipartFile libFile,
            @RequestParam(value = "prmFile", required = false) MultipartFile prmFile
    ) {

        System.out.println("1");

        System.out.println(prmFile);
        Map<String, Object> map = new HashMap<String, Object>();

        // 状态标记
        int status = -1;

        do {

            System.out.println("2");

            LibraryDO library = libraryService.getById(id);

            if (library == null) {
                status = -2;
                break;
            }
            System.out.println("3");

            PermissionUtil.check(library);
            library.setDescription(description);
            if ("library" == type) {
                library.setType(0);
            } else {
                library.setType(1);

            }
            ResultDO updateResult = libraryService.update(library);
            if (updateResult.isFailed()) {
                // 更新失败
                status = -3;
                break;
            }

            System.out.println("4");

            // 没有更新源文件
            if (libFile == null || libFile.getOriginalFilename() == null || libFile.getOriginalFilename().isEmpty()) {
                status = -4;
                break;
            }

            TaskDO taskDO = new TaskDO(TaskTemplate.UPLOAD_LIBRARY_FILE, library.getName());
            taskService.insert(taskDO);
            System.out.println("5");

            try {
                InputStream libFileStream = libFile.getInputStream();
                InputStream prmFileStream = null;

                if (null != prmFile && !prmFile.isEmpty()) {
                    prmFileStream = prmFile.getInputStream();
                }


                libraryTask.saveLibraryTask(library, libFileStream, libFile.getOriginalFilename(), prmFileStream, taskDO);

            } catch (IOException e) {
                // 更新过程出错
                e.printStackTrace();
                status = -5;
                break;
            }
            System.out.println("6");

            // 更新成功
            status = 0;
            // 返回 taskId
            map.put("taskId", taskDO.getId());


        } while (false);

        // 返回状态结果
        map.put("status", status);

        // 返回数据
        JSONObject json = new JSONObject(map);

        return json.toString();

    }


    @RequestMapping(value = "/update11", method = RequestMethod.POST)
    String update1(Model model,
                   @RequestParam(value = "id", required = true) String id,
                   @RequestParam(value = "name") String name,
                   @RequestParam(value = "type") Integer type,
                   @RequestParam(value = "description") String description,
                   @RequestParam(value = "libFile") MultipartFile libFile,
                   @RequestParam(value = "prmFile", required = false) MultipartFile prmFile,
                   RedirectAttributes redirectAttributes) {
        String redirectListUrl = null;

        if (type == 1) {
            redirectListUrl = "redirect:/library/listIrt";
        } else {
            redirectListUrl = "redirect:/library/list";
        }

        LibraryDO library = libraryService.getById(id);
        if (library == null) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.LIBRARY_NOT_EXISTED.getMessage());
            return redirectListUrl;
        }

        PermissionUtil.check(library);


        library.setDescription(description);
        library.setType(type);
        ResultDO updateResult = libraryService.update(library);
        if (updateResult.isFailed()) {
            // 更新失败
            redirectAttributes.addFlashAttribute(ResultCode.UPDATE_ERROR.getMessage(), updateResult.getMsgInfo());
            return redirectListUrl;
        }

        //如果没有更新源文件,那么直接返回标准库详情页面
        if (libFile == null || libFile.getOriginalFilename() == null || libFile.getOriginalFilename().isEmpty()) {
            return "redirect:/library/detail/" + library.getId();
        }

        TaskDO taskDO = new TaskDO(TaskTemplate.UPLOAD_LIBRARY_FILE, library.getName());
        taskService.insert(taskDO);

        try {
            InputStream libFileStream = libFile.getInputStream();
            InputStream prmFileStream = null;
            if (!prmFile.isEmpty()) {
                prmFileStream = prmFile.getInputStream();
            }

            libraryTask.saveLibraryTask(library, libFileStream, libFile.getOriginalFilename(), prmFileStream, taskDO);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 更新成功
        return "redirect:/task/detail/" + taskDO.getId();
    }

    @RequestMapping(value = "/delete/{id}")
    String delete(Model model, @PathVariable("id") String id,
                  RedirectAttributes redirectAttributes) {
        LibraryDO library = libraryService.getById(id);
        int type = 0;
        if (library != null) {
            type = library.getType();
        }
        PermissionUtil.check(library);
        ResultDO resultDO = libraryService.delete(id);

        String redirectListUrl = null;
        if (type == 1) {
            redirectListUrl = "redirect:/library/listIrt";
        } else {
            redirectListUrl = "redirect:/library/list";
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

    @RequestMapping(value = "/setPublic/{id}")
    String setPublic(@PathVariable("id") String id,
                     RedirectAttributes redirectAttributes) {
        LibraryDO library = libraryService.getById(id);
        if (library == null) {
            redirectAttributes.addFlashAttribute(ERROR_MSG, ResultCode.LIBRARY_NOT_EXISTED.getMessage());
            return "redirect:/library/list";
        }

        PermissionUtil.check(library);

        library.setDoPublic(true);
        libraryService.update(library);

        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.SET_PUBLIC_SUCCESS);
        if (library.getType().equals(Constants.LIBRARY_TYPE_STANDARD)) {
            return "redirect:/library/list";
        } else {
            return "redirect:/library/listIrt";
        }
    }

    @RequestMapping(value = "/search")
    @ResponseBody
    ResultDO<JSONObject> search(Model model,
                                @RequestParam(value = "fragmentSequence", required = false) String fragmentSequence,
                                @RequestParam(value = "precursorMz", required = false) Float precursorMz,
                                @RequestParam(value = "experimentId", required = false) String experimentId,
                                @RequestParam(value = "libraryId", required = false) String libraryId) {
        if (fragmentSequence.length() < 3) {
            return ResultDO.buildError(ResultCode.SEARCH_FRAGMENT_LENGTH_MUST_BIGGER_THAN_3);
        }

        LibraryDO library = libraryService.getById(libraryId);
        PermissionUtil.check(library);

        ResultDO<ExperimentDO> expResult = experimentService.getById(experimentId);
        PermissionUtil.check(expResult.getModel());
        if (expResult.isFailed()) {
            return ResultDO.buildError(ResultCode.EXPERIMENT_NOT_EXISTED);
        }
        ExperimentDO exp = expResult.getModel();
        List<WindowRange> rangs = exp.getWindowRanges();
        WindowRange targetRang = null;
        for (WindowRange rang : rangs) {
            if (precursorMz >= rang.getStart() && precursorMz < rang.getEnd()) {
                targetRang = rang;
                break;
            }
        }
        PeptideQuery query = new PeptideQuery();
        query.setLibraryId(libraryId);
        query.setSequence(fragmentSequence);
        if (targetRang != null) {
            query.setMzStart(Double.parseDouble(targetRang.getStart().toString()));
            query.setMzEnd(Double.parseDouble(targetRang.getEnd().toString()));
        }

        List<PeptideDO> peptides = peptideService.getAll(query);

        JSONArray peptidesArray = new JSONArray();
        for (PeptideDO peptide : peptides) {
            peptidesArray.add(peptide.getPeptideRef());
        }
        JSONObject res = new JSONObject();
        res.put("peptides", peptidesArray);
        ResultDO<JSONObject> resultDO = new ResultDO<>(true);
        resultDO.setModel(res);
        return resultDO;
    }

    @RequestMapping(value = "overview/{id}")
    @ResponseBody
    String overview(Model model, @PathVariable("id") String id) {
        LibraryDO library = libraryService.getById(id);
        PermissionUtil.check(library);

        List<PeptideDO> peptides = peptideService.getAllByLibraryId(id);
        int count = 0;
        for (PeptideDO peptide : peptides) {
            if (peptide.getFragmentMap().size() <= 3) {
                count++;
            }
        }
        return count + "个不符合要求的离子";
    }
}
