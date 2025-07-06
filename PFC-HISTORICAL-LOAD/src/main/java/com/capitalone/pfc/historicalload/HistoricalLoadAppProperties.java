package com.roh.pfc.historicalload;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties(prefix = HistoricalLoadConstants.HISTORICAL_LOAD_PROPERTIES_PREFIX)
@Validated
public class HistoricalLoadAppProperties {

    @Data
    public static class ChassisEncryptionConfig {
        private String password;

        @ToString.Exclude
        private String salt;

        @ToString.Exclude
        private Integer bits;

        private Integer poolSize;
    }

    private String pfcEnv;
    private String awsRegion;
    private boolean enableAccountStoreProvisionValidation;
    private String accountStoreTableName;
    private String transactionStoreTableName;
    private String dynamoDbEndpoint;
    private ChassisEncryptionConfig chassisEncryptionConfig;
}
