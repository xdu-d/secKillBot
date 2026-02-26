package com.xdud.seckillbot.platform.impl.imoutai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xdud.seckillbot.common.exception.BizException;
import com.xdud.seckillbot.common.exception.ErrorCode;
import com.xdud.seckillbot.domain.enums.ExecutionResult;
import com.xdud.seckillbot.domain.enums.PlatformType;
import com.xdud.seckillbot.platform.model.AuthContext;
import com.xdud.seckillbot.platform.model.OrderRequest;
import com.xdud.seckillbot.platform.model.OrderResult;
import com.xdud.seckillbot.platform.model.ProductInfo;
import com.xdud.seckillbot.platform.spi.PlatformAdapter;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;

/**
 * i茅台平台适配器 Phase 2 完整实现。
 * 签名算法：MD5(sorted_params_kv + "&" + timestamp + "&" + deviceId + "&" + APP_SECRET)
 */
@Component
public class ImoutaiAdapter implements PlatformAdapter {

    private static final Logger log = LoggerFactory.getLogger(ImoutaiAdapter.class);
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");
    private static final String APP_SECRET = "2af72f100c356273d46284f6fd1dfc08";

    @Value("${imoutai.base-url:https://app.moutai519.com.cn}")
    private String baseUrl;

    @Value("${imoutai.app-version:1.3.7}")
    private String appVersion;

    private final OkHttpClient okHttpClient;
    private final Gson gson = new Gson();

    public ImoutaiAdapter(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    // ──────────────────────────────────────────────────────────────────
    // PlatformAdapter SPI
    // ──────────────────────────────────────────────────────────────────

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.IMOUTAI;
    }

    @Override
    public AuthContext login(String credentialJson) {
        ImoutaiCredential cred = gson.fromJson(credentialJson, ImoutaiCredential.class);
        if (cred.getToken() != null && !cred.getToken().isEmpty()) {
            AuthContext ctx = new AuthContext();
            ctx.setAccessToken(cred.getToken());
            ctx.setDeviceId(cred.getDeviceId());
            ctx.setPlatformUserId(cred.getUserId());
            ctx.setExpiresAt(LocalDateTime.now().plusDays(30));
            return ctx;
        }
        throw new BizException(ErrorCode.ACCOUNT_AUTH_EXPIRED, "凭据中无 token，请先完成短信登录");
    }

    @Override
    public AuthContext refresh(AuthContext current) {
        if (current == null) {
            throw new BizException(ErrorCode.ACCOUNT_AUTH_EXPIRED, "认证上下文为空，需重新登录");
        }
        if (!needsRefresh(current)) {
            return current;
        }
        throw new BizException(ErrorCode.ACCOUNT_AUTH_EXPIRED, "i茅台 token 已过期，请重新短信登录");
    }

    @Override
    public boolean needsRefresh(AuthContext context) {
        if (context == null || context.getExpiresAt() == null) {
            return true;
        }
        return context.getExpiresAt().minusMinutes(30).isBefore(LocalDateTime.now());
    }

    @Override
    public ProductInfo queryProduct(String productId, AuthContext authContext) {
        String sessionId = queryCurrentSession(authContext);
        String body = gson.toJson(new java.util.HashMap<String, Object>() {{
            put("sessionId", sessionId);
        }});
        String respJson = post("/xhr/front/mall/product/queryProductList", body, authContext);
        JsonObject resp = JsonParser.parseString(respJson).getAsJsonObject();
        checkCode(resp, respJson);

        ProductInfo info = new ProductInfo();
        info.setProductId(productId);
        info.setRawJson(respJson);

        // 遍历 data.products 找到 productId 匹配项
        if (resp.has("data") && resp.getAsJsonObject("data").has("products")) {
            resp.getAsJsonObject("data").getAsJsonArray("products").forEach(el -> {
                JsonObject p = el.getAsJsonObject();
                if (productId.equals(getStr(p, "productId"))) {
                    info.setProductName(getStr(p, "productName"));
                    info.setInStock(p.has("remainAmount") && p.get("remainAmount").getAsInt() > 0);
                    info.setStockCount(p.has("remainAmount") ? p.get("remainAmount").getAsInt() : 0);
                }
            });
        }
        return info;
    }

    @Override
    public boolean hasStock(String productId, AuthContext authContext) {
        return queryProduct(productId, authContext).isInStock();
    }

    @Override
    public void preOrderSetup(OrderRequest request) {
        // i茅台申购无需加购步骤
    }

    @Override
    public OrderResult submitOrder(OrderRequest request) {
        AuthContext auth = request.getAuthContext();
        String sessionId = queryCurrentSession(auth);

        Map<String, String> extras = request.getExtras();
        String shopId = extras != null ? extras.get("shopId") : null;
        if (shopId == null || shopId.isEmpty()) {
            return OrderResult.fail(ExecutionResult.FAILED, "extras.shopId 不能为空", null);
        }

        String body = gson.toJson(new java.util.HashMap<String, Object>() {{
            put("sessionId", sessionId);
            put("productId", request.getProductId());
            put("shopId", shopId);
            put("count", request.getQuantity() > 0 ? request.getQuantity() : 1);
        }});

        String respJson;
        try {
            respJson = post("/xhr/front/mall/reservation/reserve", body, auth);
        } catch (Exception e) {
            log.error("提交预约请求异常", e);
            return OrderResult.fail(ExecutionResult.NETWORK_ERROR, e.getMessage(), null);
        }

        JsonObject resp = JsonParser.parseString(respJson).getAsJsonObject();
        int code = resp.has("code") ? resp.get("code").getAsInt() : -1;

        if (code == 2000) {
            String orderId = resp.has("data") && resp.getAsJsonObject("data").has("reservationId")
                    ? resp.getAsJsonObject("data").get("reservationId").getAsString()
                    : "unknown";
            return OrderResult.success(orderId, respJson);
        } else if (code == 4029) {
            return OrderResult.fail(ExecutionResult.NO_STOCK, "已申购或无库存", respJson);
        } else if (code == 4003) {
            return OrderResult.fail(ExecutionResult.AUTH_EXPIRED, "认证已过期", respJson);
        } else {
            String msg = resp.has("message") ? resp.get("message").getAsString() : "未知错误";
            return OrderResult.fail(ExecutionResult.FAILED, msg, respJson);
        }
    }

    @Override
    public String queryOrderStatus(String orderId, AuthContext authContext) {
        String body = gson.toJson(new java.util.HashMap<String, Object>() {{
            put("reservationId", orderId);
        }});
        String respJson = post("/xhr/front/mall/reservation/query", body, authContext);
        return respJson;
    }

    // ──────────────────────────────────────────────────────────────────
    // 公开方法：供 AccountService 调用的双步短信登录
    // ──────────────────────────────────────────────────────────────────

    /**
     * 发送验证码（无需认证 token）。
     */
    public void sendSmsCode(String phone, String deviceId) {
        long ts = System.currentTimeMillis();
        String sign = buildSign(new TreeMap<String, String>() {{
            put("mobile", phone);
        }}, ts, deviceId);

        String body = gson.toJson(new java.util.HashMap<String, Object>() {{
            put("mobile", phone);
        }});
        String respJson = postWithHeaders("/xhr/front/user/register/mobileCode", body,
                buildHeaders(deviceId, ts, sign, null));
        JsonObject resp = JsonParser.parseString(respJson).getAsJsonObject();
        checkCode(resp, respJson);
        log.info("i茅台发送验证码成功，phone={}", phone);
    }

    /**
     * 使用短信验证码登录，返回 AuthContext。
     */
    public AuthContext loginWithSms(String phone, String code, String deviceId) {
        long ts = System.currentTimeMillis();
        String sign = buildSign(new TreeMap<String, String>() {{
            put("mobile", phone);
            put("yzm", code);
        }}, ts, deviceId);

        String body = gson.toJson(new java.util.HashMap<String, Object>() {{
            put("mobile", phone);
            put("yzm", code);
        }});
        String respJson = postWithHeaders("/xhr/front/user/register/loginByMobile", body,
                buildHeaders(deviceId, ts, sign, null));
        JsonObject resp = JsonParser.parseString(respJson).getAsJsonObject();
        checkCode(resp, respJson);

        JsonObject data = resp.getAsJsonObject("data");
        String token = getStr(data, "token");
        String userId = getStr(data, "userId");

        AuthContext ctx = new AuthContext();
        ctx.setAccessToken(token);
        ctx.setDeviceId(deviceId);
        ctx.setPlatformUserId(userId);
        ctx.setExpiresAt(LocalDateTime.now().plusDays(30));
        log.info("i茅台短信登录成功，userId={}", userId);
        return ctx;
    }

    // ──────────────────────────────────────────────────────────────────
    // 私有辅助方法
    // ──────────────────────────────────────────────────────────────────

    private String queryCurrentSession(AuthContext auth) {
        String respJson = get("/xhr/front/mall/sessions/query", auth);
        JsonObject resp = JsonParser.parseString(respJson).getAsJsonObject();
        checkCode(resp, respJson);
        return resp.getAsJsonObject("data").get("sessionId").getAsString();
    }

    private String get(String path, AuthContext auth) {
        long ts = System.currentTimeMillis();
        String sign = buildSign(new TreeMap<>(), ts, auth.getDeviceId());
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .headers(buildHeaders(auth.getDeviceId(), ts, sign, auth.getAccessToken()))
                .get()
                .build();
        return execute(request);
    }

    private String post(String path, String jsonBody, AuthContext auth) {
        long ts = System.currentTimeMillis();
        String sign = buildSign(new TreeMap<>(), ts, auth.getDeviceId());
        return postWithHeaders(path, jsonBody, buildHeaders(auth.getDeviceId(), ts, sign, auth.getAccessToken()));
    }

    private String postWithHeaders(String path, String jsonBody, Headers headers) {
        RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA);
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .headers(headers)
                .post(body)
                .build();
        return execute(request);
    }

    private String execute(Request request) {
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new BizException(ErrorCode.INTERNAL_ERROR,
                        "HTTP 请求失败: " + response.code() + " " + request.url());
            }
            ResponseBody body = response.body();
            return body != null ? body.string() : "{}";
        } catch (IOException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "网络请求异常: " + e.getMessage());
        }
    }

    private Headers buildHeaders(String deviceId, long timestamp, String sign, String token) {
        Headers.Builder builder = new Headers.Builder()
                .add("MT-Device-ID", deviceId)
                .add("MT-APP-Version", appVersion)
                .add("MT-Timestamp", String.valueOf(timestamp))
                .add("MT-Sign", sign)
                .add("User-Agent", "iOS/16.0 iMoutai/" + appVersion)
                .add("Content-Type", "application/json");
        if (token != null && !token.isEmpty()) {
            builder.add("MT-Token", token);
        }
        return builder.build();
    }

    /**
     * 签名：MD5(sorted_params_kv + "&" + timestamp + "&" + deviceId + "&" + APP_SECRET)
     */
    private String buildSign(TreeMap<String, String> params, long timestamp, String deviceId) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(e.getKey()).append("=").append(e.getValue());
        }
        if (sb.length() > 0) sb.append("&");
        sb.append(timestamp).append("&").append(deviceId).append("&").append(APP_SECRET);
        return DigestUtils.md5DigestAsHex(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void checkCode(JsonObject resp, String rawJson) {
        int code = resp.has("code") ? resp.get("code").getAsInt() : -1;
        if (code == 4003) {
            throw new BizException(ErrorCode.ACCOUNT_AUTH_EXPIRED);
        }
        if (code == 4029) {
            throw new BizException(ErrorCode.NO_STOCK);
        }
        if (code != 2000) {
            String msg = resp.has("message") ? resp.get("message").getAsString() : "未知错误";
            throw new BizException(ErrorCode.INTERNAL_ERROR, "i茅台接口返回错误: code=" + code + ", msg=" + msg);
        }
    }

    private String getStr(JsonObject obj, String key) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsString()
                : null;
    }
}
