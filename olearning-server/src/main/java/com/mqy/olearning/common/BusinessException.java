package com.mqy.olearning.common;

import lombok.Getter;

/**
 * 业务异常：由 GlobalExceptionHandler 统一转换为 Result
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
