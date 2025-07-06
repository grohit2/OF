package com.roh.pfc.historicalload.service.impl;

import com.roh.chassis.engine.secureddatum.EncryptedDatum;
import com.roh.pfc.historicalload.entity.Transaction;
import com.roh.pfc.historicalload.entity.TransactionBuilder;
import com.roh.pfc.historicalload.facade.TransactionStoreBatchWriteItem;
import com.roh.pfc.historicalload.models.ReferenceIdField;
import com.roh.pfc.historicalload.models.TransactionStoreProcessingContext;
import com.roh.pfc.historicalload.models.TransactionStoreProcessingContextBuilder;
import com.roh.pfc.historicalload.models.TransactionStoreProcessingStatus;
import com.roh.pfc.historicalload.service.TransactionStoreMappingService;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class TransactionStoreMappingServiceImpl implements TransactionStoreMappingService {

    // Account Reference ID is expected to be in the format:
    // "accountId=982cafbb-68dc-4e59-9db4-d655a002abe3~~sortId=999"
    private static final String DELIMITER = "~~";
    private static final String EQUAL = "=";

    private final EncryptedDatum encryptedDatum;

    public TransactionStoreMappingServiceImpl(EncryptedDatum encryptedDatum) {
        this.encryptedDatum = encryptedDatum;
    }

    @Override
    public TransactionStoreProcessingContext mapBatchWriteRequestToTransaction(
            final TransactionStoreProcessingContext context) {

        final TransactionStoreBatchWriteItem transactionStoreBatchWriteItem = context.transactionStoreBatchWriteItem();
        final TransactionStoreProcessingContextBuilder contextBuilder =
                TransactionStoreProcessingContextBuilder.builder(context);
        final TransactionStoreProcessingContextBuilder validationFailedContextBuilder =
                contextBuilder.processingStatus(TransactionStoreProcessingStatus.FAILED_VALIDATION);

        // Check Required Fields

        final String accountReferenceId = transactionStoreBatchWriteItem.accountReferenceId();
        if (StringUtils.isEmpty(accountReferenceId)) {
            return validationFailedContextBuilder
                    .errorMessage(Optional.of("accountReferenceId field is required"))
                    .build();
        }

        if (StringUtils.isEmpty(transactionStoreBatchWriteItem.transactionCategory())) {
            return validationFailedContextBuilder
                    .errorMessage(Optional.of("transactionCategory field is required"))
                    .build();
        }

        final String effectiveDate = transactionStoreBatchWriteItem.transactionEffectiveDate();
        if (StringUtils.isEmpty(effectiveDate)) {
            return validationFailedContextBuilder
                    .errorMessage(Optional.of("transactionEffectiveDate field is required"))
                    .build();
        }

        final Integer postedTransactionEventOrderId = transactionStoreBatchWriteItem.postedTransactionEventOrderId();
        if (postedTransactionEventOrderId == null) {
            return validationFailedContextBuilder
                    .errorMessage(Optional.of("postedTransactionEventOrderId field is required"))
                    .build();
        }

        final Integer sequenceNumber = transactionStoreBatchWriteItem.postedTransactionSequenceNumber();
        if (sequenceNumber == null) {
            return validationFailedContextBuilder
                    .errorMessage(Optional.of("postedTransactionSequenceNumber field is required"))
                    .build();
        }

        final String financialCoreCommandId = transactionStoreBatchWriteItem.financialCoreCommandId();
        if (StringUtils.isEmpty(financialCoreCommandId)) {
            return validationFailedContextBuilder
                    .errorMessage(Optional.of("financialCoreCommandId field is required"))
                    .build();
        }

        final String financialCoreCommandIdSource = transactionStoreBatchWriteItem.financialCoreCommandIdSource();
        if (StringUtils.isEmpty(financialCoreCommandIdSource)) {
            return validationFailedContextBuilder
                    .errorMessage(Optional.of("financialCoreCommandIdSource field is required"))
                    .build();
        }

        // Decrypt the accountReferenceId
        final String contractId;
        final String accountId;
        final String sortId;
        try {
            contractId = encryptedDatum.decryptBase64Encoded(accountReferenceId);
            if (contractId == null || contractId.isEmpty()) {
                return validationFailedContextBuilder
                        .errorMessage(Optional.of("contractId cannot be null or empty"))
                        .build();
            }
            final Map<String, String> aridMap = new TreeMap<>(Stream.of(contractId.split(DELIMITER))
                    .map(s -> StringUtils.splitPreserveAllTokens(s, EQUAL))
                    .collect(Collectors.toMap(
                            keyValues -> checkArgument(keyValues, 0), keyValues -> checkArgument(keyValues, 1))));

            accountId = aridMap.getOrDefault(ReferenceIdField.ACCOUNT_ID, "");
            sortId = aridMap.getOrDefault(ReferenceIdField.SORT_ID, "");
        } catch (Exception e) {
            return validationFailedContextBuilder
                    .errorMessage(Optional.of("accountReferenceId cannot be decrypted: " + e.getMessage()))
                    .build();
        }
        if (accountId.isBlank() || sortId.isBlank()) {
            return validationFailedContextBuilder
                    .errorMessage(Optional.of("Decrypted accountId or sortId cannot be empty"))
                    .build();
        }

        // Map TransactionStoreBatchWriteItem to Transaction
        final Transaction transaction = TransactionBuilder.builder()
                .accountPartition(contractId + "~" + transactionStoreBatchWriteItem.transactionCategory())
                .orderKey(effectiveDate + "~" + transformedSequenceId(sequenceNumber) + "_")
                .sortKey(String.format("%06d", postedTransactionEventOrderId))
                .commandKey(financialCoreCommandId + "~" + financialCoreCommandIdSource)
                .contractId(contractId)
                // TODO: Event validation with avro schema
                .event(compressEvent(transactionStoreBatchWriteItem.event()))
                .financialCoreCommandId(financialCoreCommandId)
                .financialCoreCommandIdSource(financialCoreCommandIdSource)
                .instrumentVersionId(transactionStoreBatchWriteItem.instrumentVersionId())
                .postedTransactionEventOrderId(postedTransactionEventOrderId)
                .postedTransactionId(transactionStoreBatchWriteItem.postedTransactionId())
                .postedTransactionSequenceNumber(sequenceNumber)
                .schemaReference(transactionStoreBatchWriteItem.schemaReference())
                .transactionCategory(transactionStoreBatchWriteItem.transactionCategory())
                .transactionEffectiveDate(effectiveDate)
                .transactionProcessingDate(transactionStoreBatchWriteItem.transactionProcessingDate())
                .build();

        return TransactionStoreProcessingContextBuilder.builder(context)
                .transaction(Optional.of(transaction))
                .build();
    }

    private static String checkArgument(String[] array, int index) {
        if (ArrayUtils.getLength(array) > index) {
            return StringUtils.defaultString(array[index]);
        }
        throw new IllegalArgumentException(
                String.format("Chunk [%s] [%d] is not a valid entry", Arrays.toString(array), index));
    }

    private static String transformedSequenceId(Integer sequenceNumber) {
        if (sequenceNumber == null) {
            return null;
        }
        // if sequenceNumber is positive,format with "P" followed by the sequence id padded to be 11 digits long
        if (sequenceNumber >= 0) {
            return "P" + String.format("%010d", sequenceNumber);
        } else {
            // negative sequence ID values it is "N" followed by the sequence id padded to be 11 digits long
            return "N" + String.format("%010d", Math.abs(sequenceNumber));
        }
    }

    public static byte[] compressEvent(JsonNode input) {
        if (input == null || input.isEmpty()) {
            return new byte[0];
        }

        try {
            // convert str to bytes
            byte[] inputBytes = input.toString().getBytes(StandardCharsets.UTF_8);

            // Create a Deflater obj for compression
            Deflater deflater = new Deflater();
            deflater.setInput(inputBytes);
            deflater.finish();

            // Use a ByteArrayOutputStream to hold the compressed data
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                while (!deflater.finished()) {
                    int bytesCompressed = deflater.deflate(buffer);
                    outputStream.write(buffer, 0, bytesCompressed);
                }

                deflater.end();

                // Get the compressed data and encode it in base64
                byte[] compressedBytes = outputStream.toByteArray();
                return Base64.getEncoder().encode(compressedBytes);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error compressing event object: " + e.getMessage(), e);
        }
    }
}