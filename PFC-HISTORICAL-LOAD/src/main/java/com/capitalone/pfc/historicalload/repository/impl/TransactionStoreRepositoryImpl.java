package com.roh.pfc.historicalload.repository.impl;

import com.roh.pfc.historicalload.HistoricalLoadAppProperties;
import com.roh.pfc.historicalload.entity.Transaction;
import com.roh.pfc.historicalload.entity.TransactionBuilder;
import com.roh.pfc.historicalload.models.TransactionStoreProcessingContext;
import com.roh.pfc.historicalload.models.TransactionStoreProcessingContextBuilder;
import com.roh.pfc.historicalload.models.TransactionStoreProcessingStatus;
import com.roh.pfc.historicalload.repository.TransactionStoreRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure;

@Slf4j
public class TransactionStoreRepositoryImpl implements TransactionStoreRepository {

    private static final String transactionPutExpression = 
        "accountPartition <> :accountPartition AND orderKey <> :orderKey";
    
    private final String transactionStoreTableName;
    private final DynamoDbClient dynamoDbClient;
    
    public TransactionStoreRepositoryImpl(
        final HistoricalLoadAppProperties historicalLoadAppProperties, final DynamoDbClient dynamoDbClient) {
        this.transactionStoreTableName = historicalLoadAppProperties.getTransactionStoreTableName();
        this.dynamoDbClient = dynamoDbClient;
        log.info("Initializing TransactionStoreRepository with table name: {}", transactionStoreTableName);
    }
    
    @Override
    public TransactionStoreProcessingContext putItem(
        TransactionStoreProcessingContext transactionStoreProcessingContext) {
        
        // skip putting to dynamoDB if processing status is already set or transaction is not present
        if (transactionStoreProcessingContext.processingStatus() != TransactionStoreProcessingStatus.NOT_SET
            || transactionStoreProcessingContext.transaction().isEmpty()) {
            return transactionStoreProcessingContext;
        }
        
        final Transaction transaction = 
            transactionStoreProcessingContext.transaction().get();
        final String accountPartition = transaction.accountPartition();
        final String orderKey = transaction.orderKey();
        try {
            // create conditional expression for put item
            final Expression conditionalExpression = createConditionalExpression(transaction);
            // create PutItemRequest
            final PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(transactionStoreTableName)
                .item(createAttributeValueMap(transaction))
                .conditionExpression(conditionalExpression.expression())
                .expressionAttributeValues(conditionalExpression.expressionValues())
                .returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure.ALL_OLD)
                .build();
            
            // insert item into DynamoDB
            dynamoDbClient.putItem(putItemRequest);
            log.info(
                "Successfully Put Item in DynamoDB. AccountPartition: {}, OrderId: {}", accountPartition, orderKey);
            return TransactionStoreProcessingContextBuilder.builder(transactionStoreProcessingContext)
                .processingStatus(TransactionStoreProcessingStatus.SUCCESSFULLY_PUT_ITEM)
                .build();
        } catch (ConditionalCheckFailedException conditionalCheckFailedException) {
            log.warn("Conditional check failed for AccountPartition: {}, OrderKey: {}", accountPartition, orderKey);
            return TransactionStoreProcessingContextBuilder.builder(transactionStoreProcessingContext)
                .processingStatus(TransactionStoreProcessingStatus.FAILED_CONDITIONAL_CHECK)
                .errorMessage(Optional.of(String.format(
                    "Conditional check failed for AccountPartition: %s, OrderKey: %s",
                    accountPartition, orderKey)));
            // if conditional check fails, we need to get previous transactionStoreRequestItem store request
            .previousTransactionStoreRequest(toTransactionRequest(conditionalCheckFailedException))
            .build();
        } catch (final Exception e) {
            final String errorMessage = String.format(
                "Error putting item in transactionStoreRequestItem store: AccountPartition: %s, OrderKey: %s, " +
                "ErrorMessage: %s",
                accountPartition, orderKey, e.getMessage());
            log.error(errorMessage, e);
            return TransactionStoreProcessingContextBuilder.builder(transactionStoreProcessingContext)
                .processingStatus(TransactionStoreProcessingStatus.FAILED_PUT_ITEM)
                .errorMessage(Optional.of(errorMessage))
                .build();
        }
    }
    
    private Expression createConditionalExpression(final Transaction transaction) {
        return Expression.builder()
            .expression(transactionPutExpression)
            .expressionValues(Map.of(
                ":accountPartition",
                AttributeValue.builder()
                    .s(transaction.accountPartition())
                    .build(),
                ":orderKey",
                AttributeValue.builder().s(transaction.orderKey()).build()))
            .build();
    }
    
    private HashMap<String, AttributeValue> createAttributeValueMap(final Transaction transaction) {
        final HashMap<String, AttributeValue> itemValues = new HashMap<>();
        itemValues.put(
            "accountPartition",
            AttributeValue.builder().s(transaction.accountPartition()).build());
        itemValues.put(
            "orderKey", AttributeValue.builder().s(transaction.orderKey()).build());
        itemValues.put(
            "commandKey",
            AttributeValue.builder().s(transaction.commandKey()).build());
        itemValues.put(
            "contractId",
            AttributeValue.builder().s(transaction.contractId()).build());
        itemValues.put(
            "event",
            transaction.event() != null
                ? AttributeValue.builder()
                    .b(SdkBytes.fromByteArray(transaction.event()))
                    .build()
                : AttributeValue.builder().nul(true).build());
        itemValues.put(
            "financialCoreCommandId",
            AttributeValue.builder().s(transaction.financialCoreCommandId()).build());
        itemValues.put(
            "financialCoreCommandIdSource",
            AttributeValue.builder()
                .s(transaction.financialCoreCommandIdSource())
                .build());
        itemValues.put(
            "instrumentVersionId",
            AttributeValue.builder().s(transaction.instrumentVersionId()).build());
        itemValues.put(
            "postedTransactionEventOrderId",
            AttributeValue.builder()
                .n(String.valueOf(transaction.postedTransactionEventOrderId()))
                .build());
        itemValues.put(
            "postedTransactionId",
            AttributeValue.builder().s(transaction.postedTransactionId()).build());
        itemValues.put(
            "postedTransactionSequenceNumber",
            AttributeValue.builder()
                .n(String.valueOf(transaction.postedTransactionSequenceNumber()))
                .build());
        itemValues.put(
            "schemaReference",
            AttributeValue.builder().s(transaction.schemaReference()).build());
        itemValues.put(
            "transactionCategory",
            AttributeValue.builder().s(transaction.transactionCategory()).build());
        itemValues.put(
            "transactionEffectiveDate",
            AttributeValue.builder()
                .s(transaction.transactionEffectiveDate())
                .build());
        itemValues.put(
            "transactionProcessingDate",
            AttributeValue.builder()
                .s(transaction.transactionProcessingDate())
                .build());
        return itemValues;
    }
    
    private Optional<Transaction> toTransactionRequest(
        final ConditionalCheckFailedException conditionalCheckFailedException) {
        if (conditionalCheckFailedException != null && conditionalCheckFailedException.hasItem()) {
            final Map<String, AttributeValue> attributes = conditionalCheckFailedException.item();
            return Optional.of(TransactionBuilder.builder()
                .accountPartition(attributes.get("accountPartition").s())
                .orderKey(attributes.get("orderKey").s())
                .commandKey(attributes.get("commandKey").s())
                .contractId(attributes.get("contractId").s())
                .event(
                    attributes.get("event") != null
                        ? attributes.get("event").b().asByteArray()
                        : null)
                .financialCoreCommandId(
                    attributes.get("financialCoreCommandId").s())
                .financialCoreCommandIdSource(
                    attributes.get("financialCoreCommandIdSource").s())
                .instrumentVersionId(attributes.get("instrumentVersionId").s())
                .postedTransactionEventOrderId(Integer.parseInt(
                    attributes.get("postedTransactionEventOrderId").n()))
                .postedTransactionId(attributes.get("postedTransactionId").s())
                .postedTransactionSequenceNumber(Integer.parseInt(
                    attributes.get("postedTransactionSequenceNumber").n()))
                .schemaReference(attributes.get("schemaReference").s())
                .transactionCategory(attributes.get("transactionCategory").s())
                .transactionEffectiveDate(
                    attributes.get("transactionEffectiveDate").s())
                .transactionProcessingDate(
                    attributes.get("transactionProcessingDate").s())
                .build());
        }
        return Optional.empty();
    }
}