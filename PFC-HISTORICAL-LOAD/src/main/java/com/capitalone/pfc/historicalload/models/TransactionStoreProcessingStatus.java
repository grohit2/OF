package com.roh.pfc.historicalload.models;

import lombok.Getter;

@Getter
public enum TransactionStoreProcessingStatus {
    // Initial state
    NOT_SET(""),
    // if inserted successfully in dynamodb
    SUCCESSFULLY_PUT_ITEM("success"),
    // if failed validation check
    FAILED_VALIDATION("failed_validation"),
    // if failed conditional check
    FAILED_CONDITIONAL_CHECK("failed_conditional_check"),
    // if failed to get item from dynamodb due to some error
    FAILED_GET_ITEM("failed_get_item"),
    // if failed to put item in dynamodb due to some error
    FAILED_PUT_ITEM("failed_put_item");

    final String statusCode;

    TransactionStoreProcessingStatus(String statusCode) {
        this.statusCode = statusCode;
    }
}