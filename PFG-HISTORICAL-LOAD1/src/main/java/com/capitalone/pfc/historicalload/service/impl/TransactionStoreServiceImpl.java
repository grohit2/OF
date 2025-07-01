package com.hT.historicalload.service.impl;

import com.hT.historicalload.facade.*;
import com.hT.historicalload.models.Transaction;
import com.hT.historicalload.models.TransactionStoreProcessingContext;
import com.hT.historicalload.models.TransactionStoreProcessingStatus;
import com.hT.historicalload.repository.TransactionStoreRepository;
import com.hT.historicalload.service.TransactionStoreService;
import com.hT.historicalload.service.TransactionStoreValidationService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TransactionStoreServiceImpl implements TransactionStoreService {

    private final TransactionStoreValidationService transactionStoreValidationService;
    private final TransactionStoreRepository transactionStoreRepository;

    public TransactionStoreServiceImpl(
        TransactionStoreValidationService transactionStoreValidationService,
        TransactionStoreRepository transactionStoreRepository) {
        this.transactionStoreValidationService = transactionStoreValidationService;
        this.transactionStoreRepository = transactionStoreRepository;
    }

    @Override
    public TransactionStoreResponse putTransactions(final List<Transaction> transactions) {
        final List<TransactionStoreProcessingContext> processedContexts = transactions.stream()
            // create processing context for each request
            .map(TransactionStoreProcessingContext::new)
            // do validation
            .map(transactionStoreValidationService::validateRequest)
            // insert to dynamodb
            .map(transactionStoreRepository::putItem)
            .toList();

        // create transaction store response based on processed contexts
        return createTransactionStoreResponse(processedContexts);
    }

    private TransactionStoreResponse createTransactionStoreResponse(
        final List<TransactionStoreProcessingContext> processedContexts) {

        // Total number of records processed
        int totalRecordsProcessed = processedContexts.size();
        // Total written to dynamodb
        int totalRecordsWritten = (int) processedContexts.stream()
            .filter(context -> context.processingStatus() == TransactionStoreProcessingStatus.SUCCESSFULLY_PUT_ITEM)
            .count();
        // Total failed validation or dynamodb insert
        int totalRecordsFailed = totalRecordsProcessed - totalRecordsWritten;

        // Create base response builder
        final TransactionStoreResponseBuilder transactionStoreResponseBuilder =
            TransactionStoreResponseBuilder.builder()
                .totalSent(totalRecordsProcessed)
                .writtenCount(totalRecordsWritten)
                .failedCount(totalRecordsFailed);

        // If all records processed are successful, return success response
        boolean allRecordsProcessedSuccessfully = processedContexts.stream()
            .allMatch(context ->
                context.processingStatus() == TransactionStoreProcessingStatus.SUCCESSFULLY_PUT_ITEM);
        if (allRecordsProcessedSuccessfully) {
            return transactionStoreResponseBuilder
                .transactionStoreResponseType(TransactionStoreResponseType.ALL_ITEMS_PROCESSED_SUCCESSFULLY)
                .failedItems(null) // No failed items in this case
                .build();
        }

        // If all records failed validation, then return Bad Request response
        boolean allRecordsFailedValidation = processedContexts.stream()
            .allMatch(context ->
                context.processingStatus() == TransactionStoreProcessingStatus.FAILED_VALIDATION);
        if (allRecordsFailedValidation) {
            return transactionStoreResponseBuilder
                .transactionStoreResponseType(TransactionStoreResponseType.ALL_ITEMS_FAILED_VALIDATION)
                .failedItems(toFailedItems(processedContexts))
                .build();
        }

        // If all records failed conditional check, return Conflict response
        boolean allRecordsFailedConditionalCheck = processedContexts.stream()
            .allMatch(context ->
                context.processingStatus() == TransactionStoreProcessingStatus.FAILED_CONDITIONAL_CHECK);
        if (allRecordsFailedConditionalCheck) {
            return transactionStoreResponseBuilder
                .transactionStoreResponseType(TransactionStoreResponseType.ALL_ITEMS_FAILED_CONDITIONAL_CHECK)
                .failedItems(toFailedItems(processedContexts))
                .build();
        }

        // If all records failed db insert, return Server error response
        boolean allRecordsFailedDbInsert = processedContexts.stream()
            .allMatch(context ->
                context.processingStatus() == TransactionStoreProcessingStatus.FAILED_PUT_ITEM);
        if (allRecordsFailedDbInsert) {
            return transactionStoreResponseBuilder
                .transactionStoreResponseType(TransactionStoreResponseType.ALL_ITEMS_FAILED_PUT_ITEM)
                .failedItems(toFailedItems(processedContexts))
                .build();
        }

        // If we have at least one record processed successfully, then return partial response
        boolean atLeastOneRecordProcessedSuccessfully = processedContexts.stream()
            .anyMatch(context ->
                context.processingStatus() == TransactionStoreProcessingStatus.SUCCESSFULLY_PUT_ITEM);
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

    private List<TransactionStoreFailedItem> toFailedItems(
        final List<TransactionStoreProcessingContext> processedContexts) {
        return processedContexts.stream()
            .filter(context -> context.processingStatus() != TransactionStoreProcessingStatus.SUCCESSFULLY_PUT_ITEM)
            .map(context -> TransactionStoreFailedItem.builder()
                .accountPartition(context.transaction().accountPartition())
                .orderKey(context.transaction().orderKey())
                .errorCode(context.processingStatus().getStatusCode())
                .reason(context.errorMessage().orElse(null))
                .existingItem(context.previousTransactionStoreRequest().orElse(null))
                .build())
            .toList();
    }
}