package com.roh.pfc.historicalload.service;

import com.roh.pfc.historicalload.facade.TransactionStoreBatchWriteItem;
import com.roh.pfc.historicalload.facade.TransactionStoreBatchWriteResponse;

import java.util.List;

public interface TransactionStoreService {

    TransactionStoreBatchWriteResponse putTransactions(
            List<TransactionStoreBatchWriteItem> transactionStoreBatchWriteItems);
}
