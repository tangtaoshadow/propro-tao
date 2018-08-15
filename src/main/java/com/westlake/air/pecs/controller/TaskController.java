package com.westlake.air.pecs.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-13 21:33
 */
@Controller
@RequestMapping("task")
public class TaskController extends BaseController{

    @RequestMapping(value = "/create")
    String create(Model model) {

        return "task/create";
    }
}
