#!/bin/bash
JMX_EXPORTER=jmx_prometheus_javaagent-0.18.0.jar
PORT=12345
JMX_CONFIG=jmx-config.yaml
if [ -z "${1}" ]
then
  read -p 'Your fat JAR name: ' JAR
else
  JAR=$1
fi
if [ -f $JMX_EXPORTER ]
then
  echo "Found jmx_exporter"
else
  echo "Downloading jmx_exporter"
  wget https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.18.0/$JMX_EXPORTER
fi
if [ -z "${2}" ]
then
  java -javaagent:./$JMX_EXPORTER=$PORT:$JMX_CONFIG -jar $JAR &
  docker-compose up
else
  java -javaagent:./$JMX_EXPORTER=$PORT:$JMX_CONFIG -jar $JAR -cp $2 &
  docker-compose up
fi
