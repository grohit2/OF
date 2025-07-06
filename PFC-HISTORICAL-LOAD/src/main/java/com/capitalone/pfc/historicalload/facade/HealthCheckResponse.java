package com.roh.pfc.historicalload.facade;

import java.util.Optional;

public record HealthCheckResponse(
        String status, String appVersion, String region, String clusterId, String containerId) {
    public HealthCheckResponse() {
        this(
                "Up",
                Optional.ofNullable(System.getenv("APP_VERSION")).orElse("NOT_SET"),
                Optional.ofNullable(System.getenv("AWS_REGION")).orElse("Region-NA"),
                Optional.ofNullable(System.getenv("CLUSTER_ID")).orElse("Cluster-Id-NA"),
                Optional.ofNullable(System.getenv("CONTAINER_ID")).orElse("Container-Id-NA"));
    }
}