package com.qingqiu.openagent.model.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
public class R<T> {

    private int code;
    private String message;
    private T data;

    private R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> R<T> success(T data) {
        return new R<>(ApiCode.SUCCESS.code, ApiCode.SUCCESS.message, data);
    }

    public static <T> R<T> success() {
        return new R<>(ApiCode.SUCCESS.code, ApiCode.SUCCESS.message, null);
    }

    public static <T> R<T> success(T data, String message) {
        return new R<>(ApiCode.SUCCESS.code, message, data);
    }

    public static <T> R<T> error(ApiCode code, String message) {
        return new R<>(code.getCode(), message, null);
    }

    public static <T> R<T> error(String message) {
        return new R<>(ApiCode.ERROR.getCode(), message, null);
    }

    public static <T> R<T> error(int code, String message) {
        return new R<>(code, message, null);
    }

    @Getter
    @AllArgsConstructor
    public enum ApiCode {
        SUCCESS(200, "success"),
        ERROR(500, "error");

        private final int code;
        private final String message;

        public static ApiCode fromCode(int code) {
            for (ApiCode value : values()) {
                if (value.code == code) {
                    return value;
                }
            }
            throw new IllegalArgumentException("Invalid code: " + code);
        }
    }
}
