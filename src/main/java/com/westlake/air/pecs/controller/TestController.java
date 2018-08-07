package com.westlake.air.pecs.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-07 13:30
 */
@Controller
@RequestMapping("test")
public class TestController {

    @RequestMapping("test")
    @ResponseBody
    String test(Model model, RedirectAttributes redirectAttributes) {
        return "HelloWorld";
    }
}
