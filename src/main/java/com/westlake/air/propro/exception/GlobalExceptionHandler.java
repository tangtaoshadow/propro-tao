package com.westlake.air.propro.exception;

import com.westlake.air.propro.constants.ResultCode;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotLoginException.class)
    public String userNotLoginException(UserNotLoginException e, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error_msg", ResultCode.USER_NOT_EXISTED.getMessage());
        return "redirect:/login/login";
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public String unauthorizedAccessException(UnauthorizedAccessException e, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error_msg", ResultCode.UNAUTHORIZED_ACCESS.getMessage());
        return e.getRedirectPage();
    }
}
