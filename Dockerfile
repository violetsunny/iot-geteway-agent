FROM ecr-online.enncloud.cn/base/centos_jdk_elk:v1-noapm

MAINTAINER admin

RUN mkdir -p /enn/
WORKDIR /enn/

EXPOSE 8868

ADD  /agent/target/iot-gateway-agent.jar ./iot-gateway-agent.jar

ENV JAVA_OPTS "-Dspring.cloud.nacos.config.server-addr=http://10.39.82.93:8848 -Dspring.cloud.nacos.config.namespace=enn-prod-public -Dspring.cloud.nacos.config.file-extension=yaml -Dspring.application.name=iot-gateway-agent"
ENV APP_ID iot-gateway-agent
ENV SERVER_PORT 8868

ENTRYPOINT exec java -Xmx1g -Xms1g $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar iot-gateway-agent.jar --spring.profiles.active=prod
