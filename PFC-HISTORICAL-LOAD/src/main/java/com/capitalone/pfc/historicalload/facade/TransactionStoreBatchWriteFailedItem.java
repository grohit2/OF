package com.roh.pfc.historicalload.facade;

import com.roh.pfc.historicalload.entity.Transaction;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.soabase.recordbuilder.core.RecordBuilder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@RecordBuilder
public record TransactionStoreBatchWriteFailedItem(
        String accountReferenceId, String errorCode, String reason, Transaction existingItem) {}