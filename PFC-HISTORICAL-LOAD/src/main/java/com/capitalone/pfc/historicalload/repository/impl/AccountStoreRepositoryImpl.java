package com.roh.pfc.historicalload.repository.impl;

import com.roh.pfc.historicalload.HistoricalLoadAppProperties;
import com.roh.pfc.historicalload.entity.Transaction;
import com.roh.pfc.historicalload.models.TransactionStoreProcessingContext;
import com.roh.pfc.historicalload.models.TransactionStoreProcessingContextBuilder;
import com.roh.pfc.historicalload.models.TransactionStoreProcessingStatus;
import com.roh.pfc.historicalload.repository.AccountStoreRepository;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class AccountStoreRepositoryImpl implements AccountStoreRepository {

    private final String accountStoreTableName;
    private final DynamoDbClient dynamoDbClient;

    public AccountStoreRepositoryImpl(
            final HistoricalLoadAppProperties historicalLoadAppProperties, final DynamoDbClient dynamoDbClient) {
        this.accountStoreTableName = historicalLoadAppProperties.getAccountStoreTableName();
        this.dynamoDbClient = dynamoDbClient;

        log.info("Initializing AccountStoreRepository with table name: {}", accountStoreTableName);
    }

    @Override
    public TransactionStoreProcessingContext checkProvisionExists(TransactionStoreProcessingContext processingContext) {
        if (processingContext.transaction().isEmpty()) {
            return processingContext;
        }

        final Transaction transaction = processingContext.transaction().get();
        final String accountReferenceId = processingContext.transactionStoreBatchWriteItem().accountReferenceId();
        final GetItemRequest getItemRequest = GetItemRequest.builder()
                .key(createAttributesValueMap(transaction))
                .tableName(accountStoreTableName)
                .build();

        try {
            // If there is no matching item, GetItem does not return any data.
            final Map<String, AttributeValue> returnedItem =
                    dynamoDbClient.getItem(getItemRequest).item();

            if (returnedItem.isEmpty()) {
                log.error("No provision record found in account table: {} for accountReferenceId: {}",
                        accountStoreTableName, accountReferenceId);
                return TransactionStoreProcessingContextBuilder.builder(processingContext)
                        .processingStatus(TransactionStoreProcessingStatus.FAILED_VALIDATION)
                        .errorMessage(Optional.of("No provision record found in account store"))
                        .build();
            } else {
                // validation passed so just return current processing context
                return processingContext;
            }
        } catch (Exception e) {
            final String errorMessage =
                    String.format("Error getting item from account store. ErrorMessage: %s", e.getMessage());
            log.error(errorMessage, e);
            return TransactionStoreProcessingContextBuilder.builder(processingContext)
                    .processingStatus(TransactionStoreProcessingStatus.FAILED_GET_ITEM)
                    .errorMessage(Optional.of(errorMessage))
                    .build();
        }
    }

    private HashMap<String, AttributeValue> createAttributesValueMap(final Transaction transaction) {
        final HashMap<String, AttributeValue> itemValues = new HashMap<>();
        itemValues.put(
                "contractId",
                AttributeValue.builder().s(transaction.contractId()).build());

        // hardcoded for provision
        itemValues.put(
                "postedTransactionSequenceNumber",
                AttributeValue.builder().n("0").build());

        return itemValues;
    }
}
