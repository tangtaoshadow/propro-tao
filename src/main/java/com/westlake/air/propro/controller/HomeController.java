package com.westlake.air.propro.controller;

import com.westlake.air.propro.constants.Constants;
import com.westlake.air.propro.constants.TaskStatus;
import com.westlake.air.propro.dao.ConfigDAO;
import com.westlake.air.propro.domain.ResultDO;
import com.westlake.air.propro.domain.db.*;
import com.westlake.air.propro.domain.query.*;
import com.westlake.air.propro.service.*;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-05-31 09:53
 */
@Controller
@RequestMapping("/")
public class HomeController extends BaseController {

    private Logger logger = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    LibraryService libraryService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    AnalyseOverviewService analyseOverviewService;
    @Autowired
    TaskService taskService;
    @Autowired
    ProjectService projectService;
    @Autowired
    ConfigDAO configDAO;
    @Autowired
    UserService userService;

    public static int SHOW_NUM = 5;

    @RequestMapping("/")
    String home(Model model) {
        ResultDO<List<LibraryDO>> libRes = libraryService.getList(new LibraryQuery());
        String username = getCurrentUsername();

        ExperimentQuery experimentQuery = new ExperimentQuery();
        experimentQuery.setOwnerName(username);
        experimentQuery.setType(Constants.EXP_TYPE_DIA_SWATH);
        long expSWATHCount = experimentService.count(experimentQuery);
        experimentQuery.setType(Constants.EXP_TYPE_PRM);
        long expPRMCount = experimentService.count(experimentQuery);

        AnalyseOverviewQuery analyseOverviewQuery = new AnalyseOverviewQuery();

        long overviewCount = analyseOverviewService.count(analyseOverviewQuery);

        ProjectQuery projectQuery = new ProjectQuery();
        long projectCount = projectService.count(projectQuery);

        TaskQuery query = new TaskQuery();
        query.setStatus(TaskStatus.RUNNING.getName());
        long taskRunningCount = taskService.count(query);

        LibraryQuery libraryQuery = new LibraryQuery(0);
        libraryQuery.setCreator(username);
        long libCount = libraryService.count(libraryQuery);
        libraryQuery.setType(1);
        long iRtLibCount = libraryService.count(libraryQuery);

        libraryQuery = new LibraryQuery(0);
        libraryQuery.setDoPublic(true);
        long publicLibCount = libraryService.count(libraryQuery);
        libraryQuery.setType(1);
        long publicIrtCount = libraryService.count(libraryQuery);

        model.addAttribute("taskRunningCount", taskRunningCount);

        model.addAttribute("libCount", libCount);
        model.addAttribute("libIrtCount", iRtLibCount);
        model.addAttribute("publicLibCount", publicLibCount);
        model.addAttribute("publicIrtCount", publicIrtCount);

        model.addAttribute("expSWATHCount", expSWATHCount);
        model.addAttribute("expPRMCount", expPRMCount);

        model.addAttribute("projectCount", projectCount);

        model.addAttribute("overviewCount", overviewCount);

        return "home";
    }

    /**
     * 需要有Admin权限才可以执行注册功能
     *
     * @param model
     * @return
     */
    @RequiresRoles({"admin"})
    @RequestMapping("/register")
    String register(Model model, UserDO user){
        return "home";
    }

    @RequiresRoles({"admin"})
    @RequestMapping("/init")
    String init(Model model) {
        logger.info("Register");
        UserDO userDO = new UserDO();
        userDO.setUsername("Admin");
        userDO.setEmail("lumiaoshan@westlake.edu.cn");
        userDO.setNick("propro");
        String randomSalt = new SecureRandomNumberGenerator().nextBytes().toHex();
        String result = new Md5Hash("propro", randomSalt, 3).toString();
        userDO.setSalt(randomSalt);
        userDO.setPassword(result);
        Set<String> roles = new HashSet<>();
        roles.add("admin");
        userDO.setRoles(roles);
        userDO.setTelephone("13185022599");
        userDO.setOrganization("Westlake University");
        userService.register(userDO);

        return "home";
    }

}
