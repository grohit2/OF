package com.roh.pfc.historicalload.facade;

import io.soabase.recordbuilder.core.RecordBuilder;
import java.util.List;

@RecordBuilder
public record TransactionStoreBatchWriteRequest(List<TransactionStoreBatchWriteItem> transactions) {}