#!/bin/sh

# SET ENVIRONMENT VARIABLES
VERSION_FILE='/app/git.properties'
if test -f $VERSION_FILE; then
  export APP_VERSION=`sed -n 's/git.build.version=//p' $VERSION_FILE`
else
  export APP_VERSION="NOT SET"
fi

# Get the ECS Container ID and Cluster ID values from the ECS metadata.
if [ -z "$ECS_CONTAINER_METADATA_URI" ]; then
  ECS_CONTAINER_METADATA=$(curl --connect-timeout 3 --max-time 5 "$ECS_CONTAINER_METADATA_URI")
  export CONTAINER_ID=$(echo "$ECS_CONTAINER_METADATA" | grep -o "amazonaws.ecs.task-arn[^,]*" | grep -o "[^/]*$" | awk '{split($0, a, ":"); print a[3]}' | grep -o "[^-]*")
  export CLUSTER_ID=$(echo "$ECS_CONTAINER_METADATA" | grep -o "Labels[^}]*" | grep -o "amazonaws.ecs.Cluster[^,]*" | grep -o "[^:]*$" | grep -o "[^-]*")
fi

export JAVA_OPTS="${JAVA_OPTS} -Xshare:off"

exec java ${JAVA_OPTS} \
  -Djava.security.egd=file:/dev/./urandom \
  -Djava.config=$LOG_CONFIG_FILE \
  -jar \
  "org.springframework.boot.loader.LaunchJarLauncher"
