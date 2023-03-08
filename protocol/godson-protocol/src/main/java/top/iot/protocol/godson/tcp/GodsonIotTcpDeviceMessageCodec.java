package top.iot.protocol.godson.tcp;

import com.alibaba.fastjson.JSONObject;
import top.iot.protocol.godson.metadataMapping.ProtocolConfigRegistry;
import top.iot.protocol.godson.tcp.message.InvokeFunction;
import top.iot.protocol.godson.tcp.message.ReadProperty;
import top.iot.protocol.godson.tcp.message.WriteProperty;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.expands.script.engine.DynamicScriptEngine;
import org.hswebframework.expands.script.engine.DynamicScriptEngineFactory;
import top.iot.gateway.core.Value;
import top.iot.gateway.core.device.DeviceConfigKey;
import top.iot.gateway.core.message.DeviceOnlineMessage;
import top.iot.gateway.core.message.Message;
import top.iot.gateway.core.message.codec.*;
import top.iot.gateway.core.message.function.FunctionInvokeMessage;
import top.iot.gateway.core.message.property.ReadPropertyMessage;
import top.iot.gateway.core.message.property.WritePropertyMessage;
import top.iot.gateway.core.server.session.DeviceSession;
import org.reactivestreams.Publisher;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * tcp编解码处理器
 */
@AllArgsConstructor
@Slf4j
public class GodsonIotTcpDeviceMessageCodec implements DeviceMessageCodec {

    private final ProtocolConfigRegistry registry;

    /**
     * 协议类型:TCP
     */
    @Override
    public Transport getSupportTransport() {
        return DefaultTransport.TCP;
    }

    @Override
    @SneakyThrows
    @Nonnull
    public Publisher<? extends Message> decode(@Nonnull MessageDecodeContext context) {
        log.debug("tcp编解码处理器收到消息：{}",context.getMessage());

        FromDeviceMessageContext ctx = ((FromDeviceMessageContext) context);
        byte[] payload = context.getMessage().payloadAsBytes();
        String hexString = Hex.encodeHexString(payload);
        if (log.isDebugEnabled()) {
            log.debug("handle tcp message:\n{}", hexString);
        }

        DeviceSession session = ctx.getSession();

        return Flux.just(context)
                .map(MessageDecodeContext::getMessage)
                .flatMap(msg ->
                                context.getDeviceAsync()
                                        .switchIfEmpty(Mono.fromRunnable(() -> log.warn("device UnknownTcpDeviceSession")))
                                        .flatMapMany(deviceOperator ->
                                                registry.getMetadataMapping(deviceOperator.getSelfConfig(DeviceConfigKey.productId))
                                                        .flatMapMany(deviceMetadataMapping -> {
                                                            String msgType = deviceMetadataMapping.getMsgType().entrySet().stream()
                                                                    .filter(entry -> {
                                                                        boolean flag = entry.getValue()!=null && StringUtils.hasText(String.valueOf(entry.getValue())) && hexString.startsWith(String.valueOf(entry.getValue()));
                                                                        Optional<Object> lowLenLimit = deviceMetadataMapping.getMsgInfo("lowLenLimit");
                                                                        if (lowLenLimit.isPresent() && !StringUtils.isEmpty(lowLenLimit.orElse(null))) {
                                                                            flag = flag & hexString.length() >= Integer.parseInt(String.valueOf(lowLenLimit.get()));
                                                                        }
                                                                        return flag;
                                                                    })
                                                                    .map(Map.Entry::getKey).findFirst().orElse(null);

                                                            //如果是认证，需要校验key
                                                            if (StringUtils.hasText(msgType) && msgType.equals(TcpMessageType.AUTH_REQ.name())) {
                                                                Object authKeyObj = deviceMetadataMapping.getMsgInfo("authKey").orElse(null);
                                                                if (StringUtils.isEmpty(authKeyObj)) return Mono.empty();
                                                                String[] authKeyArr = String.valueOf(authKeyObj).split(",");
                                                                if (authKeyArr.length != 2) return Mono.empty();
                                                                byte[] authKey = Arrays.copyOfRange(payload, Integer.parseInt(authKeyArr[0]), Integer.parseInt(authKeyArr[1]));

                                                                return deviceOperator.getConfig("tcp_auth_key")
                                                                        .map(Value::asString)
                                                                        .filter(key -> Arrays.equals(authKey, key.getBytes()))
                                                                        .flatMapMany(r -> {
                                                                            //认证通过
                                                                            DeviceOnlineMessage onlineMessage = new DeviceOnlineMessage();
                                                                            onlineMessage.setDeviceId(deviceOperator.getDeviceId());
                                                                            onlineMessage.setTimestamp(System.currentTimeMillis());
                                                                            String authRes = String.valueOf(deviceMetadataMapping.getMsgType("AUTH_RES").orElse(""));
                                                                            if (StringUtils.hasText(authRes)) {
                                                                                try {
                                                                                    return session
                                                                                            .send(EncodedMessage.simple(Unpooled.wrappedBuffer(Hex.decodeHex(authRes))))
                                                                                            .thenReturn(onlineMessage);
                                                                                } catch (DecoderException e) {
                                                                                    log.error("响应AUTH_RES十六进制字符串{}转字节数组失败", authRes);
                                                                                }

                                                                            }
                                                                            return Mono.just(onlineMessage);
                                                                        })
                                                                        //为空可能设备不存在或者没有配置tcp_auth_key,响应错误信息.
                                                                        .switchIfEmpty(Mono.empty());
                                                            }
                                                            //keepalive, ping pong
//                                                            if (msgType.equals(TcpMessageType.PING.name())) {
//                                                                return session
//                                                                    .send(EncodedMessage.simple(Unpooled.wrappedBuffer(TcpMessage.of(TcpMessageType.PONG, new Pong()).toBytes())))
//                                                                    .then(Mono.fromRunnable(session::ping));
//                                                            }
                                                            Object reportPropertyRes = deviceMetadataMapping.getMsgType("REPORT_PROPERTY_RES").orElse(null);
                                                            Object reportPropertyResType = deviceMetadataMapping.getMsgType("REPORT_PROPERTY_RES_TYPE").orElse(null);
                                                            if (!StringUtils.isEmpty(reportPropertyRes) && !StringUtils.isEmpty(reportPropertyResType)) {
                                                                reportPropertyRes = String.valueOf(parseScriptExpression(JSONObject.parseObject(String.valueOf(reportPropertyRes)), hexString));
                                                            }
                                                            if (!StringUtils.isEmpty(reportPropertyRes)) {
                                                                try {
                                                                    session.send(EncodedMessage.simple(Unpooled.wrappedBuffer(Hex.decodeHex(String.valueOf(reportPropertyRes))))).subscribe();
                                                                } catch (DecoderException e) {
                                                                    log.error("响应REPORT_PROPERTY_RES十六进制字符串{}转字节数组失败", reportPropertyRes);
                                                                    return Mono.empty();
                                                                }
                                                            }
                                                            return registry.getMessagePayloadParser(deviceMetadataMapping.getType())
                                                                    .handle(deviceOperator.getDeviceId(), deviceMetadataMapping, msg);
                                                        }))
                );
    }

//    var CRC = {};
//
//    CRC._auchCRCHi = [
//        0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0,
//        0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41,
//        0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0,
//        0x80, 0x41, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40,
//        0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1,
//        0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0, 0x80, 0x41,
//        0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1,
//        0x81, 0x40, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41,
//        0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0,
//        0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40,
//        0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1,
//        0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40,
//        0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0,
//        0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40,
//        0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0,
//        0x80, 0x41, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40,
//        0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0,
//        0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41,
//        0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0,
//        0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41,
//        0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0,
//        0x80, 0x41, 0x00, 0xC1, 0x81, 0x40, 0x00, 0xC1, 0x81, 0x40,
//        0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0, 0x80, 0x41, 0x00, 0xC1,
//        0x81, 0x40, 0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41,
//        0x00, 0xC1, 0x81, 0x40, 0x01, 0xC0, 0x80, 0x41, 0x01, 0xC0,
//        0x80, 0x41, 0x00, 0xC1, 0x81, 0x40
//        ];
//    CRC._auchCRCLo = [
//        0x00, 0xC0, 0xC1, 0x01, 0xC3, 0x03, 0x02, 0xC2, 0xC6, 0x06,
//        0x07, 0xC7, 0x05, 0xC5, 0xC4, 0x04, 0xCC, 0x0C, 0x0D, 0xCD,
//        0x0F, 0xCF, 0xCE, 0x0E, 0x0A, 0xCA, 0xCB, 0x0B, 0xC9, 0x09,
//        0x08, 0xC8, 0xD8, 0x18, 0x19, 0xD9, 0x1B, 0xDB, 0xDA, 0x1A,
//        0x1E, 0xDE, 0xDF, 0x1F, 0xDD, 0x1D, 0x1C, 0xDC, 0x14, 0xD4,
//        0xD5, 0x15, 0xD7, 0x17, 0x16, 0xD6, 0xD2, 0x12, 0x13, 0xD3,
//        0x11, 0xD1, 0xD0, 0x10, 0xF0, 0x30, 0x31, 0xF1, 0x33, 0xF3,
//        0xF2, 0x32, 0x36, 0xF6, 0xF7, 0x37, 0xF5, 0x35, 0x34, 0xF4,
//        0x3C, 0xFC, 0xFD, 0x3D, 0xFF, 0x3F, 0x3E, 0xFE, 0xFA, 0x3A,
//        0x3B, 0xFB, 0x39, 0xF9, 0xF8, 0x38, 0x28, 0xE8, 0xE9, 0x29,
//        0xEB, 0x2B, 0x2A, 0xEA, 0xEE, 0x2E, 0x2F, 0xEF, 0x2D, 0xED,
//        0xEC, 0x2C, 0xE4, 0x24, 0x25, 0xE5, 0x27, 0xE7, 0xE6, 0x26,
//        0x22, 0xE2, 0xE3, 0x23, 0xE1, 0x21, 0x20, 0xE0, 0xA0, 0x60,
//        0x61, 0xA1, 0x63, 0xA3, 0xA2, 0x62, 0x66, 0xA6, 0xA7, 0x67,
//        0xA5, 0x65, 0x64, 0xA4, 0x6C, 0xAC, 0xAD, 0x6D, 0xAF, 0x6F,
//        0x6E, 0xAE, 0xAA, 0x6A, 0x6B, 0xAB, 0x69, 0xA9, 0xA8, 0x68,
//        0x78, 0xB8, 0xB9, 0x79, 0xBB, 0x7B, 0x7A, 0xBA, 0xBE, 0x7E,
//        0x7F, 0xBF, 0x7D, 0xBD, 0xBC, 0x7C, 0xB4, 0x74, 0x75, 0xB5,
//        0x77, 0xB7, 0xB6, 0x76, 0x72, 0xB2, 0xB3, 0x73, 0xB1, 0x71,
//        0x70, 0xB0, 0x50, 0x90, 0x91, 0x51, 0x93, 0x53, 0x52, 0x92,
//        0x96, 0x56, 0x57, 0x97, 0x55, 0x95, 0x94, 0x54, 0x9C, 0x5C,
//        0x5D, 0x9D, 0x5F, 0x9F, 0x9E, 0x5E, 0x5A, 0x9A, 0x9B, 0x5B,
//        0x99, 0x59, 0x58, 0x98, 0x88, 0x48, 0x49, 0x89, 0x4B, 0x8B,
//        0x8A, 0x4A, 0x4E, 0x8E, 0x8F, 0x4F, 0x8D, 0x4D, 0x4C, 0x8C,
//        0x44, 0x84, 0x85, 0x45, 0x87, 0x47, 0x46, 0x86, 0x82, 0x42,
//        0x43, 0x83, 0x41, 0x81, 0x80, 0x40
//        ];
//
//    CRC.CRC16 = function (buffer) {
//        var hi = 0xff;
//        var lo = 0xff;
//        for (var i = 0; i < buffer.length; i++) {
//            var idx = hi ^ buffer[i];
//            hi = (lo ^ CRC._auchCRCHi[idx]);
//            lo = CRC._auchCRCLo[idx];
//        }
//        return CRC.padLeft((lo << 8 | hi).toString(16).toUpperCase(), 4, '0');
//    };
//
//    CRC.isArray = function (arr) {
//        return Object.prototype.toString.call(arr) === '[object Array]';
//    };
//
//    CRC.ToCRC16 = function (str) {
//        return CRC.CRC16(CRC.isArray(str) ? str : CRC.strToByte(str));
//    };
//
//    CRC.ToModbusCRC16 = function (str) {
//        return CRC.CRC16(CRC.isArray(str) ? str : CRC.strToHex(str));
//    };
//
//    CRC.strToByte = function (str) {
//        var tmp = str.split(''), arr = [];
//        for (var i = 0, c = tmp.length; i < c; i++) {
//            var j = encodeURI(tmp[i]);
//            if (j.length == 1) {
//                arr.push(j.charCodeAt());
//            } else {
//                var b = j.split('%');
//                for (var m = 1; m < b.length; m++) {
//                    arr.push(parseInt('0x' + b[m]));
//                }
//            }
//        }
//        return arr;
//    };
//
//    CRC.convertChinese = function (str) {
//        var tmp = str.split(''), arr = [];
//        for (var i = 0, c = tmp.length; i < c; i++) {
//            var s = tmp[i].charCodeAt();
//            if (s <= 0 || s >= 127) {
//                arr.push(s.toString(16));
//            }
//            else {
//                arr.push(tmp[i]);
//            }
//        }
//        return arr;
//    };
//
//    CRC.filterChinese = function (str) {
//        var tmp = str.split(''), arr = [];
//        for (var i = 0, c = tmp.length; i < c; i++) {
//            var s = tmp[i].charCodeAt();
//            if (s > 0 && s < 127) {
//                arr.push(tmp[i]);
//            }
//        }
//        return arr;
//    };
//
//    CRC.strToHex = function (hex, isFilterChinese) {
//        hex = isFilterChinese ? CRC.filterChinese(hex).join('') : CRC.convertChinese(hex).join('');
//
//        //清除所有空格
//        hex = hex.replace(/\s/g, '');
//        //若字符个数为奇数，补一个空格
//        hex += hex.length % 2 != 0 ? ' ' : '';
//
//        var c = hex.length / 2, arr = [];
//        for (var i = 0; i < c; i++) {
//            arr.push(parseInt(hex.substr(i * 2, 2), 16));
//        }
//        return arr;
//    };
//
//    CRC.padLeft = function (s, w, pc) {
//        if (pc == undefined) {
//            pc = '0';
//        }
//        for (var i = 0, c = w - s.length; i < c; i++) {
//            s = pc + s;
//        }
//        return s;
//    };
//
//
//
//    let head = '7E7E';
//    //let deviceId = $currentValue.substr(3*2, 5*2);
//    let deviceId= '6020000133';
//    //let centerSiteId = $currentValue.substr(2*2, 1*2);
//    let centerSiteId = '16';
//
//    //let password = $currentValue.substring(8*2, (8+2)*2);
//    let password = 'FFFF';
//    let funcId = '32';
//    let msgFlag = '8008';
//    let startFlag = '02';
//    //let streamCode = $currentValue.substr(14*2, 2*2);
//    let streamCode = '0001';
//
//
//    Date.prototype.Format = function (fmt) {
//        var o = {
//        'M+': this.getMonth() + 1, //月份
//        'd+': this.getDate(), //日
//        'H+': this.getHours(), //小时
//        'm+': this.getMinutes(), //分
//        's+': this.getSeconds(), //秒
//        'q+': Math.floor((this.getMonth() + 3) / 3), //季度
//        'S': this.getMilliseconds() //毫秒
//    };
//    if (/(y+)/.test(fmt)) fmt = fmt.replace(RegExp.$1, (this.getFullYear() + '').substr(4 - RegExp.$1.length));
//    for (var k in o)
//    if (new RegExp('(' + k + ')').test(fmt)) fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (('00' + o[k]).substr(('' + o[k]).length)));
//    return fmt;
//    }
//
//    // let uploadTime = new Date().Format('yyMMddHHmmss');
//    let uploadTime = '200826152553';
//    let endFlag = '04';
//    let res = head+deviceId+centerSiteId+password+funcId+msgFlag+startFlag+streamCode+uploadTime+endFlag;
//
//
//        console.log(res);
//    // let crc16 = CRC.ToModbusCRC16('7E7E602000013316FFFF32800802000120082615255304', true);
//    let crc16 = CRC.ToModbusCRC16(res, true);
//        console.log(crc16);

    @SneakyThrows
    private Object parseScriptExpression(Map<String, Object> expression, Object value) {
        String lang = (String) expression.get("lang");
        String script = (String) expression.get("script");
        script = script.replaceAll("\\\\", "");

        DynamicScriptEngine engine = DynamicScriptEngineFactory.getEngine(lang);
        if (engine == null) {
            throw new IllegalArgumentException("不支持的脚本:" + lang);
        }
        String id = DigestUtils.md5Hex(script);
        if (!engine.compiled(id)) {
            engine.compile(id, script);
        }
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("$currentValue", value);
        ctx.put("$illegalArgs", "illegalArgs");
        Object result = engine.execute(id, ctx).getIfSuccess();
        if ("illegalArgs".equals(result)) throw new IllegalArgumentException("不合法的参数值:" + value);
        return result;
    }

    /**
     * demo tcp 报文协议格式
     * <p>
     * 第0字节为消息类型
     * 第1-4字节为消息体长度
     * 第5-n为消息体
     */
    @Override
    @Nonnull
    public Publisher<? extends EncodedMessage> encode(@Nonnull MessageEncodeContext context) {
        Message message = context.getMessage();
        EncodedMessage encodedMessage = null;
        log.info("推送设备消息，消息ID：{}", message.getMessageId());
        // 获取设备属性
        if (message instanceof ReadPropertyMessage) {
            ReadPropertyMessage readPropertyMessage = (ReadPropertyMessage) message;
            TcpMessage of = TcpMessage.of(TcpMessageType.READ_PROPERTY, ReadProperty.of(readPropertyMessage));
            encodedMessage = EncodedMessage.simple(of.toByteBuf());
        }
        //修改设备属性
        if (message instanceof WritePropertyMessage) {
            WritePropertyMessage writePropertyMessage = (WritePropertyMessage) message;
            TcpMessage of = TcpMessage.of(TcpMessageType.WRITE_PROPERTY, WriteProperty.of(writePropertyMessage));
            encodedMessage = EncodedMessage.simple(of.toByteBuf());
        }
        if (message instanceof FunctionInvokeMessage) {
            FunctionInvokeMessage functionInvokeMessage = (FunctionInvokeMessage) message;
            TcpMessage of = TcpMessage.of(TcpMessageType.INVOKE_FUNCTION, InvokeFunction.of(functionInvokeMessage));
            encodedMessage = EncodedMessage.simple(of.toByteBuf());
        }
        // 设备上报属性
//        if (message instanceof ReportPropertyMessage) {
//            ReportPropertyMessage reportPropertyMessage = (ReportPropertyMessage) message;
//            TcpMessage of = TcpMessage.of(TcpMessageType.REPORT_TEMPERATURE, ReportProperty.of(reportPropertyMessage));
//            encodedMessage = EncodedMessage.simple(of.toByteBuf());
//        }
        return encodedMessage != null ? Mono.just(encodedMessage) : Mono.empty();
//        return Mono.empty();
    }
}