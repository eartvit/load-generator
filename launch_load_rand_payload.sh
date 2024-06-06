#!/bin/bash

# For testing purposes
# Ensure you have a podman network called `test` and that has DNS enabled. You can create one with `podman network create test`
# Then deploy the wiremock-metrics application as: `podman run -d --name wiremock-metrics2 --net test -p 8080:8080 quay.io/avitui/wiremock-metrics:v1`
# Then build the app as container in your local store using `podman build -t load-generator -f Containerfile`.
# Once the build is completed you can use the launcher script.

podman rm load-generator > /dev/null 2>&1

podman run -d --name load-generator --net test -p 9092:9090 \
            -e TRACEACTIVE='True' -e CONNECTIONS=2 -e DURATION=5 \
            -e ENDPOINT=http://wiremock-metrics2:8080/mock -e PROMETHEUSPORT=9090 \
            -e OUTPUT='json' -e THREADSLEEPMS=50 -e STOPONERROR='False' -e RANDREQMODE='True' -e RANDPAYLOAD='True' \
            -e PAYLOADSIZES='50,150,255' -e LTREQPAYLOADSIZEFACTOR=10 -e LTREQFIRSTSIZEONLY='False' \
            localhost/load-generator:latest