package com.westlake.air.pecs.controller;

import com.alibaba.fastjson.JSON;
import com.westlake.air.pecs.constants.ResultCode;
import com.westlake.air.pecs.constants.TaskTemplate;
import com.westlake.air.pecs.domain.ResultDO;
import com.westlake.air.pecs.domain.db.ExperimentDO;
import com.westlake.air.pecs.domain.db.TaskDO;
import com.westlake.air.pecs.domain.query.ExperimentQuery;
import com.westlake.air.pecs.domain.query.TaskQuery;
import com.westlake.air.pecs.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-13 21:33
 */
@Controller
@RequestMapping("task")
public class TaskController extends BaseController {

    @Autowired
    TaskService taskService;

    @RequestMapping(value = "/list")
    String list(Model model,
                @RequestParam(value = "currentPage", required = false, defaultValue = "1") Integer currentPage,
                @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {

        model.addAttribute("pageSize", pageSize);
        TaskQuery query = new TaskQuery();
        query.setPageSize(pageSize);
        query.setPageNo(currentPage);
        ResultDO<List<TaskDO>> resultDO = taskService.getList(query);

        model.addAttribute("tasks", resultDO.getModel());
        model.addAttribute("totalPage", resultDO.getTotalPage());
        model.addAttribute("currentPage", currentPage);
        return "task/list";
    }

    @RequestMapping(value = "/create")
    String create(Model model,
                  @RequestParam(value = "templateName", required = true) String templateName) {
        TaskTemplate template = TaskTemplate.getByName(templateName);
        if (template != null) {
            return "redirect:" + template.getPagePath();
        } else {
            return "/home";
        }
    }

    @RequestMapping(value = "/detail/{id}")
    String detail(Model model, @PathVariable("id") String id) {
        ResultDO<TaskDO> resultDO = taskService.getById(id);
        if (resultDO.isFailed()) {
            model.addAttribute(ERROR_MSG, ResultCode.OBJECT_NOT_EXISTED.getMessage());
            return "task/detail";
        }
        TaskDO taskDO = resultDO.getModel();
        model.addAttribute("task", taskDO);
        model.addAttribute("taskId", taskDO.getId());
        TaskTemplate taskTemplate = TaskTemplate.getByName(taskDO.getTaskTemplate());
        if (taskTemplate == null) {
            model.addAttribute(ERROR_MSG, ResultCode.OBJECT_NOT_EXISTED.getMessage());
            return "task/detail";
        }
        return "task/detail";
    }

    @RequestMapping(value = "/getTaskInfo/{id}")
    @ResponseBody
    String getTaskInfo(Model model, @PathVariable("id") String id) {
        ResultDO<TaskDO> resultDO = taskService.getById(id);
        if (resultDO.isSuccess() && resultDO.getModel() != null) {
            return JSON.toJSONString(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(resultDO.getModel().getLastModifiedDate()));
        } else {
            return null;
        }
    }

    @RequestMapping(value = "/delete/{id}")
    String delete(Model model) {
        return "task/detail";
    }
}
