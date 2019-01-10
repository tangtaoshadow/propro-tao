package com.westlake.air.pecs.controller;

import com.westlake.air.pecs.constants.SuccessMsg;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.ProjectDO;
import com.westlake.air.pecs.domain.query.ProjectQuery;
import com.westlake.air.pecs.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-04 10:00
 */
@Controller
@RequestMapping("project")
public class ProjectController extends BaseController {

   @Autowired
   ProjectService projectService;


    @RequestMapping(value = "/list")
    String list(Model model,
                @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                @RequestParam(value = "pageSize", required = false, defaultValue = "50") Integer pageSize,
                @RequestParam(value = "name", required = false) String name,
                @RequestParam(value = "ownerName", required = false) String ownerName) {
        model.addAttribute("name", name);
        model.addAttribute("ownerName", ownerName);
        model.addAttribute("pageSize", pageSize);
        ProjectQuery query = new ProjectQuery();
        if (name != null && !name.isEmpty()) {
            query.setName(name);
        }
        if(ownerName != null && !ownerName.isEmpty()){
            query.setOwnerName(ownerName);
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
               @RequestParam(value = "ownerName", required = false) String ownerName,
               @RequestParam(value = "description", required = false) String description,
               RedirectAttributes redirectAttributes) {

        model.addAttribute("repository", repository);
        model.addAttribute("ownerName", ownerName);
        model.addAttribute("name", name);
        model.addAttribute("description", description);

        ProjectDO projectDO = new ProjectDO();
        projectDO.setName(name);
        projectDO.setDescription(description);
        projectDO.setRepository(repository);
        projectDO.setOwnerName(ownerName);

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
            model.addAttribute("project", resultDO.getModel());
            return "/project/edit";
        }
    }

    @RequestMapping(value = "/delete/{id}")
    String delete(Model model, @PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        projectService.delete(id);
        redirectAttributes.addFlashAttribute(SUCCESS_MSG, SuccessMsg.DELETE_SUCCESS);
        return "redirect:/project/list";

    }
}
