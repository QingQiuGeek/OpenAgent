package com.qingqiu.openagent.interceptor;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import com.qingqiu.openagent.enums.BizExceptionEnum;
import com.qingqiu.openagent.exception.BizException;
import com.qingqiu.openagent.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @author: qingqiugeek
 * @date: 2026/5/3 08:53
 * @description: SaToken interceptor
 */
@Slf4j
@Component
public class SaTokenInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (requestAttributes == null) {
      throw new BizException(BizExceptionEnum.REQUEST_ERROR.getCode(), BizExceptionEnum.REQUEST_ERROR.getMessage());
    }
    // getServletPath() 不含 context-path，避免因 server.servlet.context-path 变化导致匹配失效
    String requestURI = request.getServletPath();
    // SSE 推流内部通过 sa-token token 解析出当前用户，避免对 EventSource 握手做拦截
    // （某些浏览器 EventSource 无法注入 Authorization 头，走独立路径更稳）。
    if (requestURI.startsWith("/sse/")) {
      try {
        StpUtil.checkLogin();
        Long userId = Long.valueOf(StpUtil.getLoginId().toString());
        UserContext.saveUser(userId);
      } catch (NotLoginException ignore) {
        // SSE 未登录直接关闭连接
        throw ignore;
      }
      return true;
    }

    StpUtil.checkLogin();
    if (requestURI.startsWith("/admin/") || requestURI.equals("/admin")) {
      StpUtil.checkRole("admin");
    }
    Long userId;
    try {
      userId = Long.valueOf(StpUtil.getLoginId().toString());
    } catch (NumberFormatException e) {
      log.warn("非法的 loginId 类型: {}", StpUtil.getLoginId());
      throw new BizException(BizExceptionEnum.NOT_LOGIN_ERROR.getCode(),
          BizExceptionEnum.NOT_LOGIN_ERROR.getMessage());
    }
    UserContext.saveUser(userId);
    return true;
  }

  // 移除用户,防止内存泄漏!!!
  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) {
    UserContext.removeUser();
  }

}
