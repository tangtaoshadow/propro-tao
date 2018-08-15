package com.westlake.air.pecs.controller;

import com.westlake.air.pecs.constants.TaskTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-13 21:33
 */
@Controller
@RequestMapping("task")
public class TaskController extends BaseController{

    @RequestMapping(value = "/create")
    String create(Model model,
                  @RequestParam(value = "templateName", required = true) String templateName) {
        TaskTemplate template = TaskTemplate.getByName(templateName);
        if(template != null){
            return "redirect:"+template.getPagePath();
        }else{
            return "/home";
        }
    }

    @RequestMapping(value = "/detail/{id}")
    String detail(Model model) {
        return "task/detail";
    }

    @RequestMapping(value = "/update/{id}")
    String update(Model model) {
        return "task/detail";
    }

    @RequestMapping(value = "/delete/{id}")
    String delete(Model model) {
        return "task/detail";
    }
}
