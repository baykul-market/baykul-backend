package by.baykulbackend.exceptions;

import lombok.Getter;

@Getter
public class CatalogConflictException extends RuntimeException {
    private final String code;

    public CatalogConflictException(String code, String message) {
        super(message);
        this.code = code;
    }
}
