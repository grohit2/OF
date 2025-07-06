package com.roh.pfc.historicalload.repository;

import com.roh.pfc.historicalload.models.TransactionStoreProcessingContext;

public interface TransactionStoreRepository {

    TransactionStoreProcessingContext putItem(TransactionStoreProcessingContext transactionStoreProcessingContext);
}
