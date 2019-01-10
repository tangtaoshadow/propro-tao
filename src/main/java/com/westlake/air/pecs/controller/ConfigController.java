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
                  @RequestParam(value = "airdFilePath", required = false) String airdFilePath,
                  @RequestParam(value = "aircFilePath", required = false) String aircFilePath,
                  @RequestParam(value = "mzxmlFilePath", required = false) String oldExperimentFilePath,
                  @RequestParam(value = "libraryFilePath", required = false) String libraryFilePath,
                  @RequestParam(value = "exportScoresFilePath", required = false) String exportScoresFilePath,
                  @RequestParam(value = "prefixForAirdFile", required = false) String prefixForAirdFile,
                  @RequestParam(value = "prefixForAircFile", required = false) String prefixForAircFile
                  ) {
        ConfigDO configDO = configDAO.getConfig();
        if(airdFilePath != null){
            configDO.setAirdFilePath(airdFilePath);
        }
        if(libraryFilePath != null){
            configDO.setLibraryFilePath(libraryFilePath);
        }
        if(aircFilePath != null){
            configDO.setAircFilePath(aircFilePath);
        }
        if(oldExperimentFilePath != null){
            configDO.setMzxmlFilePath(oldExperimentFilePath);
        }
        if(prefixForAirdFile != null){
            configDO.setPrefixForAirdFile(prefixForAirdFile);
        }
        if(prefixForAircFile != null){
            configDO.setPrefixForAircFile(prefixForAircFile);
        }
        if(exportScoresFilePath != null){
            configDO.setExportScoresFilePath(exportScoresFilePath);
        }
        configDAO.updateConfig(configDO);
        model.addAttribute(SUCCESS_MSG, SuccessMsg.UPDATE_SUCCESS);
        model.addAttribute("config", configDAO.getConfig());
        return "config";
    }
}
