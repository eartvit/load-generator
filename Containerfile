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
# If RANDPAYLOAD=True then PAYLOADSIZES must be provided as comma separated values
ENV PAYLOADSIZES="50,150,250"
# Otherwise the payloads must be provided as JSON strings, e.g., ENV PAYLOAD[n]="{Content: Message}"
# ENV PAYLOAD1="{Content: Message one}"
# ENV PAYLOAD2="{Content: Message two}"
# ENV PAYLOAD3="{Content: Message three}"
# Number of request payloads must be provided if RANDPAYLOAD='False'
# ENV REQPAYLOADS=3

# Provide additional headers if needed for the HttpRequest towards the target endpoint.
ENV HEADERS="{'Authorization': 'Bearer YourAccessToken'}"

CMD java -jar load-generator-1.0-SNAPSHOT-jar-with-dependencies.jar