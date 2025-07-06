package com.roh.pfc.historicalload.facade;

import com.fasterxml.jackson.databind.JsonNode;
import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record TransactionStoreBatchWriteItem(
        String accountReferenceId,
        String postedTransactionId,
        String financialCoreCommandId,
        String financialCoreCommandIdSource,
        String transactionEffectiveDate,
        String transactionCategory,
        String transactionProcessingDate,
        Integer postedTransactionSequenceNumber,
        Integer postedTransactionEventOrderId,
        String schemaReference,
        String instrumentVersionId,
        JsonNode event) {}