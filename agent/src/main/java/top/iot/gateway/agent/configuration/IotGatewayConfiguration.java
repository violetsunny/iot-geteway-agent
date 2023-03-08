package top.iot.gateway.agent.configuration;

import top.iot.gateway.agent.configuration.fst.FSTMessageCodec;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.guava.CaffeinatedGuava;
import top.iot.gateway.manager.message.DeviceMessageConnector;
import top.iot.gateway.network.SessionIdDeviceIdBindingRegistry;
import io.scalecube.cluster.ClusterConfig;
import io.scalecube.net.Address;
import io.scalecube.transport.netty.tcp.TcpTransportFactory;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.token.UserTokenManager;
import org.hswebframework.web.authorization.token.redis.RedisUserTokenManager;
import top.iot.gateway.core.ProtocolSupports;
import top.iot.gateway.core.cluster.ClusterManager;
import top.iot.gateway.core.cluster.ServerNode;
import top.iot.gateway.core.config.ConfigStorageManager;
import top.iot.gateway.core.device.*;
import top.iot.gateway.core.device.session.DeviceSessionManager;
import top.iot.gateway.core.event.EventBus;
import top.iot.gateway.core.message.interceptor.DeviceMessageSenderInterceptor;
import top.iot.gateway.core.server.MessageHandler;
import top.iot.gateway.supports.cluster.ClusterDeviceRegistry;
import top.iot.gateway.supports.cluster.EventBusDeviceOperationBroker;
import top.iot.gateway.supports.cluster.redis.RedisClusterManager;
import top.iot.gateway.supports.config.EventBusStorageManager;
import top.iot.gateway.supports.device.session.LocalDeviceSessionManager;
import top.iot.gateway.supports.event.BrokerEventBus;
import top.iot.gateway.supports.event.EventBroker;
import top.iot.gateway.supports.scalecube.ExtendedCluster;
import top.iot.gateway.supports.scalecube.ExtendedClusterImpl;
import top.iot.gateway.supports.scalecube.event.ScalecubeEventBusBroker;
import top.iot.gateway.supports.server.ClusterSendToDeviceMessageHandler;
import top.iot.gateway.supports.server.DecodedClientMessageHandler;
import org.nustaq.serialization.FSTConfiguration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties({IotGatewayProperties.class,ClusterProperties.class})
@Slf4j
public class IotGatewayConfiguration {

    @Bean
    public WebFluxConfigurer corsConfigurer() {
        return new WebFluxConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS").allowCredentials(true).allowedOriginPatterns("*");
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/upload/**").addResourceLocations("file:D:/data/sharefile/");
            }
        };
    }

//    @Bean
//    public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> webServerFactoryWebServerFactoryCustomizer() {
//        //解决请求参数最大长度问题
//        return factory -> factory.addServerCustomizers(httpServer ->
//            httpServer.httpRequestDecoder(spec -> {
//                spec.maxInitialLineLength(10240);
//                return spec;
//            }));
//    }
//
//    @Bean
//    @ConfigurationProperties(prefix = "vertx")
//    public VertxOptions vertxOptions() {
//        return new VertxOptions();
//    }
//
//    @Bean
//    public Vertx vertx(VertxOptions vertxOptions) {
//        return Vertx.vertx(vertxOptions);
//    }
//
//    @Bean
//    public RedisClusterEventBroker eventBroker(ClusterManager clusterManager, ReactiveRedisConnectionFactory redisConnectionFactory) {
//        return new RedisClusterEventBroker(clusterManager, redisConnectionFactory);
//    }
//
//    @Bean
//    public EventBus eventBus(EventBroker eventBroker) {
//        BrokerEventBus eventBus = new BrokerEventBus();
//        eventBus.addBroker(eventBroker);
//        return eventBus;
//    }
//
////    @Bean
////    public StandaloneDeviceMessageBroker standaloneDeviceMessageBroker() {
////        return new StandaloneDeviceMessageBroker();
////    }
//
//    @Bean
//    public EventBusDeviceOperationBroker eventBusDeviceOperationBroker(NyIotProperties properties, EventBus eventBus) {
//        return new EventBusDeviceOperationBroker(properties.getServerId(), eventBus);
//    }
//
//    @Bean
//    public EventBusStorageManager eventBusStorageManager(ClusterManager clusterManager, EventBus eventBus) {
//        return new EventBusStorageManager(clusterManager,
//                                          eventBus,
//                                          () -> CaffeinatedGuava.build(Caffeine.newBuilder()));
//    }
//
//    @Bean(initMethod = "startup")
//    public RedisClusterManager clusterManager(NyIotProperties properties, ReactiveRedisTemplate<Object, Object> template) {
//        return new RedisClusterManager(properties.getClusterName(),
//                ServerNode.builder().
//                        id(properties.getServerId()).name(properties.getServerId()).tags(properties.getTags()).build(), template);
//    }
//
//    @Bean
//    public ClusterDeviceRegistry clusterDeviceRegistry(ProtocolSupports supports,
//                                                       ClusterManager manager,
//                                                       ConfigStorageManager storageManager,
//                                                       DeviceOperationBroker handler) {
//
//        return new ClusterDeviceRegistry(supports,
//                                         storageManager,
//                                         manager,
//                                         handler,
//                                         CaffeinatedGuava.build(Caffeine.newBuilder()));
//    }
//
//    @Bean
//    public GatewayServerMonitor gatewayServerMonitor(NyIotProperties properties){
////        GatewayServerMetrics metrics = new MicrometerGatewayServerMetrics(properties.getServerId(),
////                registry.getMeterRegister(DeviceTimeSeriesMetric
////                        .deviceMetrics().getId()));
//
//        return new GatewayServerMonitor() {
//            @Override
//            public String getCurrentServerId() {
//                return properties.getServerId();
//            }
//
//            @Override
//            public GatewayServerMetrics metrics() {
//                return null;
//            }
//        };
//    }
//
//    @Bean(initMethod = "init", destroyMethod = "shutdown")
//    public DefaultDeviceSessionManager deviceSessionManager(NyIotProperties properties,
//                                                            GatewayServerMonitor monitor,
//                                                            DeviceRegistry registry) {
//        DefaultDeviceSessionManager sessionManager = new DefaultDeviceSessionManager();
//        sessionManager.setGatewayServerMonitor(monitor);
//        sessionManager.setRegistry(registry);
//        Optional.ofNullable(properties.getTransportLimit()).ifPresent(sessionManager::setTransportLimits);
//
//        return sessionManager;
//    }
//
//
//    @Bean
//    public BeanPostProcessor interceptorRegister(ClusterDeviceRegistry registry) {
//        return new BeanPostProcessor() {
//            @Override
//            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
//                if (bean instanceof DeviceMessageSenderInterceptor) {
//                    registry.addInterceptor(((DeviceMessageSenderInterceptor) bean));
//                }
//                return bean;
//            }
//        };
//    }
//
//    @Bean
//    @ConfigurationProperties(prefix = "hsweb.user-token")
//    public UserTokenManager userTokenManager(ReactiveRedisOperations<Object, Object> template) {
//        return new RedisUserTokenManager(template);
//    }
//
////    @Bean
////    public DecodedClientMessageHandler defaultDecodedClientMessageHandler() {
////        return new DecodedClientMessageHandler() {
////            @Override
//
////            public Mono<Boolean> handleMessage(@Nullable DeviceOperator deviceOperator, @Nonnull Message message) {
////                System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
////                return Mono.just(true);
////            }
////        };
////    }
//
//    @Bean
//    public DeviceMessageConnector deviceMessageConnector(EventBus eventBus,
//                                                         MessageHandler messageHandler,
//                                                         DeviceSessionManager sessionManager,
//                                                         DeviceRegistry registry,
//                                                         RabbitTemplate rabbitTemplate) {
//        return new DeviceMessageConnector(eventBus, registry, messageHandler, sessionManager, rabbitTemplate);
//    }
//
//    @Bean
//    public SessionIdDeviceIdBindingRegistry sessionIdDeviceIdBindingRegistry(ConfigStorageManager manager) {
//        return new SessionIdDeviceIdBindingRegistry(manager);
//    }
//
//    @Bean(initMethod = "startup")
//    public DefaultSendToDeviceMessageHandler defaultSendToDeviceMessageHandler(NyIotProperties properties,
//                                                                               DeviceSessionManager sessionManager,
//                                                                               DeviceRegistry registry,
//                                                                               MessageHandler messageHandler,
//                                                                               DecodedClientMessageHandler clientMessageHandler) {
//        return new DefaultSendToDeviceMessageHandler(properties.getServerId(), sessionManager, messageHandler, registry, clientMessageHandler);
//    }

    @Bean
    public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> webServerFactoryWebServerFactoryCustomizer() {
        //解决请求参数最大长度问题
        return factory -> factory.addServerCustomizers(httpServer ->
                httpServer.httpRequestDecoder(spec -> {
                    spec.maxInitialLineLength(10240);
                    return spec;
                }));
    }

    @Bean
    @ConfigurationProperties(prefix = "vertx")
    public VertxOptions vertxOptions() {
        return new VertxOptions();
    }

    @Bean
    public Vertx vertx(VertxOptions vertxOptions) {
        return Vertx.vertx(vertxOptions);
    }

    @Bean
    @ConfigurationProperties(prefix = "hsweb.user-token")
    public UserTokenManager userTokenManager(ReactiveRedisOperations<Object, Object> template) {
        return new RedisUserTokenManager(template);
    }

    @Bean
    public DeviceMessageConnector deviceMessageConnector(EventBus eventBus,
                                                         MessageHandler messageHandler,
                                                         DeviceSessionManager sessionManager,
                                                         DeviceRegistry registry) {
        return new DeviceMessageConnector(eventBus, registry, messageHandler, sessionManager);
    }

    @Bean
    public SessionIdDeviceIdBindingRegistry sessionIdDeviceIdBindingRegistry(ConfigStorageManager manager) {
        return new SessionIdDeviceIdBindingRegistry(manager);
    }


    @Bean
    public ExtendedClusterImpl cluster(ClusterProperties properties, ResourceLoader resourceLoader) {
        FSTMessageCodec codec = new FSTMessageCodec(() -> {
            FSTConfiguration configuration = FSTConfiguration
                    .createDefaultConfiguration()
                    .setForceSerializable(true);

            configuration.setClassLoader(resourceLoader.getClassLoader());
            return configuration;
        });

        ExtendedClusterImpl impl = new ExtendedClusterImpl(
                new ClusterConfig()
                        .transport(conf -> conf
                                .port(properties.getPort())
                                .messageCodec(codec)
                                .transportFactory(new TcpTransportFactory()))
                        .memberAlias(properties.getId())
                        .externalHost(properties.getExternalHost())
                        .externalPort(properties.getExternalPort())
                        .membership(conf -> conf
                                .seedMembers(properties
                                        .getSeeds()
                                        .stream()
                                        .map(Address::from)
                                        .collect(Collectors.toList()))
                        )
        );
        impl.startAwait();
        return impl;
    }

    @Bean
    public EventBroker eventBroker(ExtendedCluster cluster) {
        return new ScalecubeEventBusBroker(cluster);
    }

//    @Bean
//    public RedisClusterEventBroker eventBroker(ClusterManager clusterManager, ReactiveRedisConnectionFactory redisConnectionFactory) {
//        return new RedisClusterEventBroker(clusterManager, redisConnectionFactory);
//    }

    @Bean
    public BrokerEventBus eventBus(ObjectProvider<EventBroker> provider,
                                   ObjectProvider<Scheduler> scheduler) {

        BrokerEventBus eventBus = new BrokerEventBus();
        eventBus.setPublishScheduler(scheduler.getIfAvailable(Schedulers::parallel));
        for (EventBroker eventBroker : provider) {
            eventBus.addBroker(eventBroker);
        }

        return eventBus;
    }

    @Bean
    public EventBusStorageManager eventBusStorageManager(ClusterManager clusterManager, EventBus eventBus) {
        return new EventBusStorageManager(clusterManager,
                eventBus,
                -1);
    }

//    @Bean(initMethod = "startup")
//    public RedisClusterManager clusterManager(ClusterProperties properties, ReactiveRedisTemplate<Object, Object> template) {
//        return new RedisClusterManager(properties.getName(),
//                ServerNode.builder().
//                        id(properties.getId()).name(properties.getId()).tags(properties.getTags()).build(), template);
//    }

    @Bean(initMethod = "startup")
    public RedisClusterManager clusterManager(IotGatewayProperties properties, ReactiveRedisTemplate<Object, Object> template) {
        return new RedisClusterManager(properties.getClusterName(),
                ServerNode.builder().
                        id(properties.getServerId()).name(properties.getServerId()).tags(properties.getTags()).build(), template);
    }

//    @Bean(initMethod = "startAwait", destroyMethod = "stopAwait")
//    public ScalecubeRpcManager rpcManager(ExtendedCluster cluster, ClusterProperties properties) {
//        return new ScalecubeRpcManager(cluster,
//                () -> new RSocketServiceTransport()
//                        .serverTransportFactory(RSocketServerTransportFactory.tcp(properties.getRpcPort()))
//                        .clientTransportFactory(RSocketClientTransportFactory.tcp()))
//                .externalHost(properties.getRpcExternalHost())
//                .externalPort(properties.getRpcExternalPort());
//    }




    @Bean
    public ClusterDeviceRegistry deviceRegistry(ProtocolSupports supports,
                                                ClusterManager manager,
                                                ConfigStorageManager storageManager,
                                                DeviceOperationBroker handler) {

        return new ClusterDeviceRegistry(supports,
                storageManager,
                manager,
                handler,
                CaffeinatedGuava.build(Caffeine.newBuilder()));
    }


    @Bean
    @ConditionalOnBean(ClusterDeviceRegistry.class)
    public BeanPostProcessor interceptorRegister(ClusterDeviceRegistry registry) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof DeviceMessageSenderInterceptor) {
                    registry.addInterceptor(((DeviceMessageSenderInterceptor) bean));
                }
                if (bean instanceof DeviceStateChecker) {
                    registry.addStateChecker(((DeviceStateChecker) bean));
                }
                return bean;
            }
        };
    }

//    @Bean(initMethod = "init", destroyMethod = "shutdown")
//    @ConditionalOnBean(RpcManager.class)
//    public PersistenceDeviceSessionManager deviceSessionManager(RpcManager rpcManager) {
//
//        return new PersistenceDeviceSessionManager(rpcManager);
//    }

    @Bean(initMethod = "init", destroyMethod = "shutdown")
    public LocalDeviceSessionManager deviceSessionManager() {

        return new LocalDeviceSessionManager();
    }


    @ConditionalOnBean(DecodedClientMessageHandler.class)
    @Bean
    public ClusterSendToDeviceMessageHandler defaultSendToDeviceMessageHandler(DeviceSessionManager sessionManager,
                                                                               DeviceRegistry registry,
                                                                               MessageHandler messageHandler,
                                                                               DecodedClientMessageHandler clientMessageHandler) {
        return new ClusterSendToDeviceMessageHandler(sessionManager, messageHandler, registry, clientMessageHandler);
    }


//    @Bean
//    public ClusterDeviceOperationBroker clusterDeviceOperationBroker(ExtendedCluster cluster,
//                                                                     DeviceSessionManager sessionManager) {
//        return new ClusterDeviceOperationBroker(cluster, sessionManager);
//    }

    @Bean
    public EventBusDeviceOperationBroker eventBusDeviceOperationBroker(IotGatewayProperties properties, EventBus eventBus) {
        return new EventBusDeviceOperationBroker(properties.getServerId(), eventBus);
    }

}
