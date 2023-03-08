package top.iot.gateway.component.gateway.external;

import lombok.Data;

/**
 * @author ruanhong
 */
@Data
public class Result {

    private String code;

    private String message;

    private String data;

    public Result(String code, String message, String data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static Result success(String data) {
        return new Result("200", "success", data);

    }

    public static Result error(String message) {
        return new Result("500", message, null);

    }

}
