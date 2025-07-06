package com.roh.pfc.historicalload.service.impl;

import com.roh.pfc.historicalload.HistoricalLoadAppProperties;
import com.roh.pfc.historicalload.facade.TransactionStoreBatchWriteFailedItem;
import com.roh.pfc.historicalload.facade.TransactionStoreBatchWriteItem;
import com.roh.pfc.historicalload.facade.TransactionStoreBatchWriteRequest;
import com.roh.pfc.historicalload.facade.TransactionStoreBatchWriteResponse;
import com.roh.pfc.historicalload.facade.TransactionStoreBatchWriteResponseBuilder;
import com.roh.pfc.historicalload.models.TransactionStoreProcessingStatus;
import com.roh.pfc.historicalload.models.TransactionStoreProcessingContext;
import com.roh.pfc.historicalload.models.TransactionStoreResponseType;
import com.roh.pfc.historicalload.repository.AccountStoreRepository;
import com.roh.pfc.historicalload.repository.TransactionStoreRepository;
import com.roh.pfc.historicalload.service.TransactionStoreMappingService;
import com.roh.pfc.historicalload.service.TransactionStoreService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class TransactionStoreServiceImpl implements TransactionStoreService {

    private final HistoricalLoadAppProperties historicalLoadAppProperties;
    private final TransactionStoreMappingService transactionStoreMappingService;
    private final TransactionStoreRepository transactionStoreRepository;
    private final AccountStoreRepository accountStoreRepository;

    public TransactionStoreServiceImpl(
            final HistoricalLoadAppProperties historicalLoadAppProperties,
            final TransactionStoreMappingService transactionStoreMappingService,
            final TransactionStoreRepository transactionStoreRepository,
            final AccountStoreRepository accountStoreRepository) {
        this.historicalLoadAppProperties = historicalLoadAppProperties;
        this.transactionStoreMappingService = transactionStoreMappingService;
        this.transactionStoreRepository = transactionStoreRepository;
        this.accountStoreRepository = accountStoreRepository;
    }

    @Override
    public TransactionStoreBatchWriteResponse putTransactions(
            final List<TransactionStoreBatchWriteItem> transactionStoreBatchWriteItems) {

        final List<TransactionStoreProcessingContext> processedContexts =
                transactionStoreBatchWriteItems.stream()
                        // create processing context for each request
                        .map(TransactionStoreProcessingContext::new)
                        // map batch write request item to transaction
                        .map(transactionStoreMappingService::mapBatchWriteRequestToTransaction)
                        // validate provision is present in account store if enabled
                        .map(ctx -> historicalLoadAppProperties.isEnableAccountStoreProvisionValidation()
                                ? accountStoreRepository.checkProvisionExists(ctx)
                                : ctx)
                        // insert to dynamodb
                        .map(transactionStoreRepository::putItem)
                        .toList();

        // create transactionStoreRequestItem store response based on processed contexts
        return createTransactionStoreResponse(processedContexts);
    }

    private TransactionStoreBatchWriteResponse createTransactionStoreResponse(
            final List<TransactionStoreProcessingContext> processedContexts) {

        // Total number of transactions processed
        int totalRecordsProcessed = processedContexts.size();

        // Total written to dynamodb
        int totalRecordsWritten = (int) processedContexts.stream()
                .filter(context -> context.processingStatus() == TransactionStoreProcessingStatus.SUCCESSFULLY_PUT_ITEM)
                .count();

        // Total failed validation or dynamodb insert
        int totalRecordsFailed = totalRecordsProcessed - totalRecordsWritten;

        // Create base response builder
        final TransactionStoreBatchWriteResponseBuilder transactionStoreResponseBuilder =
                TransactionStoreBatchWriteResponseBuilder.builder()
                        .totalSent(totalRecordsProcessed)
                        .totalWritten(totalRecordsWritten)
                        .failedCount(totalRecordsFailed);

        // if all transactions are successful, return success response
        boolean allRecordsProcessedSuccessfully = processedContexts.stream()
                .allMatch(context -> context.processingStatus() == TransactionStoreProcessingStatus.SUCCESSFULLY_PUT_ITEM);

        if (allRecordsProcessedSuccessfully) {
            return transactionStoreResponseBuilder
                    .transactionStoreResponseType(TransactionStoreResponseType.ALL_ITEMS_PROCESSED_SUCCESSFULLY)
                    .failedItems(null) // No failed items in this case
                    .build();
        }

        // if all transactions failed validation, then return Bad Request response
        boolean allRecordsFailedValidation = processedContexts.stream()
                .allMatch(context -> context.processingStatus() == TransactionStoreProcessingStatus.FAILED_VALIDATION);

        if (allRecordsFailedValidation) {
            return transactionStoreResponseBuilder
                    .transactionStoreResponseType(TransactionStoreResponseType.ALL_ITEMS_FAILED_VALIDATION)
                    .failedItems(toFailedItems(processedContexts))
                    .build();
        }

        // if all transactions failed conditional check, return Conflict response
        boolean allRecordsFailedConditionalCheck = processedContexts.stream()
                .allMatch(context -> context.processingStatus() == TransactionStoreProcessingStatus.FAILED_CONDITIONAL_CHECK);

        if (allRecordsFailedConditionalCheck) {
            return transactionStoreResponseBuilder
                    .transactionStoreResponseType(TransactionStoreResponseType.ALL_ITEMS_FAILED_CONDITIONAL_CHECK)
                    .failedItems(toFailedItems(processedContexts))
                    .build();
        }

        // if all transactions failed db insert, return Server error response
        boolean allRecordsFailedDbInsert = processedContexts.stream()
                .allMatch(context -> context.processingStatus() == TransactionStoreProcessingStatus.FAILED_PUT_ITEM
                        || context.processingStatus() == TransactionStoreProcessingStatus.FAILED_GET_ITEM);

        if (allRecordsFailedDbInsert) {
            return transactionStoreResponseBuilder
                    .transactionStoreResponseType(TransactionStoreResponseType.ALL_ITEMS_FAILED_PUT_ITEM)
                    .failedItems(toFailedItems(processedContexts))
                    .build();
        }

        // if we have at least one record processed successfully, then return partial response
        boolean atLeastOneRecordProcessedSuccessfully = processedContexts.stream()
                .anyMatch(context -> context.processingStatus() == TransactionStoreProcessingStatus.SUCCESSFULLY_PUT_ITEM);

        if (atLeastOneRecordProcessedSuccessfully) {
            return transactionStoreResponseBuilder
                    .transactionStoreResponseType(TransactionStoreResponseType.PARTIAL_ITEMS_PROCESSED_SUCCESSFULLY)
                    .failedItems(toFailedItems(processedContexts))
                    .build();
        }

        // mixed failed response case, return a response indicating failure
        return transactionStoreResponseBuilder
                .transactionStoreResponseType(TransactionStoreResponseType.UNKNOWN_ERRORS)
                .failedItems(toFailedItems(processedContexts))
                .build();
    }

    private List<TransactionStoreBatchWriteFailedItem> toFailedItems(List<TransactionStoreProcessingContext> processedContexts) {
        return processedContexts.stream()
                .filter(context -> context.processingStatus() != TransactionStoreProcessingStatus.SUCCESSFULLY_PUT_ITEM)
                .map(context -> TransactionStoreBatchWriteFailedItem.builder()
                        .accountReferenceId(context.transactionStoreBatchWriteItem().accountReferenceId())
                        .errorCode(context.processingStatus().getStatusCode())
                        .reason(context.errorMessage().orElse(null))
                        .existingItem(context.previousTransactionStoreRequest().orElse(null))
                        .build())
                .toList();
    }
}
