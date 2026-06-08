#!/bin/bash
# Download OpenTelemetry Java agent if not already present
AGENT_VERSION="2.2.0"
AGENT_DIR="$HOME/.otel-agent"
AGENT_JAR="$AGENT_DIR/opentelemetry-javaagent-$AGENT_VERSION.jar"

if [ ! -f "$AGENT_JAR" ]; then
    echo "Downloading OpenTelemetry Java agent $AGENT_VERSION..."
    mkdir -p "$AGENT_DIR"
    curl -L -o "$AGENT_JAR" \
        "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v$AGENT_VERSION/opentelemetry-javaagent.jar"
    echo "Agent downloaded to $AGENT_JAR"
fi

# Find the executable JAR (not the -plain.jar)
JAR_FILE=$(find build/libs -name "sdui-server-*.jar" ! -name "*-plain.jar" | head -1)

if [ -z "$JAR_FILE" ]; then
    echo "Error: Could not find executable JAR file. Run './gradlew build' first."
    exit 1
fi

echo "Starting server with JAR: $JAR_FILE"

# Run server with the agent
exec java -javaagent:"$AGENT_JAR" \
    -Dotel.service.name=sdui-server \
    -Dotel.exporter.otlp.protocol=http/protobuf \
    -Dotel.exporter.otlp.endpoint=http://localhost:4318 \
    -Dotel.traces.exporter=otlp \
    -Dotel.metrics.exporter=none \
    -Dotel.logs.exporter=none \
    -jar "$JAR_FILE"
