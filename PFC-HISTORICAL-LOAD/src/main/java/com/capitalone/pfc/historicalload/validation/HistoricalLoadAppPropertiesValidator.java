package com.roh.pfc.historicalload.validation;

import com.roh.pfc.historicalload.HistoricalLoadAppProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Slf4j
@Component
public class HistoricalLoadAppPropertiesValidator implements Validator {

    @Override
    public boolean supports(final Class<?> type) {
        return type == HistoricalLoadAppProperties.class;
    }

    @Override
    public void validate(Object target, Errors errors) {
        final HistoricalLoadAppProperties historicalLoadAppProperties = (HistoricalLoadAppProperties) target;

        if (historicalLoadAppProperties.getPfcEnv() == null
                || historicalLoadAppProperties.getPfcEnv().isEmpty()) {
            errors.rejectValue("pfcEnv", "pfcEnv.empty", "PFC environment must not be empty");
        }

        if (historicalLoadAppProperties.getAwsRegion() == null
                || historicalLoadAppProperties.getAwsRegion().isEmpty()) {
            errors.rejectValue("awsRegion", "awsRegion.empty", "AWS region must not be empty");
        }

        final HistoricalLoadAppProperties.ChassisEncryptionConfig chassisEncryptionConfig =
                historicalLoadAppProperties.getChassisEncryptionConfig();

        if (chassisEncryptionConfig == null) {
            errors.rejectValue(
                    "chassisEncryptionConfig",
                    "chassisEncryptionConfig.null",
                    "Chassis encryption configuration must not be null");
        }

        if (chassisEncryptionConfig != null && !StringUtils.hasText(chassisEncryptionConfig.getPassword())) {
            errors.rejectValue(
                    "chassisEncryptionConfig.password",
                    "chassisEncryptionConfig.password.empty",
                    "Chassis encryption password must not be empty");
        }

        if (chassisEncryptionConfig != null && !StringUtils.hasText(chassisEncryptionConfig.getSalt())) {
            errors.rejectValue(
                    "chassisEncryptionConfig.salt",
                    "chassisEncryptionConfig.salt.empty",
                    "Chassis encryption salt must not be empty");
        }

        if (historicalLoadAppProperties.getTransactionStoreTableName() == null
                || historicalLoadAppProperties.getTransactionStoreTableName().isEmpty()) {
            errors.rejectValue(
                    "dynamoDbHistoricalLoadTableName",
                    "dynamoDbHistoricalLoadTableName.empty",
                    "DynamoDB table name must not be empty");
        }

        log.info("Historical load historicalLoadAppProperties validated successfully: {}", historicalLoadAppProperties);
    }
}
