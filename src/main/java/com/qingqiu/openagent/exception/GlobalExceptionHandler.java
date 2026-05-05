package com.qingqiu.openagent.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import com.qingqiu.openagent.enums.BizExceptionEnum;
import com.qingqiu.openagent.model.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    /**
     * 捕获业务异常，保留业务 code 透传给前端。
     */
    @ExceptionHandler(BizException.class)
    public R<Void> handleBizException(BizException e) {
        return R.error(e.getCode(), e.getMessage());
    }

    /**
     * sa-token 未登录异常（token 缺失/失效/被踢）。统一以 40200 返回，前端据此跳登录。
     */
    @ExceptionHandler(NotLoginException.class)
    public R<Void> handleNotLoginException(NotLoginException e) {
        return R.error(BizExceptionEnum.NOT_LOGIN_ERROR.getCode(),
                BizExceptionEnum.NOT_LOGIN_ERROR.getMessage());
    }

    /**
     * sa-token 无权限异常。
     */
    @ExceptionHandler(NotRoleException.class)
    public R<Void> handleNotRoleException(NotRoleException e) {
        return R.error(BizExceptionEnum.FORBIDDEN_ERROR.getCode(),
                BizExceptionEnum.FORBIDDEN_ERROR.getMessage());
    }

    /**
     * 处理 404 错误
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handle404(NoResourceFoundException e) {
        return ResponseEntity.notFound().build();
    }

    /**
     * 捕获所有未处理的异常, 对前端不返回错误信息
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("服务器内部错误", e);
        return R.error("服务器内部错误");
    }
}
