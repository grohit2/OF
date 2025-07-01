package com.hT.historicalload.models;

import io.soabase.recordbuilder.core.RecordBuilder;
import java.util.Optional;

@RecordBuilder
public record TransactionStoreProcessingContext(
    Transaction transaction,
    TransactionStoreProcessingStatus processingStatus,
    Optional<String> errorMessage,
    Optional<Transaction> previousTransactionStoreRequest) {

    public TransactionStoreProcessingContext(Transaction transaction) {
        this(transaction, TransactionStoreProcessingStatus.NOT_SET, Optional.empty(), Optional.empty());
    }
}