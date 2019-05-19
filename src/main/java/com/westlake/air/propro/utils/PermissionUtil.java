package com.westlake.air.propro.utils;

import com.westlake.air.propro.constants.Roles;
import com.westlake.air.propro.domain.db.ExperimentDO;
import com.westlake.air.propro.domain.db.ProjectDO;
import com.westlake.air.propro.domain.db.UserDO;
import com.westlake.air.propro.exception.UnauthorizedAccessException;
import org.apache.shiro.SecurityUtils;

public class PermissionUtil {

    public static void check(ProjectDO project) throws UnauthorizedAccessException {
        UserDO user = getCurrentUser();
        if(project == null || user == null){
            throw new UnauthorizedAccessException("redirect:/project/list");
        }

        if(project.isDoPublic()){
            return;
        }

        if(user.getRoles().contains(Roles.ROLE_ADMIN)){
            return;
        }

        if (project.getOwnerName().equals(user.getUsername())) {
            throw new UnauthorizedAccessException("redirect:/project/list");
        } else {
            return;
        }
    }

    public static boolean check(ExperimentDO experiment) throws UnauthorizedAccessException{
        UserDO user = getCurrentUser();
        if(experiment == null || user == null){
            throw new UnauthorizedAccessException("redirect:/experiment/list");
        }

        if(user.getRoles().contains(Roles.ROLE_ADMIN)){
            return true;
        }

        if (experiment.getOwnerName().equals(user.getUsername())) {
            throw new UnauthorizedAccessException("redirect:/experiment/list");
        } else {
            return true;
        }
    }

    public static UserDO getCurrentUser() {
        Object object = SecurityUtils.getSubject().getPrincipal();
        if (object != null) {
            return (UserDO) object;
        }

        return null;
    }
}
