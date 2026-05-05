package com.qingqiu.openagent.enums;


import lombok.Getter;

/**
 * @author: qing qiu
 * @date: 2024/9/12 23:08
 * @description: 自定义错误码
 */
@Getter
public enum BizExceptionEnum {

  SUCCESS(0, "ok"),
  PARAMS_ERROR(40000, "请求参数错误"),
  NO_AUTH_ERROR(40100, "无权限"),
  NOT_LOGIN_ERROR(40200, "未登录"),
  LOGIN_PARAMS_ERROR(40300, "账号或密码错误"),
  CAPTCHA_ERROR(40400, "验证码错误"),
  FORBIDDEN_ERROR(40500, "禁止访问"),
  NOT_REGISTER_ERROR(40600, "该账号未注册"),
  NOT_FOUND_ERROR(40700, "数据不存在"),
  CAPTCHA_EXPIRE_ERROR(40800, "验证码过期"),
  ACCOUNT_EXISTED_ERROR(40900, "该账号已注册"),
  REGISTER_ERROR(40900, "注册失败"),
  SYSTEM_ERROR(50000, "系统内部异常"),
  OPERATION_ERROR(50001, "操作失败"),
  REQUEST_ERROR(50002, "请求异常"),
  IMAGE_TYPE_ERROR(50003, "图片格式有误"),
  SYSTEM_BUSY(50005, "系统繁忙"),
  FILE_UPLOAD_ERROR(50004, "文件上传失败");
  /**
   * 状态码
   */
  private final int code;

  /**
   * 信息
   */
  private final String message;

  BizExceptionEnum(int code, String message) {
    this.code = code;
    this.message = message;
  }

}
