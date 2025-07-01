package com.hT.historicalload.service.impl;

import com.hT.historicalload.models.Transaction;
import com.hT.historicalload.models.TransactionStoreProcessingContext;
import com.hT.historicalload.models.TransactionStoreProcessingContextBuilder;
import com.hT.historicalload.models.TransactionStoreProcessingStatus;
import com.hT.historicalload.service.TransactionStoreValidationService;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.zip.Inflater;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TransactionStoreValidationServiceImpl implements TransactionStoreValidationService {

    @Override
    public TransactionStoreProcessingContext validateRequest(final TransactionStoreProcessingContext context) {
        final Transaction request = context.transaction();
        if (request.accountPartition() == null || request.accountPartition().isEmpty()) {
            return TransactionStoreProcessingContextBuilder.builder(context)
                .processingStatus(TransactionStoreProcessingStatus.FAILED_VALIDATION)
                .errorMessage(Optional.of("Account partition is required"))
                .build();
        }
        if (request.orderKey() == null || request.orderKey().isEmpty()) {
            return TransactionStoreProcessingContextBuilder.builder(context)
                .processingStatus(TransactionStoreProcessingStatus.FAILED_VALIDATION)
                .errorMessage(Optional.of("Order key is required"))
                .build();
        }
        if (request.event() == null || request.event().isEmpty()) {
            return TransactionStoreProcessingContextBuilder.builder(context)
                .processingStatus(TransactionStoreProcessingStatus.FAILED_VALIDATION)
                .errorMessage(Optional.of("Event is required"))
                .build();
        }

        // decode and decompress the event JSON
        final Optional<String> eventJson = eventJson(request.event());
        if (eventJson.isEmpty()) {
            return TransactionStoreProcessingContextBuilder.builder(context)
                .processingStatus(TransactionStoreProcessingStatus.FAILED_VALIDATION)
                .errorMessage(Optional.of("Unable to decode or decompress event JSON"))
                .build();
        }

        // TODO: Add avro validation check for event
        return context;
    }

    private Optional<String> eventJson(final String binaryEvent) {
        // decode base64 encoded string
        final Base64.Decoder decoder = Base64.getDecoder();
        final byte[] compressedData = decoder.decode(binaryEvent);

        // decompress the data using Inflater
        final Inflater inflater = new Inflater();
        inflater.setInput(compressedData);
        try {
            byte[] buffer = new byte[1024];
            final StringBuilder output = new StringBuilder();
            int bytesRead;
            while (!inflater.finished()) {
                bytesRead = inflater.inflate(buffer);
                if (bytesRead > 0) {
                    output.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
                }
            }
            // Return the decompressed JSON string
            return Optional.of(output.toString());
        } catch (Exception e) {
            log.error("Error decompressing event JSON: {}", e.getMessage(), e);
            return Optional.empty();
        } finally {
            inflater.end();
        }
    }
}