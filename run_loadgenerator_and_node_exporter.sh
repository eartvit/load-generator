#!/bin/bash
./node_exporter >/dev/null 2>&1 &
java -Xms512m -Xmx3584m -Xss64m -jar load-generator-1.0-SNAPSHOT-jar-with-dependencies.jar


