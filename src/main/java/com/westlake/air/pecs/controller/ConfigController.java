package com.westlake.air.pecs.controller;

import com.westlake.air.pecs.constants.SuccessMsg;
import com.westlake.air.pecs.dao.ConfigDAO;
import com.westlake.air.pecs.domain.db.ConfigDO;
import org.apache.tomcat.util.buf.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

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
        model.addAttribute("repoUrls", StringUtils.join(configDO.getRepoUrls(),','));
        return "config";
    }

    @RequestMapping(value = "/update")
    String update(Model model,
                  @RequestParam(value = "repoUrls", required = false) String repoUrls
                  ) {
        ConfigDO configDO = configDAO.getConfig();
        List<String> repoUrlList = new ArrayList<>();
        if(repoUrls != null && !repoUrls.isEmpty()){
            String[] urls = repoUrls.split(",");
            for(String url : urls){
                repoUrlList.add(url);
            }
            configDO.setRepoUrls(repoUrlList);
        }

        configDAO.updateConfig(configDO);
        model.addAttribute(SUCCESS_MSG, SuccessMsg.UPDATE_SUCCESS);
        model.addAttribute("config", configDAO.getConfig());
        model.addAttribute("repoUrls", StringUtils.join(configDO.getRepoUrls(),','));
        return "config";
    }
}
