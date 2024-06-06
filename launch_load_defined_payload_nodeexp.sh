#!/bin/bash

# For testing purposes
# Ensure you have a podman network called `test` and that has DNS enabled. You can create one with `podman network create test`
# Then deploy the wiremock-metrics application as: `podman run -d --name wiremock-metrics-nodeexporter --net test -p 8080:8080 quay.io/avitui/wiremock-metrics-nodeexporter:v1`
# Then build the app as container in your local store using `podman build -t load-generator-nodeexp -f Containerfile.node_exporter`.
# Once the build is completed you can use the launcher script.

podman rm load-generator-nodeexp > /dev/null 2>&1

podman run -d --name load-generator-nodeexp --net test -p 9092:9090 -p 9102:9100 \
            -e TRACEACTIVE='True' -e CONNECTIONS=2 -e DURATION=5 \
            -e ENDPOINT=http://wiremock-metrics-nodeexporter:8080/mock -e PROMETHEUSPORT=9090 \
            -e OUTPUT='json' -e THREADSLEEPMS=50 -e STOPONERROR='False' -e RANDREQMODE='True' -e RANDPAYLOAD='False' \
            -e REQPAYLOADS=3 -e PAYLOAD1="{'content':'50'}" -e PAYLOAD2="{'content':'150'}" -e PAYLOAD3="{'content':'255'}" \
            localhost/load-generator-nodeexp:latest