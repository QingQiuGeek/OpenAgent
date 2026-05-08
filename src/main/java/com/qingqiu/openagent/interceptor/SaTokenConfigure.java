package com.qingqiu.openagent.interceptor;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.filter.SaTokenContextFilterForJakartaServlet;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import jakarta.annotation.Resource;
import jakarta.servlet.DispatcherType;
import java.util.EnumSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;



/**
 * @author: qingqiugeek
 * @date: 2026/5/5 17:04
 * @description: SaTokenConfigure
 */

@Slf4j
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

  @Resource
  SaTokenInterceptor saTokenInterceptor;

  /**
   * 注册 Sa-Token 拦截器打开注解鉴权功能
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    // 注册 Sa-Token 拦截器打开注解鉴权功能
    log.info("注册自定义拦截器");
    registry.addInterceptor(saTokenInterceptor)
        .addPathPatterns("/**")
        .excludePathPatterns(
            "/user/login",
            "/user/register",
            "/user/register-code",
            "/doc.html",
            "/doc.html/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/webjars/**",
            "/favicon.ico",
            "/error"
        );


//    registry.addInterceptor(new SaInterceptor(handle -> {
//
//      //定义详细的路由匹配校验规则
//      SaRouter.notMatch("/tag/getRandomTags"
//          ,"/category/getCategories"
//          ,"comment/getCommentByCursor");
//
//      SaRouter.match("/ai/**",r->StpUtil.checkLogin());
//
//      // 指定一条 match 规则
//      SaRouter.match("/user/**")    // 拦截的 path 列表，可以写多个
//          .notMatch("/user/login",
//              "/user/register",
//              "/user/getUserInfo/{uid}",
//              "/user/sendRegisterCode",
//              "/user/find/{userName}",
//              "/user/userInfoData")
//          .check(r->StpUtil.checkLogin());
//
//      SaRouter.match("/passage/**")
//          .notMatch("/passage/otherPassages/{uid}",
//              "/passage/topCollects",
//              "/passage/content/{uid}/{pid}",
//              "/passage/homePassageList",
//              "/passage/search",
//              "/passage/passageInfo/{pid}",
//              "/passage/topPassages")
//          .check(r->StpUtil.checkLogin());
//
//      // 角色校验
//      SaRouter.match("/admin/**", r -> StpUtil.checkRole(UserRoleEnum.ADMIN.getRole()));
//
//    })).addPathPatterns("/**").order(0);

  }

  /**
   * 注册 [Sa-Token 全局过滤器]
   */
//  @Bean
  public SaServletFilter getSaServletFilter() {
    return new SaServletFilter()

        // 指定 [拦截路由] 与 [放行路由]
        .addInclude("/**")
        // 认证函数: 每次请求执行
        .setAuth(obj -> {
           SaManager.getLog().info("----- 请求path={},authorization={}", SaHolder.getRequest().getRequestPath(), StpUtil.getTokenValue());
          // 权限校验 -- 不同模块认证不同权限
          //		这里你可以写和拦截器鉴权同样的代码，不同点在于：
          // 		校验失败后不会进入全局异常组件，而是进入下面的 .setError 函数
//          SaRouter.match("/admin/**", r -> StpUtil.checkPermission("admin"));
        })

        // 异常处理函数：每次认证函数发生异常时执行此函数
        .setError(e -> {
          log.warn("---------- sa-token全局异常 ");
          return SaResult.error(e.getMessage());
        })

        // 前置函数：在每次认证函数之前执行（BeforeAuth 不受 includeList 与 excludeList 的限制，所有请求都会进入）
        .setBeforeAuth(r -> {
          // ---------- 设置一些安全响应头 ----------
          SaHolder.getResponse()
              // 是否可以在iframe显示视图： DENY=不可以 | SAMEORIGIN=同域下可以 | ALLOW-FROM uri=指定域名下可以
              .setHeader("X-Frame-Options", "SAMEORIGIN")
              // 是否启用浏览器默认XSS防护： 0=禁用 | 1=启用 | 1; mode=block 启用, 并在检查到XSS攻击时，停止渲染页面
              .setHeader("X-XSS-Protection", "1; mode=block")
              // 禁用浏览器内容嗅探
              .setHeader("X-Content-Type-Options", "nosniff")

//              .setHeader("Connection", "keep-alive")
//              .setHeader("Cache-Control", "no-cache")
//              .setHeader("Content-Encoding", "identity");
          ;
        })
        ;
  }

  /**
   * 解决SaTokenContext 上下文尚未初始化的问题
   * 参考: https://gitee.com/dromara/sa-token/issues/IC4XFE
   * @return
   */
  @Bean
  public FilterRegistrationBean saTokenContextFilterForJakartaServlet() {
    FilterRegistrationBean bean = new FilterRegistrationBean<>(new SaTokenContextFilterForJakartaServlet());
    // 配置 Filter 拦截的 URL 模式
    bean.addUrlPatterns("/*");
    // 设置 Filter 的执行顺序,数值越小越先执行
    bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    bean.setAsyncSupported(true);
    bean.setDispatcherTypes(EnumSet.of(DispatcherType.ASYNC, DispatcherType.REQUEST));
    return bean;
  }

}
