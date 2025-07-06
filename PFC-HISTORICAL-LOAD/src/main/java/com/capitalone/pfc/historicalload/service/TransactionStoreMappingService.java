package com.roh.pfc.historicalload.service;

import com.roh.pfc.historicalload.models.TransactionStoreProcessingContext;

public interface TransactionStoreMappingService {

    TransactionStoreProcessingContext mapBatchWriteRequestToTransaction(
            TransactionStoreProcessingContext transactionStoreProcessingContext);
}
