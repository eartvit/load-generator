# Load-Generator
HTTP POST load generator application. It can generate random payload or use predefined payload. It can use several payloads during the same load test, randomly or sequantially selected from a payload pool.

The container checks for the following environment parameters (defaults provided below):
* Turn off app tracing: ENV TRACEACTIVE="False"
* Number of concurrent connections: ENV CONNECTIONS=5
* Load test duration in seconds: ENV DURATION=30
* Load testing target endpoint URL: ENV ENDPOINT="http://wiremock.demo.apps.svc.local:8080/mock"
* Output report format: silent, text or json: ENV OUTPUT="json"
* Stop or not on errors: ENV STOPONERROR="True"
* Thread sleep ms: ENV THREADSLEEPMS=50
* Random request mode, meaning that the request payloads will be randomly selected from the pool, instead of in cyclic fashion: ENV RANDREQMODE="True"
* Random payload. If not each payload must be provided as JSON value, e.g. ENV PAYLOAD[n]="{Content: Message}": ENV RANDPAYLOAD="True"
* If RANDOMPAYLOAD=True then LTREQPAYLOADSIZEFACTOR defines the ratio between the sent payload and the one expected to be received from the PAYLOADSIZES list. ENV LTREQPAYLOADSIZEFACTOR=10
* If RANDOMPAYLOAD=True then LTREQFIRSTSIZEONLY defines if only one payload size to be used in the sent request for the list of expected PAYLOADSIZES to receive. ENV LTREQFIRSTSIZEONLY="False"
* If random payload generation flag is True then provide the list of payload sizes: ENV PAYLOADSIZES='50,150,255'
* Otherwise provide how many payloads ENV REQPAYLOADS=3
* And then match up the manually provided payloads: ENV PAYLOAD1="{Content: Message one}" \ ENV PAYLOAD2="{Content: Message two}" \ ENV PAYLOAD3="{Content: Message three}"
* Provide header for the HttpRequest towards the target endpoint: ENV HEADERS="{'Authorization': 'Bearer YourAccessToken'}"


A containerized version is available in [quay.io](https://quay.io/repository/avitui/load-generator)
