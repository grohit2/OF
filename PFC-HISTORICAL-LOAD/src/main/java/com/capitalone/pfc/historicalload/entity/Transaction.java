package com.roh.pfc.historicalload.entity;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record Transaction(
    String accountPartition,
    String orderKey,
    String commandKey,
    String contractId,
    byte[] event,
    String financialCoreCommandId,
    String financialCoreCommandIdSource,
    String instrumentVersionId,
    int postedTransactionEventOrderId,
    String postedTransactionId,
    int postedTransactionSequenceNumber,
    String schemaReference,
    String transactionCategory,
    String transactionEffectiveDate,
    String transactionProcessingDate) {}