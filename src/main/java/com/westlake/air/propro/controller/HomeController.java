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
        LibraryQuery libraryQuery = new LibraryQuery(1, SHOW_NUM, Sort.Direction.DESC, "createDate");
        ResultDO<List<LibraryDO>> libRes = libraryService.getList(libraryQuery);

        ExperimentQuery experimentQuery = new ExperimentQuery(1, SHOW_NUM, Sort.Direction.DESC, "createDate");
        experimentQuery.setType(Constants.EXP_TYPE_DIA_SWATH);
        ResultDO<List<ExperimentDO>> expSWATH = experimentService.getList(experimentQuery);
        experimentQuery.setType(Constants.EXP_TYPE_PRM);
        ResultDO<List<ExperimentDO>> expPRM = experimentService.getList(experimentQuery);

        AnalyseOverviewQuery overviewQuery = new AnalyseOverviewQuery(1, SHOW_NUM, Sort.Direction.DESC, "createDate");
        ResultDO<List<AnalyseOverviewDO>> overviewRes = analyseOverviewService.getList(overviewQuery);

        ProjectQuery projectQuery = new ProjectQuery(1, SHOW_NUM, Sort.Direction.DESC, "createDate");
        ResultDO<List<ProjectDO>> projectsRes = projectService.getList(projectQuery);

        TaskQuery query = new TaskQuery(1, SHOW_NUM, Sort.Direction.DESC, "createDate");
//        ResultDO<List<TaskDO>> taskTotalRes = taskService.getList(query);
        query.setStatus(TaskStatus.RUNNING.getName());
        ResultDO<List<TaskDO>> taskRunningRes = taskService.getList(query);
        ConfigDO configDO = configDAO.getConfig();

        List<LibraryDO> slist = getLibraryList(0);
        List<LibraryDO> iRtlist = getLibraryList(1);
        model.addAttribute("libraries", slist);
        model.addAttribute("iRtLibraries", iRtlist);

        model.addAttribute("taskRunningCount", taskRunningRes.getTotalNum());
//        model.addAttribute("taskTotalCount", taskTotalRes.getTotalNum());
        model.addAttribute("libCount", libRes.getTotalNum());
        model.addAttribute("expSWATHCount", expSWATH.getTotalNum());
        model.addAttribute("expPRMCount", expPRM.getTotalNum());
        model.addAttribute("projectCount", projectsRes.getTotalNum());
        model.addAttribute("overviewCount", overviewRes.getTotalNum());
        model.addAttribute("runningTasks", taskRunningRes.getModel());
//        model.addAttribute("tasks", taskRunningRes.getModel());
        model.addAttribute("libs", libRes.getModel());
        model.addAttribute("projects", projectsRes.getModel());
        model.addAttribute("expsSWATH", expSWATH.getModel());
        model.addAttribute("expsPRM", expPRM.getModel());
        model.addAttribute("overviews", overviewRes.getModel());
        model.addAttribute("config", configDO);
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
