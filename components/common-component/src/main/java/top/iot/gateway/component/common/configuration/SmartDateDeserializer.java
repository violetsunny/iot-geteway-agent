package top.iot.gateway.component.common.configuration;

import top.iot.gateway.component.common.utils.TimeUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.SneakyThrows;

import java.util.Date;

/**
 * 时间反序列化配置
 *
 * @author hanyl
 */
public class SmartDateDeserializer extends JsonDeserializer<Date> {
    @Override
    @SneakyThrows
    public Date deserialize(JsonParser p, DeserializationContext ctxt) {
        if (p.hasToken(JsonToken.VALUE_STRING)) {
            String str = p.getText().trim();
            if (str.length() == 0) {
                return (Date) getEmptyValue(ctxt);
            }
            return TimeUtils.parseDate(str);
        }
        if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            long ts = p.getLongValue();
            return new Date(ts);
        }
        return null;
    }
}
