package com.hT.historicalload.config;

import com.hT.historicalload.HistoricalLoadAppProperties;
import com.hT.historicalload.repository.TransactionStoreRepository;
import com.hT.historicalload.repository.impl.TransactionStoreRepositoryImpl;
import com.hT.historicalload.service.TransactionStoreService;
import com.hT.historicalload.service.TransactionStoreValidationService;
import com.hT.historicalload.service.impl.TransactionStoreServiceImpl;
import com.hT.historicalload.service.impl.TransactionStoreValidationServiceImpl;
import com.hT.historicalload.validation.HistoricalLoadAppPropertiesValidator;
import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Validator;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
@EnableConfigurationProperties(HistoricalLoadAppProperties.class)
@EnableWebMvc
public class HistoricalLoadConfig {

    @Bean
    public static Validator configurationPropertiesValidator() {
        return new HistoricalLoadAppPropertiesValidator();
    }

    @Bean(destroyMethod = "close")
    public DynamoDbClient dynamoDbClient(final HistoricalLoadAppProperties historicalLoadAppProperties) {
        return DynamoDbClient.builder()
            .endpointOverride(URI.create(historicalLoadAppProperties.getDynamoDbEndpoint()))
            .region(Region.of(historicalLoadAppProperties.getAwsRegion()))
            .build();
    }

    @Bean
    public TransactionStoreRepository transactionStoreRepository(
        final HistoricalLoadAppProperties historicalLoadAppProperties, final DynamoDbClient dynamoDbClient) {
        return new TransactionStoreRepositoryImpl(historicalLoadAppProperties, dynamoDbClient);
    }

    @Bean
    public TransactionStoreValidationService transactionStoreValidationService() {
        return new TransactionStoreValidationServiceImpl();
    }

    @Bean
    public TransactionStoreService transactionStoreService(
        final TransactionStoreValidationService transactionStoreValidationService,
        final TransactionStoreRepository transactionStoreRepository) {
        return new TransactionStoreServiceImpl(transactionStoreValidationService, transactionStoreRepository);
    }
}