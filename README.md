Serverless Kotlin Kafka
-----------------------

## ws-to-kafka

Dev (TestContainer Kafka):
```
./gradlew :ws-to-kafka:bootRun
```

Prod (External Kafka):
```
export BOOTSTRAP_SERVERS=YOUR_BOOTSTRAP_SERVEERS
export SASL_JAAS_CONFIG_PROPERTY_FORMAT="org.apache.kafka.common.security.plain.PlainLoginModule required username='YOUR_KEY' password='YOUR_SECRET';"

./gradlew :ws-to-kafka:run
```

## todo

- schema stuff
- parse tags
- ksqldb stateful (total favorites)
- ksql transform (language groupings)
- web event consumer (websocket stream + pull query)
