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
        if (!isAdmin()) {
            username = getCurrentUsername();
        }
        if (!includePublic) {
            return libraryService.getSimpleAll(username, type, null);
        } else {
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

        // 这里根据 token 来获取用户信息 因为用户名在 token 里
        String token = request.getHeader("token");

        if (null == token) {
            return null;
        }

        // 获取真实用户名
        String username = JWTUtil.getUsername(token);
        // 获取数据库中对应的用户
        UserDO userInfo = userService.getByUsername(username);

        if (userInfo != null) {
            // 返回查询到的存在的用户
            return userInfo;
        }

        return null;
    }

    // 通过 token 获取用户名 通过用户名向服务器查询出真正存在的一条用户数据 userInfo
    public String getCurrentUsername() {
        UserDO user = getCurrentUser();
        // 当前仅当用户名存在 且不为空 才会返回用户名
        // /所以这一步就相当于已经通过 token 验证了用户名的正确性
        if (user != null && user.getUsername() != null && !user.getUsername().isEmpty()) {
            return user.getUsername();
        } else {
            // 不存在就返回 null
            return null;
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
