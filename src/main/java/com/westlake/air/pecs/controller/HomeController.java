package com.westlake.air.pecs.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-05-31 09:53
 */
@Controller
public class HomeController {

    private Logger logger = LoggerFactory.getLogger(HomeController.class);

    @RequestMapping(value = {"/","/home"},method = RequestMethod.GET)
    String home(Model model) {
        model.addAttribute("name","陆妙善");
        return "home";
    }
}
