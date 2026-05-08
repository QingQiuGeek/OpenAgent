package com.qingqiu.openagent.exception;

import lombok.Getter;

/**
 * @author: qingqiugeek
 * @date: 2026/5/5 14:37
 * @description: Biz exception
 */

@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = 400;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
