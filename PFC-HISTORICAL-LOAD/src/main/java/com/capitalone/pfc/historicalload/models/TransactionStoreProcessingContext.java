package com.roh.pfc.historicalload.models;

import com.roh.pfc.historicalload.entity.Transaction;
import com.roh.pfc.historicalload.facade.TransactionStoreBatchWriteItem;
import io.soabase.recordbuilder.core.RecordBuilder;
import java.util.Optional;

@RecordBuilder
public record TransactionStoreProcessingContext(
        TransactionStoreBatchWriteItem transactionStoreBatchWriteItem,
        TransactionStoreProcessingStatus processingStatus,
        Optional<Transaction> transaction,
        Optional<String> errorMessage,
        Optional<Transaction> previousTransactionStoreRequest) {

    public TransactionStoreProcessingContext(TransactionStoreBatchWriteItem transactionStoreBatchWriteItem) {
        this(
                transactionStoreBatchWriteItem,
                TransactionStoreProcessingStatus.NOT_SET,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }
}