#!/bin/sh

# SET ENVIRONMENT VARIABLES
VERSION_FILE='/app/git.properties'
if test -f $VERSION_FILE; then
    export APP_VERSION="sed -n 's/git.build.version//p' $VERSION_FILE"
else
    export APP_VERSION="NOT SET"
fi

# Get the ECS Container ID and Cluster ID values from the ECS metadata.
if [ -z "$ECS_CONTAINER_METADATA_URI" ]; then
    ECS_CONTAINER_METADATA=$(curl --connect-timeout 3 --max-time 5 "$ECS_CONTAINER_METADATA_URI")
    export CONTAINER_ID=$(echo "$ECS_CONTAINER_METADATA" | grep -o "\"Labels\":{[^}]*}" | grep -o "\"com.amazonaws.ecs.task-arn\":{[^}]*}" | grep -o "[^:]*$" | tr -d '",')
    export CLUSTER_ID=$(echo "$ECS_CONTAINER_METADATA" | grep -o "\"Labels\":{[^}]*}" | grep -o "\"com.amazonaws.ecs.cluster\":{[^}]*}" | grep -o "[^:]*$" | tr -d '",')
fi

export JAVA_OPTS="${JAVA_OPTS} -Xshare:off"

exec java ${JAVA_OPTS} \
    "-Djava.security.egd=file:/dev/./urandom" \
    "-Dspring.config=${LOG_CONFIG_FILE}" \
    "-Dorg.springframework.boot.loader.launch.JarLauncher" \
    "$@"