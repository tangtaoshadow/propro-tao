package com.westlake.air.swathplatform.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController extends BaseController{

    private Logger logger = LoggerFactory.getLogger(HomeController.class);

    @RequestMapping(value = {"/","/home"},method = RequestMethod.GET)
    String home(Model model) {
        model.addAttribute("name","陆妙善");
        return "home";
    }
}
