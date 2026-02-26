package com.xdud.seckillbot.platform.impl.maoyan;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
import java.util.HashMap;
import java.util.Map;

/**
 * 猫眼平台适配器（Phase 4 完整实现）。
 * 签名：MD5(appKey + timestamp + appSecret)，放入请求头 x-sign。
 */
@Component
public class MaoyanAdapter implements PlatformAdapter {

    private static final Logger log = LoggerFactory.getLogger(MaoyanAdapter.class);
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    @Value("${maoyan.base-url:https://api.maoyan.com}")
    private String baseUrl;

    private final OkHttpClient okHttpClient;
    private final Gson gson = new Gson();

    public MaoyanAdapter(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    // ──────────────────────────────────────────────────────────────────
    // PlatformAdapter SPI
    // ──────────────────────────────────────────────────────────────────

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.MAOYAN;
    }

    @Override
    public AuthContext login(String credentialJson) {
        MaoyanCredential cred = gson.fromJson(credentialJson, MaoyanCredential.class);
        if (cred.getToken() != null && !cred.getToken().isEmpty()) {
            AuthContext ctx = new AuthContext();
            ctx.setAccessToken(cred.getToken());
            ctx.setDeviceId(cred.getDeviceId());
            ctx.setPlatformUserId(cred.getUserId());
            ctx.setExpiresAt(LocalDateTime.now().plusDays(7));
            Map<String, String> extras = new HashMap<>();
            extras.put("appKey", cred.getAppKey() != null ? cred.getAppKey() : "");
            extras.put("appSecret", cred.getAppSecret() != null ? cred.getAppSecret() : "");
            extras.put("appVersion", cred.getAppVersion() != null ? cred.getAppVersion() : "9.2.0");
            ctx.setExtras(extras);
            return ctx;
        }
        throw new BizException(ErrorCode.ACCOUNT_AUTH_EXPIRED, "请先完成猫眼短信登录");
    }

    @Override
    public AuthContext refresh(AuthContext current) {
        if (current == null) {
            throw new BizException(ErrorCode.ACCOUNT_AUTH_EXPIRED, "认证上下文为空，需重新登录");
        }
        if (!needsRefresh(current)) {
            return current;
        }
        throw new BizException(ErrorCode.ACCOUNT_AUTH_EXPIRED, "猫眼 token 已过期，请重新短信登录");
    }

    @Override
    public boolean needsRefresh(AuthContext context) {
        if (context == null || context.getExpiresAt() == null) {
            return true;
        }
        return context.getExpiresAt().minusMinutes(30).isBefore(LocalDateTime.now());
    }

    @Override
    public ProductInfo queryProduct(String productId, AuthContext auth) {
        String respJson = get("/ticket/project/detail?projectId=" + productId, auth);
        JsonObject resp = JsonParser.parseString(respJson).getAsJsonObject();
        checkMaoyanStatus(resp, respJson);

        ProductInfo info = new ProductInfo();
        info.setProductId(productId);
        info.setRawJson(respJson);

        if (resp.has("data") && !resp.get("data").isJsonNull()) {
            JsonObject data = resp.getAsJsonObject("data");
            boolean projectOnSale = false;
            if (data.has("project") && !data.get("project").isJsonNull()) {
                JsonObject project = data.getAsJsonObject("project");
                info.setProductName(getStr(project, "projectName"));
                int status = project.has("status") ? project.get("status").getAsInt() : 0;
                projectOnSale = (status == 1);
            }
            // 仅当项目在售时，再检查 shows 的 prices inventoryType
            if (projectOnSale && data.has("shows") && !data.get("shows").isJsonNull()) {
                JsonArray shows = data.getAsJsonArray("shows");
                boolean anyStock = false;
                outer:
                for (int i = 0; i < shows.size(); i++) {
                    JsonObject show = shows.get(i).getAsJsonObject();
                    if (show.has("prices") && !show.get("prices").isJsonNull()) {
                        JsonArray prices = show.getAsJsonArray("prices");
                        for (int j = 0; j < prices.size(); j++) {
                            JsonObject price = prices.get(j).getAsJsonObject();
                            int invType = price.has("inventoryType") ? price.get("inventoryType").getAsInt() : 0;
                            if (invType == 1) {
                                anyStock = true;
                                break outer;
                            }
                        }
                    }
                }
                info.setInStock(anyStock);
            }
        }
        return info;
    }

    @Override
    public boolean hasStock(String productId, AuthContext auth) {
        return queryProduct(productId, auth).isInStock();
    }

    @Override
    public void preOrderSetup(OrderRequest request) {
        // 猫眼无需单独锁票步骤，直接提交订单
        log.debug("猫眼 preOrderSetup 无需操作，跳过");
    }

    @Override
    public OrderResult submitOrder(OrderRequest request) {
        Map<String, String> extras = request.getExtras();
        String showId = extras != null ? extras.get("showId") : null;
        String priceId = extras != null ? extras.get("priceId") : null;
        if (showId == null || showId.isEmpty() || priceId == null || priceId.isEmpty()) {
            return OrderResult.fail(ExecutionResult.FAILED, "extras 缺少 showId 或 priceId", null);
        }

        int quantity = request.getQuantity() > 0 ? request.getQuantity() : 1;
        String buyerName = extras.getOrDefault("buyerName", "");
        String buyerPhone = extras.getOrDefault("buyerPhone", "");
        String buyerIdCard = extras.getOrDefault("buyerIdCard", "");

        String bodyJson = gson.toJson(new HashMap<String, Object>() {{
            put("projectId", request.getProductId());
            put("showId", showId);
            put("priceId", priceId);
            put("quantity", quantity);
            put("seatType", "auto");
            put("buyerName", buyerName);
            put("buyerPhone", buyerPhone);
            put("buyerIdCard", buyerIdCard);
        }});

        String respJson;
        try {
            respJson = post("/ticket/order/submit", bodyJson, request.getAuthContext());
        } catch (Exception e) {
            log.error("猫眼下单请求异常", e);
            return OrderResult.fail(ExecutionResult.NETWORK_ERROR, e.getMessage(), null);
        }

        JsonObject resp = JsonParser.parseString(respJson).getAsJsonObject();
        int status = resp.has("status") ? resp.get("status").getAsInt() : -1;

        if (status == 0) {
            String orderId = null;
            if (resp.has("data") && !resp.get("data").isJsonNull()) {
                orderId = getStr(resp.getAsJsonObject("data"), "orderId");
            }
            log.info("猫眼下单成功，orderId={}", orderId);
            return OrderResult.success(orderId != null ? orderId : "unknown", respJson);
        } else if (status == 10001 || status == 10002) {
            return OrderResult.fail(ExecutionResult.AUTH_EXPIRED, "认证已过期: " + status, respJson);
        } else if (status == 30001) {
            return OrderResult.fail(ExecutionResult.NO_STOCK, "已无库存", respJson);
        } else {
            String msg = getStr(resp, "message");
            return OrderResult.fail(ExecutionResult.FAILED, "下单失败: " + (msg != null ? msg : status), respJson);
        }
    }

    @Override
    public String queryOrderStatus(String orderId, AuthContext auth) {
        String respJson = get("/ticket/order/detail?orderId=" + orderId, auth);
        return respJson;
    }

    // ──────────────────────────────────────────────────────────────────
    // 公开方法：供 AccountService 调用的双步短信登录
    // ──────────────────────────────────────────────────────────────────

    /**
     * 发送猫眼短信验证码。
     */
    public void sendSmsCode(String phone, MaoyanCredential cred) {
        String bodyJson = "{\"mobile\":\"" + phone + "\",\"areaCode\":\"86\"}";
        String respJson = post("/user/login/sendSmsCode", bodyJson, buildUnauthContext(cred));
        JsonObject resp = JsonParser.parseString(respJson).getAsJsonObject();
        checkMaoyanStatus(resp, respJson);
        log.info("猫眼发送验证码成功，phone={}", phone);
    }

    /**
     * 使用短信验证码登录猫眼，返回 AuthContext。
     */
    public AuthContext loginWithSms(String phone, String code, MaoyanCredential cred) {
        String bodyJson = "{\"mobile\":\"" + phone + "\","
                + "\"smsCode\":\"" + code + "\","
                + "\"areaCode\":\"86\","
                + "\"deviceId\":\"" + (cred.getDeviceId() != null ? cred.getDeviceId() : "") + "\"}";
        String respJson = post("/user/login/smsLogin", bodyJson, buildUnauthContext(cred));
        JsonObject resp = JsonParser.parseString(respJson).getAsJsonObject();
        checkMaoyanStatus(resp, respJson);

        String token = null;
        String userId = null;
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        if (resp.has("data") && !resp.get("data").isJsonNull()) {
            JsonObject data = resp.getAsJsonObject("data");
            token = getStr(data, "token");
            userId = getStr(data, "userId");
            if (data.has("expireTime") && !data.get("expireTime").isJsonNull()) {
                long expireMs = data.get("expireTime").getAsLong();
                expiresAt = LocalDateTime.ofEpochSecond(expireMs / 1000, 0,
                        java.time.ZoneOffset.ofHours(8));
            }
        }

        AuthContext ctx = new AuthContext();
        ctx.setAccessToken(token != null ? token : "");
        ctx.setDeviceId(cred.getDeviceId());
        ctx.setPlatformUserId(userId);
        ctx.setExpiresAt(expiresAt);
        Map<String, String> extras = new HashMap<>();
        extras.put("appKey", cred.getAppKey() != null ? cred.getAppKey() : "");
        extras.put("appSecret", cred.getAppSecret() != null ? cred.getAppSecret() : "");
        extras.put("appVersion", cred.getAppVersion() != null ? cred.getAppVersion() : "9.2.0");
        ctx.setExtras(extras);
        log.info("猫眼短信登录成功，userId={}", userId);
        return ctx;
    }

    // ──────────────────────────────────────────────────────────────────
    // 私有辅助方法
    // ──────────────────────────────────────────────────────────────────

    private String post(String path, String jsonBody, AuthContext auth) {
        RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA);
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .headers(buildHeaders(auth))
                .post(body)
                .build();
        return execute(request);
    }

    private String get(String path, AuthContext auth) {
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .headers(buildHeaders(auth))
                .get()
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

    private Headers buildHeaders(AuthContext auth) {
        long ts = System.currentTimeMillis();
        String appKey = "";
        String appSecret = "";
        String appVersion = "9.2.0";
        String deviceId = "";
        String accessToken = null;

        if (auth != null) {
            deviceId = auth.getDeviceId() != null ? auth.getDeviceId() : "";
            accessToken = auth.getAccessToken();
            if (auth.getExtras() != null) {
                appKey = auth.getExtras().getOrDefault("appKey", "");
                appSecret = auth.getExtras().getOrDefault("appSecret", "");
                appVersion = auth.getExtras().getOrDefault("appVersion", "9.2.0");
            }
        }

        String sign = buildMaoyanSign(appKey, ts, appSecret);

        Headers.Builder builder = new Headers.Builder()
                .add("User-Agent", "MaoYanMovie/" + appVersion + " (iPhone; iOS 15.0)")
                .add("Content-Type", "application/json")
                .add("x-app-key", appKey)
                .add("x-timestamp", String.valueOf(ts))
                .add("x-sign", sign)
                .add("x-device-id", deviceId)
                .add("x-app-version", appVersion);

        if (accessToken != null && !accessToken.isEmpty()) {
            builder.add("Authorization", "Bearer " + accessToken);
        }
        return builder.build();
    }

    /**
     * MD5(appKey + timestamp + appSecret)
     */
    String buildMaoyanSign(String appKey, long timestamp, String appSecret) {
        String raw = (appKey != null ? appKey : "") + timestamp + (appSecret != null ? appSecret : "");
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    private void checkMaoyanStatus(JsonObject resp, String rawJson) {
        int status = resp.has("status") ? resp.get("status").getAsInt() : -1;
        if (status == 0) return;
        if (status == 10001 || status == 10002) {
            throw new BizException(ErrorCode.ACCOUNT_AUTH_EXPIRED, "猫眼认证已过期: " + status);
        }
        if (status == 30001) {
            throw new BizException(ErrorCode.NO_STOCK, "猫眼无库存");
        }
        String msg = getStr(resp, "message");
        throw new BizException(ErrorCode.INTERNAL_ERROR, "猫眼接口错误: status=" + status
                + (msg != null ? ", " + msg : ""));
    }

    /** 构建未认证状态的 AuthContext（仅携带 credential 中的 appKey 等） */
    private AuthContext buildUnauthContext(MaoyanCredential cred) {
        AuthContext ctx = new AuthContext();
        ctx.setDeviceId(cred.getDeviceId());
        Map<String, String> extras = new HashMap<>();
        extras.put("appKey", cred.getAppKey() != null ? cred.getAppKey() : "");
        extras.put("appSecret", cred.getAppSecret() != null ? cred.getAppSecret() : "");
        extras.put("appVersion", cred.getAppVersion() != null ? cred.getAppVersion() : "9.2.0");
        ctx.setExtras(extras);
        return ctx;
    }

    private String getStr(JsonObject obj, String key) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsString()
                : null;
    }
}
