package com.westlake.air.swathplatform.controller;

import com.westlake.air.swathplatform.algorithm.FragmentCalculator;
import com.westlake.air.swathplatform.dao.TransitionDAO;
import com.westlake.air.swathplatform.domain.bean.FragmentResult;
import com.westlake.air.swathplatform.domain.db.TransitionDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-06-12 12:19
 */
@Controller
@RequestMapping("fragment")
public class FragmentController {

    @Autowired
    FragmentCalculator fragmentCalculator;

    @Autowired
    TransitionDAO transitionDAO;

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    @ResponseBody
    FragmentResult list(Model model) {
        TransitionDO transitionDO = transitionDAO.getById("5b1f466547d23c17a07e5b98");
        return fragmentCalculator.getResult(transitionDO);
    }
}
