package com.roh.pfc.historicalload.repository;

import com.roh.pfc.historicalload.models.TransactionStoreProcessingContext;

public interface AccountStoreRepository {

    TransactionStoreProcessingContext checkProvisionExists(TransactionStoreProcessingContext processingContext);
}
