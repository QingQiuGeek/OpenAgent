package com.qingqiu.openagent.enums;


import lombok.Getter;

/**
 * @author: qingqiugeek
 * @date: 2026/5/3 16:00
 * @description: ChatMessage enum
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
