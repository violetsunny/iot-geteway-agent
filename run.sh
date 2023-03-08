#!/usr/bin/env bash

./mvnw clean package -Dmaven.test.skip=true -Dmaven.build.timestamp="$(date "+%Y-%m-%d %H:%M:%S")"
if [ $? -ne 0 ];then
    echo "构建失败!"
else
   java -jar "$(pwd)/iot-gateway-agent/target/iot-gateway-agent.jar"
fi