Serverless Kotlin Kafka
-----------------------

## ws_to_kafka

Dev (TestContainer Kafka):
```
./gradlew :ws_to_kafka:bootRun
```

Prod (External Kafka):
```
export BOOTSTRAP_SERVERS=YOUR_BOOTSTRAP_SERVEERS
export SASL_JAAS_CONFIG_PROPERTY_FORMAT="org.apache.kafka.common.security.plain.PlainLoginModule required username='YOUR_KEY' password='YOUR_SECRET';"

./gradlew :ws_to_kafka:run
```
