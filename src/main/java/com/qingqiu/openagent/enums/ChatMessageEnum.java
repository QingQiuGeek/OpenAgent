package com.qingqiu.openagent.enums;


import lombok.Getter;

/**
 * @author: qing qiu
 * @date: 2024/9/12 23:08
 * @description: chatMessage枚举
 */

@Getter
public enum ChatMessageEnum {

  USER_MESSAGE("user", "用户消息"),
  SYSTEM_MESSAGE("system", "系统消息"),
  AI_MESSAGE("ai", "AI消息");
  
  /**
   * 状态码
   */
  private final String type;

  /**
   * 信息
   */
  private final String desc;

  ChatMessageEnum(String type, String desc) {
    this.type = type;
    this.desc = desc;
  }

  public String getType() {
    return type;
  }

  public String getDesc() {
    return desc;
  }

}
