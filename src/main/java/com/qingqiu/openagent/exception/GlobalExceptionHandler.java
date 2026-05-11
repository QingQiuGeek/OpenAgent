package com.qingqiu.openagent.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import com.qingqiu.openagent.enums.BizExceptionEnum;
import com.qingqiu.openagent.model.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * @author: qingqiugeek
 * @date: 2026/5/7 11:36
 * @description: GlobalException handler
 */

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
     * SSE 场景：用户关页以后 Tomcat 写失败会招 Spring async 机制 dispatch 一个
     * {@link AsyncRequestNotUsableException}（原因 cause 经常是 {@code ClientAbortException}）
     * 进这里。它不是程序 bug，仅是“对端已断开”的正常副产品，以 ERROR 输出幽默默刷屏毫无意义。
     * 这里提前拦截并降为 debug。
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsable(AsyncRequestNotUsableException e) {
        log.debug("[SSE] async 连接已断开，跳过响应写出: {}", e.getMessage());
        // 不返 R：响应已不可用，再写也会报错。返回 void 让 Spring 结束该 request 处理。
    }

    /**
     * 捕获所有未处理的异常, 对前端不返回错误信息
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        // 兑底：某些组件（如 Tomcat）会在 cause 中包 ClientAbortException/IOException，
        // 这些仍是“用户主动断开”的正常剧本，不该上是 ERROR。
        if (isClientAbort(e)) {
            log.debug("[SSE] 客户端主动断开：{}", e.getMessage());
            return null;
        }
        log.error("服务器内部错误", e);
        return R.error("服务器内部错误");
    }

    /** 递归检查异常链是否含“客户端断开”语义。 */
    private static boolean isClientAbort(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            String name = c.getClass().getName();
            if (name.equals("org.apache.catalina.connector.ClientAbortException")
                    || name.equals("org.springframework.web.context.request.async.AsyncRequestNotUsableException")
                    || name.equals("org.apache.catalina.connector.CoyoteAdapter$RecycleRequiredException")) {
                return true;
            }
            if (c == c.getCause()) break; // 防环
        }
        return false;
    }
}
