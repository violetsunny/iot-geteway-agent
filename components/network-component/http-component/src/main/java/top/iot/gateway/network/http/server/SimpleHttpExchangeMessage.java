package top.iot.gateway.network.http.server;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Charsets;
import io.micrometer.core.instrument.util.StringUtils;
import io.netty.buffer.Unpooled;
import io.vertx.core.http.HttpServerRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.hswebframework.web.crud.web.ResponseMessage;
import top.iot.gateway.core.message.codec.MessagePayloadType;
import top.iot.gateway.core.message.codec.http.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class SimpleHttpExchangeMessage extends SimpleHttpRequestMessage implements HttpExchangeMessage {

    private HttpServerRequest request;

    @SneakyThrows
    public static void of(HttpServerRequest request, Consumer<SimpleHttpExchangeMessage> callback) {
        log.info("http request请求头\r\n：{}", request.scheme()+request.version() + "\r\n" + request.headers().toString());
        request.handler(body -> {
            SimpleHttpExchangeMessage httpExchangeMessage = new SimpleHttpExchangeMessage();
            httpExchangeMessage.request = request;
            httpExchangeMessage.setMethod(HttpMethod.resolve(request.method().name()));
            httpExchangeMessage.setPath(HttpUtils.getUrlPath(request.path()));
            httpExchangeMessage.setUrl(request.path());
            if (StringUtils.isNotBlank(request.query())) {
                httpExchangeMessage.setQueryParameters(
                        Stream.of(request.query().split("[&]"))
                                .map(str -> str.split("[=]", 2))
                                .filter(arr -> arr.length > 1)
                                .collect(Collectors.toMap(arr -> arr[0], arr -> arr[1], (a, b) -> String.join(",", a, b)))
                );
            }

            httpExchangeMessage.setHeaders(request.headers().entries()
                    .stream()
                    .map(e -> new Header(e.getKey(), new String[]{e.getValue()}))
                    .collect(Collectors.toList()));

            String cnt = body.getByteBuf().toString(Charsets.UTF_8);
            MessagePayloadType type;
            byte[] data = new byte[0];
            if (cnt.startsWith("0x")) {
                type = MessagePayloadType.BINARY;
                try {
                    data = Hex.decodeHex(cnt.substring(2));
                } catch (DecoderException e) {
                    e.printStackTrace();
                }
            } else if (cnt.startsWith("{") || cnt.startsWith("[")) {
                type = MessagePayloadType.JSON;
                if (JSONObject.parseObject(cnt).getJSONObject("header") != null) {
                    log.info("deviceId:[{}]", JSONObject.parseObject(cnt).getJSONObject("header").getString("dev"));
                }
                data = cnt.getBytes();
            } else {
                type = MessagePayloadType.STRING;
                data = cnt.getBytes();
            }
            httpExchangeMessage.setPayload(Unpooled.wrappedBuffer(data));

            if (request.getHeader(HttpHeaders.CONTENT_TYPE)==null) {
                if (type == MessagePayloadType.JSON) {
                    httpExchangeMessage.setContentType(MediaType.APPLICATION_JSON);
                } else if (type == MessagePayloadType.STRING) {
                    httpExchangeMessage.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                }
            }
            log.info("start repsonse msg for request uri:{}", request.uri());
            request.response().end(JSONObject.toJSONString(ResponseMessage.ok()));

            callback.accept(httpExchangeMessage);
        });
    }

    @Nonnull
    @Override
    public Mono<Void> response(@Nonnull HttpResponseMessage message) {
        return Mono.just(request.response().end(JSONObject.toJSONString(ResponseMessage.ok(JSONObject.parseObject(message.getPayload().toString(Charsets.UTF_8)))))).then();
    }
}
