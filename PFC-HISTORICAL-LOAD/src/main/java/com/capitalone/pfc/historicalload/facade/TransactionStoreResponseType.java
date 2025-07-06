package com.roh.pfc.historicalload.facade;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum TransactionStoreResponseType {
    ALL_ITEMS_PROCESSED_SUCCESSFULLY(HttpStatus.OK),
    ALL_ITEMS_FAILED_VALIDATION(HttpStatus.BAD_REQUEST),
    ALL_ITEMS_FAILED_CONDITIONAL_CHECK(HttpStatus.CONFLICT),
    ALL_ITEMS_FAILED_PUT_ITEM(HttpStatus.INTERNAL_SERVER_ERROR),
    PARTIAL_ITEMS_PROCESSED_SUCCESSFULLY(HttpStatus.MULTI_STATUS),
    UNKNOWN_ERRORS(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    TransactionStoreResponseType(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }
}