# Armeria OpenTelemetry Instrumentation + New Relic Java Agent
This project includes a blog server and a client for making requests to the server, both of which are built using the [Armeria framework](https://armeria.dev/). 

Both the client and server are instrumented by the [OpenTelemetry Armeria standalone instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/armeria/armeria-1.3).

The New Relic Java agent is also included, however it does not instrument Armeria.

The purpose of this project is to test a [prototype](https://github.com/newrelic/newrelic-java-agent/pull/1886) of the New Relic Java agent that can detect emitted OTel Spans and incorporate them into New Relic traces. 

## Requirements
* New Relic Java agent built from the [saxon/replace-otel-spans2](https://github.com/newrelic/newrelic-java-agent/tree/saxon/replace-otel-spans2) branch.
* Java 17+

## Usage
### Server
#### Config
At a minimum the following system properties should be added when running the server to add the New Relic Java agent and enable it to detect OTel spans:
```
-javaagent:/path/to/newrelic/newrelic.jar
-Dnewrelic.config.log_file_name=armeria-server.log
-Dnewrelic.config.app_name=armeria-server
-Dopentelemetry.sdk.spans.enabled=true
-Dopentelemetry.sdk.autoconfigure.enabled=true
```

The following environment variables can also be added to dump some OTel logs:
```
OTEL_LOGS_EXPORTER=logging
OTEL_METRIC_EXPORT_INTERVAL=15000
OTEL_METRICS_EXPORTER=logging
OTEL_SERVICE_NAME=armeria-server
OTEL_TRACES_EXPORTER=logging
```

If you want to disable all the instrumentation loaded by the New Relic Java agent and only let the OTel spans be instrumented, then add the following system properties. 
```
-Dnewrelic.config.class_transformer.com.newrelic.instrumentation.java.completable-future-jdk11.enabled=false
-Dnewrelic.config.class_transformer.com.newrelic.instrumentation.java.process.enabled=false
-Dnewrelic.config.class_transformer.com.newrelic.instrumentation.netty-4.1.16.enabled=false
-Dnewrelic.config.class_transformer.com.newrelic.instrumentation.jdbc-socket.enabled=false
-Dnewrelic.config.class_transformer.com.newrelic.instrumentation.java.logging-jdk8.enabled=false
-Dnewrelic.config.class_transformer.com.newrelic.instrumentation.httpurlconnection.enabled=false
```

#### Usage
Run `example.armeria.blog.server.Main` to start the server. It will serve the DocService that documents the service at the following:

http://127.0.0.1:8080/docs  
http://127.0.0.1:8443/docs  

### Client
#### Config
At a minimum the following system properties should be added when running the client to add the New Relic Java agent and enable it to detect OTel spans:
```
-javaagent:/path/to/newrelic/newrelic.jar
-Dnewrelic.config.log_file_name=armeria-client.log
-Dnewrelic.config.app_name=armeria-client
-Dopentelemetry.sdk.spans.enabled=true
-Dopentelemetry.sdk.autoconfigure.enabled=true
```

The following environment variables can also be added to dump some OTel logs:
```
OTEL_LOGS_EXPORTER=logging
OTEL_METRIC_EXPORT_INTERVAL=15000
OTEL_METRICS_EXPORTER=logging
OTEL_SERVICE_NAME=armeria-client
OTEL_TRACES_EXPORTER=logging
```

If you want to disable all the instrumentation loaded by the New Relic Java agent and only let the OTel spans be instrumented, then add the following system properties.
```
-Dnewrelic.config.class_transformer.com.newrelic.instrumentation.java.completable-future-jdk11.enabled=false
-Dnewrelic.config.class_transformer.com.newrelic.instrumentation.java.process.enabled=false
-Dnewrelic.config.class_transformer.com.newrelic.instrumentation.netty-4.1.16.enabled=false
-Dnewrelic.config.class_transformer.com.newrelic.instrumentation.jdbc-socket.enabled=false
-Dnewrelic.config.class_transformer.com.newrelic.instrumentation.java.logging-jdk8.enabled=false
-Dnewrelic.config.class_transformer.com.newrelic.instrumentation.httpurlconnection.enabled=false
```

#### Usage
Run `example.armeria.blog.client.Main` to start the clients. It will create two clients that automatically makes CRUD requests to the following domains:

http://127.0.0.1:8080  
http://127.0.0.1:8443  


