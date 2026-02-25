package com.xdud.seckillbot.platform.model;

import com.xdud.seckillbot.domain.enums.ExecutionResult;
import lombok.Data;

@Data
public class OrderResult {

    private ExecutionResult result;
    private String orderId;
    private String message;
    private String rawResponse;

    public static OrderResult success(String orderId, String rawResponse) {
        OrderResult r = new OrderResult();
        r.setResult(ExecutionResult.SUCCESS);
        r.setOrderId(orderId);
        r.setRawResponse(rawResponse);
        return r;
    }

    public static OrderResult fail(ExecutionResult result, String message, String rawResponse) {
        OrderResult r = new OrderResult();
        r.setResult(result);
        r.setMessage(message);
        r.setRawResponse(rawResponse);
        return r;
    }
}
