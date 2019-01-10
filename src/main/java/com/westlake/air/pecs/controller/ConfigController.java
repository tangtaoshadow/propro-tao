package com.westlake.air.pecs.controller;

import com.westlake.air.pecs.constants.SuccessMsg;
import com.westlake.air.pecs.dao.ConfigDAO;
import com.westlake.air.pecs.domain.db.ConfigDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-08-14 16:02
 */
@Controller
@RequestMapping("/config")
public class ConfigController extends BaseController {

    @Autowired
    ConfigDAO configDAO;

    @RequestMapping(value = "/config")
    String config(Model model) {
        ConfigDO configDO = configDAO.getConfig();
        model.addAttribute("config", configDO);
        return "config";
    }

    @RequestMapping(value = "/update")
    String update(Model model,
                  @RequestParam(value = "repository", required = false) String repository
                  ) {
        ConfigDO configDO = configDAO.getConfig();

        configDAO.updateConfig(configDO);
        model.addAttribute(SUCCESS_MSG, SuccessMsg.UPDATE_SUCCESS);
        model.addAttribute("config", configDAO.getConfig());
        return "config";
    }
}
