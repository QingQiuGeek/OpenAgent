package com.qingqiu.openagent.enums;

import lombok.Getter;

/**
 *
 * @author
 * @date 2025-12-14
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
