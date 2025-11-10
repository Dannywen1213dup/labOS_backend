package com.labOS.backend.common;

/**
 * customized errorcode
 *
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAMS_ERROR(40000, "request params error"),
    NOT_LOGIN_ERROR(40100, "user not login"),
    NO_AUTH_ERROR(40101, "no auth"),
    NOT_FOUND_ERROR(40400, "requests data not found"),
    FORBIDDEN_ERROR(40300, "unauthorized request"),
    SYSTEM_ERROR(50000, "system error"),
    OPERATION_ERROR(50001, "operation error"),;

    /**
     * state code
     */
    private final int code;

    /**
     * message
     */
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
