package com.westlake.air.propro.controller;

import com.westlake.air.propro.async.task.ExperimentTask;
import com.westlake.air.propro.domain.db.LibraryDO;
import com.westlake.air.propro.domain.db.UserDO;
import com.westlake.air.propro.domain.query.PageQuery;
import com.westlake.air.propro.exception.UserNotLoginException;
import com.westlake.air.propro.service.ExperimentService;
import com.westlake.air.propro.service.LibraryService;
import com.westlake.air.propro.service.TaskService;
import com.westlake.air.propro.async.task.LibraryTask;
import com.westlake.air.propro.service.UserService;
import com.westlake.air.propro.utils.JWTUtil;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.HttpRequestHandlerServlet;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Created by James Lu MiaoShan
 * Time: 2018-05-31 09:53
 */
public class BaseController {

    public final String redirectToLoginPage = "redirect:/login/login";

    @Autowired
    LibraryService libraryService;
    @Autowired
    ExperimentService experimentService;
    @Autowired
    TaskService taskService;
    @Autowired
    LibraryTask libraryTask;
    @Autowired
    ExperimentTask experimentTask;
    @Autowired
    HttpServletRequest request;
    @Autowired
    UserService userService;

    public final Logger logger = LoggerFactory.getLogger(getClass());
    public static String ERROR_MSG = "error_msg";
    public static String SUCCESS_MSG = "success_msg";

    //0:标准库,1:irt校准库
    public List<LibraryDO> getLibraryList(Integer type, boolean includePublic) {
        String username = null;
        //如果是管理员的话不要设置指定的用户名
        if(!isAdmin()){
            username = getCurrentUsername();
        }
        if(!includePublic){
            return libraryService.getSimpleAll(username, type, null);
        }else{
            List<LibraryDO> libraries = libraryService.getSimpleAll(username, type, false);
            List<LibraryDO> publicLibraries = libraryService.getAllPublic(type);
            libraries.addAll(publicLibraries);
            return libraries;
        }
    }

    public void buildPageQuery(PageQuery query, Integer currentPage, Integer pageSize) {
        if (currentPage != null) {
            query.setPageNo(currentPage);
        }
        if (pageSize != null) {
            query.setPageSize(pageSize);
        }
    }

    public UserDO getCurrentUser() {

        // Object object = SecurityUtils.getSubject().getPrincipal();
        String token = request.getHeader("token");
        System.out.println(token);

        if(null==token){
            return null;
        }

        // 获取真实用户名
        String username = JWTUtil.getUsername(token);
        System.out.println("username="+username);
        // 获取数据库中对应的用户
        UserDO userInfo = userService.getByUsername(username);
        System.out.println(userInfo);

        if (userInfo != null) {
            return userInfo;
        }

        return null;
    }

    public String getCurrentUsername() {
        UserDO user = getCurrentUser();
        if (user != null && user.getUsername() != null && !user.getUsername().isEmpty()) {
            return user.getUsername();
        } else {
            throw new UserNotLoginException();
        }
    }

    public boolean isAdmin() {
        UserDO user = getCurrentUser();
        if (user != null && user.getRoles() != null && user.getRoles().contains("admin")) {
            return true;
        } else {
            return false;
        }
    }
}
