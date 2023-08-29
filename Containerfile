FROM registry.access.redhat.com/ubi8/openjdk-17-runtime

COPY ./target/load-generator-1.0-SNAPSHOT-jar-with-dependencies.jar load-generator-1.0-SNAPSHOT-jar-with-dependencies.jar

# Define ENV defaults
# Turn off app tracing
ENV TRACEACTIVE="False"
# Number of concurrent connections
ENV CONNECTIONS=5
# Load test duration in seconds
ENV DURATION=30 
# Load testing target endpoint URL
ENV ENDPOINT="http://wiremock.demo.apps.svc.local:8080/mock"
# Output report format: silent, text or json
ENV OUTPUT="json"
# Stop or not on errors
ENV STOPONERROR="True"
# Thread sleep ms
ENV THREADSLEEPMS=50
# Random request mode, meaning that the request payloads will be randomly selected from the pool, instead of in cyclic fashion
ENV RANDREQMODE="True"
# Random payload. If not each payload must be provided as JSON value, e.g. ENV PAYLOAD[n]="{Content: Message}"
ENV RANDPAYLOAD="True"
# Number of request payloads
ENV REQPAYLOADS=3
# Provide either the payload sizes or actual payloads up to REQPAYLOADS based on the value of the RANDPAYLOAD field. 
# If RANDPAYLOAD=True then PAYLOADSIZE[n] must be provided in char (bytes)
ENV PAYLOADSIZE1=50
ENV PAYLOADSIZE2=150
ENV PAYLOADSIZE3=250

# Provide header for the HttpRequest towards the target endpoint.
ENV HEADERS="{'Authorization': 'Bearer YourAccessToken'}"

CMD java -jar load-generator-1.0-SNAPSHOT-jar-with-dependencies.jar