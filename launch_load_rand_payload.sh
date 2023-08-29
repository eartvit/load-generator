#!/bin/bash

# For testing purposes
# Ensure you have a podman network called `test` and that has DNS enabled. You can create one with `podman network create test`
# Then deploy the wiremock-mlasp application as: `podman run -d --name wiremock-mlasp --net test -p 8080:8080 quay.io/avitui/wire_mock_mlasp_ocp:v1.1`
# Then build the app as container in your local store using `podman build -t load-generaror -f Containerfile`.
# Once the build is completed you can use the launcher script.

podman rm load-generator > /dev/null 2>&1

podman run -d --name load-generator --net test -e TRACEACTIVE='True' -e CONNECTIONS=2 -e DURATION=5 \
            -e ENDPOINT=http://wiremock-metrics2:8080/mock \
            -e OUTPUT='json' -e THREADSLEEPMS=50 -e STOPONERROR='False' -e RANDREQMODE='True' -e RANDPAYLOAD='True' \
            -e REQPAYLOADS=3 -e PAYLOADSIZE1=50 -e PAYLOADSIZE2=150 -e PAYLOADSIZE3=255 \
            localhost/load-generator:latest