#!/bin/bash
./node_exporter &
java -Xms512m -Xmx3584m -Xss64m -jar load-generator-1.0-SNAPSHOT-jar-with-dependencies.jar


