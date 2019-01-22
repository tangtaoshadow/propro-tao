package com.westlake.air.pecs.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-07-31 13:08
 */
@Controller
@RequestMapping("document")
public class DocumentController {

    @RequestMapping(value = "/feature")
    String delete(Model model) {
        return "document/feature";
    }
}
