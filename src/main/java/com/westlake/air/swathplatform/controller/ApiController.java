package com.westlake.air.swathplatform.controller;

import com.westlake.air.swathplatform.domain.traml.TraML;
import com.westlake.air.swathplatform.service.TraMLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-05-31 09:53
 */
@RestController
@RequestMapping("api")
public class ApiController {

    @Autowired
    TraMLService traMLService;

    @RequestMapping("traml")
    String transTraML(){
        File file = new File(ApiController.class.getClassLoader().getResource("data/MRMDecoyGenerator_input.TraML").getPath());
        TraML traML = traMLService.parse(file);
        return traML.getVersion();
    }
}
