package top.iot.protocol.configuration;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.hswebframework.expands.script.engine.DynamicScriptEngine;
import org.hswebframework.expands.script.engine.DynamicScriptEngineFactory;
import org.hswebframework.web.bean.FastBeanCopier;
import top.iot.gateway.core.ProtocolSupport;
import top.iot.gateway.core.Value;
import top.iot.gateway.core.defaults.Authenticator;
import top.iot.gateway.core.defaults.CompositeProtocolSupport;
import top.iot.gateway.core.device.*;
import top.iot.gateway.core.message.ChildDeviceMessage;
import top.iot.gateway.core.message.CommonDeviceMessage;
import top.iot.gateway.core.message.Message;
import top.iot.gateway.core.message.codec.*;
import top.iot.gateway.core.message.property.ReportPropertyMessage;
import top.iot.gateway.core.metadata.DefaultConfigMetadata;
import top.iot.gateway.core.metadata.types.PasswordType;
import top.iot.gateway.core.metadata.types.StringType;
import top.iot.gateway.core.spi.ProtocolSupportProvider;
import top.iot.gateway.core.spi.ServiceContext;
import top.iot.gateway.supports.official.IotGatewayDeviceMetadataCodec;
import top.iot.gateway.supports.protocol.management.ProtocolSupportDefinition;
import org.reactivestreams.Publisher;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@AllArgsConstructor
public class ScriptProtocolSupportProvider implements ProtocolSupportProvider {

    private ProtocolSupportDefinition definition;

    private static Map<String, Function<MessageDecodeContext, Map<String, Object>>> decoderFuncs = new ConcurrentHashMap<>();
    private static Map<String, Function<MessageEncodeContext, Publisher<? extends EncodedMessage>>> encoderFuncs = new ConcurrentHashMap<>();

    @Override
    public void dispose() {
        //协议卸载时执行
    }

    private static final DefaultConfigMetadata mqttConfig = new DefaultConfigMetadata(
        "MQTT认证配置"
        , "")
        .add("username", "username", "MQTT用户名", StringType.GLOBAL)
        .add("password", "password", "MQTT密码", PasswordType.GLOBAL);


    @Override
    @SneakyThrows
    public Mono<? extends ProtocolSupport> create(ServiceContext context) {
        CompositeProtocolSupport support = new CompositeProtocolSupport();
        support.setId(definition.getId());
        support.setName(definition.getName());
        support.setDescription(definition.getDescription());

        //固定为IotGatewayDeviceMetadataCodec
        support.setMetadataCodec(new IotGatewayDeviceMetadataCodec());

        Map<String, Object> configs = definition.getConfiguration();
        String scriptContent = (String) configs.get("script");
        DynamicScriptEngine engine = DynamicScriptEngineFactory.getEngine((String) configs.get("lang"));
        engine.compile(support.getId(), scriptContent);
        Map<String, Object> ctx = Maps.newHashMap();
        ctx.put("codec", this);
        ctx.put("logger", LoggerFactory.getLogger("message.handler"));
        engine.execute(support.getId(), ctx).getIfSuccess();
        engine.remove(support.getId());

        String transportStr = String.valueOf(configs.get("transport"));
        String[] transportArr = transportStr.split(",");

        for (String transport : transportArr) {
            DeviceMessageCodec codec = new DeviceMessageCodec() {
                @Nonnull
                @Override
                public Publisher<? extends EncodedMessage> encode(@Nonnull MessageEncodeContext context) {
                    return encoderFuncs.get(definition.getId()).apply(context);
                }

                @Nonnull
                @Override
                public Publisher<? extends Message> decode(@Nonnull MessageDecodeContext context) {
                    return doDecode(context);
                }

                @Override
                public Transport getSupportTransport() {
                    return DefaultTransport.valueOf(transport);
                }
            };
            support.addMessageCodecSupport(DefaultTransport.valueOf(transport), () -> Mono.just(codec));

            if ("MQTT".equals(transport)) {
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
            }
        }
        return Mono.just(support);
    }

    public void decoder(Function<MessageDecodeContext, Map<String, Object>> _decoderFunc) {
        decoderFuncs.put(definition.getId(), _decoderFunc);
    }

    public void encoder(Function<MessageEncodeContext, Publisher<? extends EncodedMessage>> _encoderFunc) {
        encoderFuncs.put(definition.getId(), _encoderFunc);
    }

    private Publisher<? extends Message> doDecode(@Nonnull MessageDecodeContext context) {
        return Flux.defer(() -> {
            String jsonString = JSONObject.toJSONString(decoderFuncs.get(definition.getId()).apply(context));
            JSONObject jsonObject = JSONObject.parseObject(jsonString);
            if (jsonString.startsWith("{\"0\"")) {
                Collection<Object> values = jsonObject.values();
                List<CommonDeviceMessage<?>> messages = new ArrayList<>();
                for (Object obj : values) {
                    JSONObject jsonObj = JSONObject.parseObject(JSONObject.toJSONString(obj));
                    String messageType = jsonObj.getString("messageType");
                    if ("REPORT_PROPERTY".equals(messageType)) {
                        ReportPropertyMessage msg = ReportPropertyMessage.create();
                        msg.fromJson(jsonObj);
                        messages.add(msg);
                    } else if ("CHILD".equals(messageType)) {
                        ChildDeviceMessage childDeviceMessage = new ChildDeviceMessage();
                        FastBeanCopier.copy(jsonObj, childDeviceMessage, "headers", "childDeviceMessage");
                        JSONObject childJsonObj = jsonObj.getJSONObject("childDeviceMessage");
                        ReportPropertyMessage msg = ReportPropertyMessage.create();
                        msg.fromJson(childJsonObj);
                        childDeviceMessage.setChildDeviceMessage(msg);
                        messages.add(childDeviceMessage);
                    }
                }
                return Flux.fromIterable(messages);
            } else {
                String messageType = jsonObject.getString("messageType");
                if ("REPORT_PROPERTY".equals(messageType)) {
                    ReportPropertyMessage msg = ReportPropertyMessage.create();
                    msg.fromJson(jsonObject);
                    return Mono.just(msg);
                } else if ("CHILD".equals(messageType)) {
                    ChildDeviceMessage childDeviceMessage = new ChildDeviceMessage();
                    FastBeanCopier.copy(jsonObject, childDeviceMessage, "headers", "childDeviceMessage");
                    JSONObject childJsonObj = jsonObject.getJSONObject("childDeviceMessage");
                    ReportPropertyMessage msg = ReportPropertyMessage.create();
                    msg.fromJson(childJsonObj);
                    childDeviceMessage.setChildDeviceMessage(msg);
                    return Mono.just(childDeviceMessage);
                }
            }
            return Mono.empty();
        });
    }
}
