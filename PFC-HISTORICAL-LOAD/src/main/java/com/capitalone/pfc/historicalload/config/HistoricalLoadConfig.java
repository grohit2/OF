package com.roh.pfc.historicalload.config;

import com.roh.chassis.engine.securedatum.EncryptedDatum;
import com.roh.pfc.historicalload.HistoricalLoadAppProperties;
import com.roh.pfc.historicalload.repository.AccountStoreRepository;
import com.roh.pfc.historicalload.repository.TransactionStoreRepository;
import com.roh.pfc.historicalload.repository.impl.AccountStoreRepositoryImpl;
import com.roh.pfc.historicalload.repository.impl.TransactionStoreRepositoryImpl;
import com.roh.pfc.historicalload.service.TransactionStoreMappingService;
import com.roh.pfc.historicalload.service.TransactionStoreService;
import com.roh.pfc.historicalload.service.impl.TransactionStoreMappingServiceImpl;
import com.roh.pfc.historicalload.service.impl.TransactionStoreServiceImpl;
import com.roh.pfc.historicalload.validation.HistoricalLoadAppPropertiesValidator;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.validation.Validator;
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
    public AccountStoreRepository accountStoreRepository(
        final HistoricalLoadAppProperties historicalLoadAppProperties, final DynamoDbClient dynamoDbClient) {
        return new AccountStoreRepositoryImpl(historicalLoadAppProperties, dynamoDbClient);
    }

    @Bean
    public EncryptedDatum encryptedDatum(final HistoricalLoadAppProperties historicalLoadAppProperties) {
        return new EncryptedDatum(
            historicalLoadAppProperties.getChassisEncryptionConfig().getPassword(),
            historicalLoadAppProperties.getChassisEncryptionConfig().getSalt(),
            historicalLoadAppProperties.getChassisEncryptionConfig().getInitV(),
            historicalLoadAppProperties.getChassisEncryptionConfig().getPoolSize());
    }

    @Bean
    public TransactionStoreMappingService transactionStoreValidationService(final EncryptedDatum encryptedDatum) {
        return new TransactionStoreMappingServiceImpl(encryptedDatum);
    }

    @Bean
    public TransactionStoreService transactionStoreService(
        final HistoricalLoadAppProperties historicalLoadAppProperties,
        final TransactionStoreMappingService transactionStoreMappingService,
        final TransactionStoreRepository transactionStoreRepository,
        final AccountStoreRepository accountStoreRepository) {
        return new TransactionStoreServiceImpl(
            historicalLoadAppProperties,
            transactionStoreMappingService,
            transactionStoreRepository,
            accountStoreRepository);
    }
}