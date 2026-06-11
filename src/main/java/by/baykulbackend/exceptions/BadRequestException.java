package by.baykulbackend.exceptions;

import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {
    private final String errorCode;

    public BadRequestException(String msg) {
        super(msg);
        this.errorCode = null;
    }

    public BadRequestException(String msg, String errorCode) {
        super(msg);
        this.errorCode = errorCode;
    }
}
