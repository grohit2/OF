package com.hT.historicalload.facade;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.soabase.recordbuilder.core.RecordBuilder;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@RecordBuilder
public record TransactionStoreResponse(
    @JsonIgnore TransactionStoreResponseType transactionStoreResponseType,
    int totalSent,
    int writtenCount,
    int failedCount,
    List<TransactionStoreFailedItem> failedItems) {}