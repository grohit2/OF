@Slf4j
@RestController
@RequestMapping("historicalload/transactions")
public class TransactionStoreController {

    private final TransactionStoreService transactionStoreService;

    public TransactionStoreController(final TransactionStoreService transactionStoreService) {
        this.transactionStoreService = transactionStoreService;
    }

    @PostMapping("/batch-write")
    public ResponseEntity<TransactionStoreBatchWriteResponse> batchWrite(
        @RequestBody final TransactionStoreBatchWriteRequest transactionStoreBatchWriteRequest) {

        final List<TransactionStoreBatchWriteItem> transactions = transactionStoreBatchWriteRequest.transactions();
        if (transactions == null || transactions.isEmpty()) {
            log.warn("Received empty transactionStoreRequestItem store request");
            return ResponseEntity.badRequest().build();
        }

        log.info("Received request to put transactionStoreRequestItem store items: {}", transactions.size());

        final TransactionStoreBatchWriteResponse transactionStoreResponse = 
            transactionStoreService.putTransactions(transactions);

        log.info(
            "Transaction store ResponseType: {} - StatusCode: {}",
            transactionStoreResponse.transactionStoreResponseType(),
            transactionStoreResponse.transactionStoreResponseType().getHttpStatus());

        return ResponseEntity.status(
            transactionStoreResponse.transactionStoreResponseType().getHttpStatus())
            .body(transactionStoreResponse);
    }
}