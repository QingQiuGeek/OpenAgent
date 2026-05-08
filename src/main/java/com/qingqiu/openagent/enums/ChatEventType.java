package com.qingqiu.openagent.enums;

import lombok.Getter;

/**
 * @author: qingqiugeek
 * @date: 2026/5/3 08:36
 * @description: ChatEventType
 */
@Getter
public enum ChatEventType {

    REASONING("reasoning" ),
    EXECUTING("executing"),
    DONE("done"),
    CONTENT("content"),
    THINKING("thinking"),
    ERROR("error");

    private final String chatEventType;

    ChatEventType( String chatEventType) {
        this.chatEventType = chatEventType;
    }

}
