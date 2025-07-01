package com.hT.historicalload.controller;

import com.hT.historicalload.facade.TransactionStoreRequest;
import com.hT.historicalload.facade.TransactionStoreResponse;
import com.hT.historicalload.models.Transaction;
import com.hT.historicalload.service.TransactionStoreService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("historicalload/transactions")
public class TransactionStoreController {

    private final TransactionStoreService transactionStoreService;

    public TransactionStoreController(TransactionStoreService transactionStoreService) {
        this.transactionStoreService = transactionStoreService;
    }

    @PostMapping("/batch-write")
    public ResponseEntity<TransactionStoreResponse> batchWrite(
        @RequestBody TransactionStoreRequest transactionStoreRequest) {

        final List<Transaction> records = transactionStoreRequest.records();
        if (records == null || records.isEmpty()) {
            log.warn("Received empty transaction store request");
            return ResponseEntity.badRequest().build();
        }

        log.info("Received request to put transaction store items: {}", records.size());

        final TransactionStoreResponse transactionStoreResponse = transactionStoreService.putTransactions(records);

        log.info(
            "Transaction store ResponseType: {} - StatusCode: {}",
            transactionStoreResponse.transactionStoreResponseType(),
            transactionStoreResponse.transactionStoreResponseType().getHttpStatus());

        return ResponseEntity.status(
            transactionStoreResponse.transactionStoreResponseType().getHttpStatus())
            .body(transactionStoreResponse);
    }
}