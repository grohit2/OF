#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Re-creates the PFC-HISTORICAL-LOAD workspace (folders + placeholder files),
# now INCLUDING the impl classes inside src/.../service/impl/.
# ---------------------------------------------------------------------------

set -euo pipefail

BASE="PFC-HISTORICAL-LOAD"

echo ">> Creating top-level directories…"
mkdir -p "$BASE"/{.vscode,7ps_tests,docker,dynamodb-data,iam}
mkdir -p "$BASE"/dynamodb-resources/{dynamodb-migration,_data_historical_load,_schema_historical_load}

echo ">> Creating src/ tree…"
SRC="$BASE/src/main/java/com/capitalone/pfc/historicalload"
mkdir -p \
  "$SRC"/{config,controller,entity,facade,models,repository/impl,service/impl,validation} \
  "$BASE/src"/{main/resources,test,tools}

echo ">> Touching root-level files…"
touch "$BASE"/{7ps_cli.yml,.gitignore,.sdkmanrc,Boglefile,docker-compose.yaml,.GitVersion.yml,pom.xml,README.md,sm_secrets.yaml,test_config.yml,entrypoint.sh}

echo ">> Touching docker files…"
touch "$BASE"/docker/{entrypoint.sh,logback-spring.xml}

echo ">> Touching DynamoDB local DB file…"
touch "$BASE"/dynamodb-data/shared-local-instance.db

echo ">> Touching DynamoDB migration & data JSONs…"
touch "$BASE"/dynamodb-resources/dynamodb-migration/{01_populate_historical_load.json,02_populate_historical_load_pfc_account_store.json}
touch "$BASE"/dynamodb-resources/_data_historical_load/{01_historical_load_pfc_transaction_store.json,02_historical_load_pfc_account_store.json}
touch "$BASE"/dynamodb-resources/_schema_historical_load/.gitkeep   # placeholder

echo ">> Touching Java source placeholders…"
touch "$SRC"/config/HistoricalLoadConfig.java
touch "$SRC"/controller/{HealthController.java,TransactionStoreController.java}
touch "$SRC"/entity/Transaction.java
touch "$SRC"/facade/{HealthCheckResponse.java,TransactionStoreBatchWriteFailedItem.java,TransactionStoreBatchWriteItem.java,TransactionStoreBatchWriteRequest.java,TransactionStoreBatchWriteResponse.java,TransactionStoreResponseType.java}
touch "$SRC"/models/{ReferenceIdField.java,TransactionStoreProcessingContext.java,TransactionStoreProcessingStatus.java}
touch "$SRC"/repository/impl/{AccountStoreRepositoryImpl.java,TransactionStoreRepositoryImpl.java}
touch "$SRC"/repository/{AccountStoreRepository.java,TransactionStoreRepository.java}

# NEW: concrete service implementations
touch "$SRC"/service/impl/{TransactionStoreMappingServiceImpl.java,TransactionStoreServiceImpl.java}

# service interfaces (unchanged)
touch "$SRC"/service/{TransactionStoreMappingService.java,TransactionStoreService.java}

touch "$SRC"/validation/HistoricalLoadAppPropertiesValidator.java
touch "$SRC"/{HistoricalLoadAppMain.java,HistoricalLoadAppProperties.java,HistoricalLoadConstants.java}

echo ">> Adding .gitkeep files for otherwise-empty folders…"
touch "$BASE"/{7ps_tests/.gitkeep,src/main/resources/.gitkeep,src/test/.gitkeep,src/tools/.gitkeep}

echo ">> Done! Folder structure created at ./$BASE"
