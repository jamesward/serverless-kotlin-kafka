#!/usr/bin/env bash

# Kafka
CLUSTER_ID=$(ccloud kafka cluster list -o json | jq -r '.[0].id')
export CLUSTER_ID

ccloud kafka cluster use "$CLUSTER_ID"

KAFKA_USER_PASS=$(ccloud api-key create --resource "${CLUSTER_ID}" -ojson)
KAFKA_USERNAME=$(echo "$KAFKA_USER_PASS" | jq -r '.key')
KAFKA_PASSWORD=$(echo "$KAFKA_USER_PASS" | jq -r '.secret')

export KAFKA_USERNAME KAFKA_PASSWORD
ccloud api-key use "$KAFKA_USERNAME" --resource "$CLUSTER_ID"

KAFKA_BOOTSTRAP_SERVERS=$(ccloud kafka cluster describe "$CLUSTER_ID" -ojson | jq -r '.endpoint')
export KAFKA_BOOTSTRAP_SERVERS

# Schema Registry
SR_CLUSTER=$(ccloud schema-registry cluster enable --cloud gcp --geo us -ojson)
SCHEMA_REGISTRY_ID=$(echo "$SR_CLUSTER" | jq -r '.id')
SCHEMA_REGISTRY_URL=$(echo "$SR_CLUSTER" | jq -r '.endpoint_url')

export SCHEMA_REGISTRY_ID SCHEMA_REGISTRY_URL

SR_USER_PASS=$(ccloud api-key create --resource "$SCHEMA_REGISTRY_ID" -ojson)
SCHEMA_REGISTRY_KEY=$(echo "$SR_USER_PASS" | jq -r '.key')
SCHEMA_REGISTRY_PASSWORD=$(echo "$SR_USER_PASS" | jq -r '.secret')

export SCHEMA_REGISTRY_KEY SCHEMA_REGISTRY_PASSWORD

# ksqDB
KSQLDB_APPS=$(ccloud ksql app list -ojson )
KSQLDB_ID=$(echo "$KSQLDB_APPS" |  jq -r '.[0].id')
KSQLDB_ENDPOINT=$(echo "$KSQLDB_APPS" |  jq -r '.[0].endpoint')

export KSQLDB_ID KSQLDB_ENDPOINT

ccloud ksql app configure-acls "$KSQLDB_ID" mytopic

KSQLDB_KEY_PASS=$(ccloud api-key create --resource "$KSQLDB_ID" -o json)
KSQLDB_USERNAME=$(echo "$KSQLDB_KEY_PASS" | jq -r '.key')
KSQLDB_PASSWORD=$(echo "$KSQLDB_KEY_PASS" | jq -r '.secret')

export KSQLDB_USERNAME KSQLDB_PASSWORD

