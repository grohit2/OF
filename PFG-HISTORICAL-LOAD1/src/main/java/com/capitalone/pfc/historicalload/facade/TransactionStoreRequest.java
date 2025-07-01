package com.hT.historicalload.facade;

import com.hT.historicalload.models.Transaction;
import io.soabase.recordbuilder.core.RecordBuilder;
import java.util.List;

@RecordBuilder
public record TransactionStoreRequest(List<Transaction> records) {}