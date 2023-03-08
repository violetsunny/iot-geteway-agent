package top.iot.protocol.godson;

import top.iot.protocol.godson.tcp.GodsonIotTcpDeviceMessageCodec;
import top.iot.protocol.godson.http.GodsonIotHttpDeviceMessageCodec;
import top.iot.protocol.godson.metadataMapping.ProtocolConfigRegistry;
import top.iot.protocol.godson.mqtt.GodsonIotMqttDeviceMessageCodec;
import top.iot.protocol.godson.udp.GodsonIotUdpDeviceMessageCodec;
import lombok.extern.slf4j.Slf4j;
import top.iot.gateway.core.Value;
import top.iot.gateway.core.defaults.Authenticator;
import top.iot.gateway.core.defaults.CompositeProtocolSupport;
import top.iot.gateway.core.device.*;
import top.iot.gateway.core.message.codec.DefaultTransport;
import top.iot.gateway.core.metadata.DefaultConfigMetadata;
import top.iot.gateway.core.metadata.DeviceConfigScope;
import top.iot.gateway.core.metadata.types.IntType;
import top.iot.gateway.core.metadata.types.PasswordType;
import top.iot.gateway.core.metadata.types.StringType;
import top.iot.gateway.core.spi.ProtocolSupportProvider;
import top.iot.gateway.core.spi.ServiceContext;
import top.iot.gateway.supports.official.IotGatewayDeviceMetadataCodec;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;


@Slf4j
public class GodsonIotProtocolSupportProvider implements ProtocolSupportProvider {

    private final ProtocolConfigRegistry registry;

    public GodsonIotProtocolSupportProvider(ProtocolConfigRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void dispose() {
        //协议卸载时执行
    }

    private static final DefaultConfigMetadata mqttConfig = new DefaultConfigMetadata(
        "MQTT认证配置"
        , "")
        .add("username", "username", "MQTT用户名", StringType.GLOBAL)
        .add("password", "password", "MQTT密码", PasswordType.GLOBAL);


    private static final DefaultConfigMetadata tcpConfig = new DefaultConfigMetadata(
        "TCP认证配置"
        , "")
        .add("tcp_auth_key", "key", "TCP认证KEY", StringType.GLOBAL);

    private static final DefaultConfigMetadata udpConfig = new DefaultConfigMetadata(
        "UDP认证配置"
        , "")
        .add("udp_auth_key", "key", "UDP认证KEY", StringType.GLOBAL);

    private static final DefaultConfigMetadata tcpClientConfig = new DefaultConfigMetadata(
        "远程服务配置"
        , "")
        .add("host", "host", "host", StringType.GLOBAL, DeviceConfigScope.product)//只需要产品配置
        .add("port", "port", "host", IntType.GLOBAL, DeviceConfigScope.product);//只需要产品配置

    private static final DefaultConfigMetadata httpConfig = new DefaultConfigMetadata(
        "HTTP认证配置"
        , "HTTP认证时需要的配置")
        .add("secureId", "secureId", "密钥ID", new StringType())
        .add("secureKey", "secureKey", "密钥KEY", new PasswordType());

    @Override
    public Mono<CompositeProtocolSupport> create(ServiceContext context) {
        log.info("GodsonIot协议支持");
        return Mono.defer(() -> {
            CompositeProtocolSupport support = new CompositeProtocolSupport();

            support.setId("GodsonIot");
            support.setName("GodsonIot v1(内置协议)");
            support.setDescription("Godson Protocol Version 1.0");

//            support.addAuthenticator(DefaultTransport.MQTT, new IotGatewayAuthenticator());
//            support.addAuthenticator(DefaultTransport.TCP, new IotGatewayAuthenticator());

            support.setMetadataCodec(new IotGatewayDeviceMetadataCodec());

            //TCP 演示协议
            {
                GodsonIotTcpDeviceMessageCodec codec = new GodsonIotTcpDeviceMessageCodec(registry);
                support.addMessageCodecSupport(DefaultTransport.TCP, () -> Mono.just(codec));
                support.addMessageCodecSupport(DefaultTransport.TCP_TLS, () -> Mono.just(codec));
                support.addConfigMetadata(DefaultTransport.TCP, tcpConfig);
                support.addConfigMetadata(DefaultTransport.TCP_TLS, tcpConfig);

            }

            {
                //UDP消息编解码器
                GodsonIotUdpDeviceMessageCodec codec = new GodsonIotUdpDeviceMessageCodec(registry);
                support.addMessageCodecSupport(DefaultTransport.UDP, () -> Mono.just(codec));
                support.addMessageCodecSupport(DefaultTransport.UDP_DTLS, () -> Mono.just(codec));

                support.addConfigMetadata(DefaultTransport.UDP, udpConfig);
                support.addConfigMetadata(DefaultTransport.UDP_DTLS, udpConfig);
            }

            {
                //MQTT消息编解码器
                GodsonIotMqttDeviceMessageCodec codec = new GodsonIotMqttDeviceMessageCodec(registry);
                support.addMessageCodecSupport(DefaultTransport.MQTT, () -> Mono.just(codec));
            }

            {
                //DeviceBindManager 支持绑定第三方平台信息
//            context.getService(DeviceBindManager.class)
//                   .ifPresent(bind->{
                //HTTP
                GodsonIotHttpDeviceMessageCodec codec = new GodsonIotHttpDeviceMessageCodec(registry);
                support.addMessageCodecSupport(DefaultTransport.HTTP, () -> Mono.just(codec));
//                   });
            }

//        {
//            //HTTP Client
//            Mono.zip(
//                Mono.justOrEmpty(context.getService(DeviceRegistry.class)),
//                Mono.justOrEmpty(context.getService(DeviceSessionManager.class)),
//                HttpClientDeviceMessageCodec::new
//            )
//                .subscribe(codec -> {
//
//                    //注册到协议
//                    //support.addMessageCodecSupport(DefaultTransport.HTTP, () -> Mono.just(codec));
//
//                });
//        }


            {
                //CoAP
                //临时注释
//            CoAPDeviceMessageCodec codec = new CoAPDeviceMessageCodec();
//            support.addMessageCodecSupport(DefaultTransport.CoAP, () -> Mono.just(codec));
            }

//            {
//                //WebSocket
//                GodsonIotWebsocketDeviceMessageCodec codec = new GodsonIotWebsocketDeviceMessageCodec(registry);
//                support.addMessageCodecSupport(DefaultTransport.WebSocket, () -> Mono.just(codec));
//            }

            //MQTT需要的配置信息
            support.addConfigMetadata(DefaultTransport.MQTT, mqttConfig);
            //MQTT认证策略
            support.addAuthenticator(DefaultTransport.MQTT, new Authenticator() {
                @Override
                //使用clientId作为设备ID时的认证方式
                public Mono<AuthenticationResponse> authenticate(@Nonnull AuthenticationRequest request, @Nonnull DeviceOperator device) {
                    MqttAuthenticationRequest mqttRequest = ((MqttAuthenticationRequest) request);
                    return device.getConfigs("username", "password")
                        .flatMap(values -> {
                            String username = values.getValue("username").map(Value::asString).orElse(null);
                            String password = values.getValue("password").map(Value::asString).orElse(null);
                            if (mqttRequest.getUsername().equals(username) && mqttRequest
                                .getPassword()
                                .equals(password)) {
                                return Mono.just(AuthenticationResponse.success());
                            } else {
                                return Mono.just(AuthenticationResponse.error(400, "密码错误"));
                            }

                        });
                }

                @Override
                //在网关中配置使用指定的认证协议时的认证方式
                public Mono<AuthenticationResponse> authenticate(@Nonnull AuthenticationRequest request, @Nonnull DeviceRegistry registry) {
                    MqttAuthenticationRequest mqttRequest = ((MqttAuthenticationRequest) request);
                    return registry
                        .getDevice(mqttRequest.getUsername()) //用户名作为设备ID
                        .flatMap(device -> device
                            .getSelfConfig("password").map(Value::asString) //密码
                            .flatMap(password -> {
                                if (password.equals(mqttRequest.getPassword())) {
                                    //认证成功，需要返回设备ID
                                    return Mono.just(AuthenticationResponse.success(mqttRequest.getUsername()));
                                } else {
                                    return Mono.just(AuthenticationResponse.error(400, "密码错误"));
                                }
                            }));
                }
            });

//        //tcp client,通过tcp客户端连接其他服务处理设备消息
//        {
//            support.addConfigMetadata(DefaultTransport.TCP, tcpClientConfig);
//            return Mono
//                .zip(
//                    Mono.justOrEmpty(context.getService(DecodedClientMessageHandler.class)),
//                    Mono.justOrEmpty(context.getService(DeviceSessionManager.class)),
//                    Mono.justOrEmpty(context.getService(Vertx.class))
//                )
//                .map(tp3 -> new TcpClientMessageSupport(tp3.getT1(), tp3.getT2(), tp3.getT3()))
//                .doOnNext(tcp -> {
//                    //设置状态检查
//                    support.setDeviceStateChecker(tcp);
//                    support.doOnDispose(tcp); //协议失效时执行
//                })
//                .thenReturn(support);
//
//        }

            return Mono.just(support);
        });
    }
}
