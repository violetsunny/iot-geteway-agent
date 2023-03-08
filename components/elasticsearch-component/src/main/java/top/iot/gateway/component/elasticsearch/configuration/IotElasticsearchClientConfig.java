///**
// * llkang.com Inc.
// * Copyright (c) 2010-2023 All Rights Reserved.
// */
//package top.iot.gateway.component.elasticsearch.configuration;
//
//import org.jetbrains.annotations.NotNull;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.elasticsearch.client.ClientConfiguration;
//import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchConfiguration;
//
//import java.time.Duration;
//
///**
// * es配置
// *
// * @author kanglele
// * @version $Id: IotClientConfig, v 0.1 2023/1/11 17:40 kanglele Exp $
// */
//@Configuration
//public class IotElasticsearchClientConfig extends ReactiveElasticsearchConfiguration {
//
//    @Value("${elasticsearch.client.host:127.0.0.1}")
//    private String host;
//    @Value("${elasticsearch.client.port:9200}")
//    private String port;
//
//    @NotNull
//    @Override
//    public ClientConfiguration clientConfiguration() {
//        return ClientConfiguration.builder()
//                .connectedTo(host+":"+port)
//                .withConnectTimeout(Duration.ofSeconds(5))
//                .withSocketTimeout(Duration.ofSeconds(3))
//                .build();
//    }
//}
