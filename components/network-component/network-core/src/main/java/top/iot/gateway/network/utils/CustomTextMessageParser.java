package top.iot.gateway.network.utils;

import io.vavr.CheckedConsumer;
import io.vavr.CheckedRunnable;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Hex;
import top.iot.gateway.core.message.codec.MessagePayloadType;
import top.iot.gateway.core.message.codec.TextMessageParser;

import java.util.function.BiConsumer;

@AllArgsConstructor(staticName = "of")
public class CustomTextMessageParser {

    private final CheckedConsumer<String> startConsumer;

    private final BiConsumer<String, String> headerConsumer;

    private final CheckedConsumer<TextMessageParser.Payload> bodyConsumer;

    private final CheckedRunnable noBodyConsumer;



    @SneakyThrows
    public void parse(String text) {
//        String[] lines = text.trim().split("[\n]");
//
//        int lineIndex = 0;
//        for (String line : lines) {
//            line = line.trim();
//            if(line.startsWith("//")){
//                continue;
//            }
//            if (StringUtils.isEmpty(line)) {
//                if (lineIndex > 0) {
//                    break;
//                }
//                continue;
//            }
//            if (lineIndex++ == 0) {
//                startConsumer.accept(line);
//            } else {
//                String[] header = line.split("[:]");
//                if (header.length > 1) {
//                    headerConsumer.accept(header[0].trim(), header[1].trim());
//                }
//            }
//        }
        //body
//        if (lineIndex < lines.length) {
//            String body = String.join("\n", Arrays.copyOfRange(lines, lineIndex, lines.length)).trim();
        String body = text.replaceAll("\r\n    ", "").replaceAll("\r\n", "");
        MessagePayloadType type;
        byte[] data;
        if (body.startsWith("0x")) {
            type = MessagePayloadType.BINARY;
            data = Hex.decodeHex(body = body.substring(2));
        } else if (body.startsWith("{") || body.startsWith("[")) {
            type = MessagePayloadType.JSON;
            data = body.getBytes();
        } else {
            type = MessagePayloadType.STRING;
            data = body.getBytes();
        }
        bodyConsumer.accept(new TextMessageParser.Payload(type, body, data));
//        } else {
//            noBodyConsumer.run();
//        }
    }
}
