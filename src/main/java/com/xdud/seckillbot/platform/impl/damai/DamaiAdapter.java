package com.xdud.seckillbot.platform.impl.damai;

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
 * 大麦网平台适配器（Phase 4 完整实现）。
 * 基于阿里 mtop 框架：签名=MD5(h5TkToken32位 + "&" + t + "&" + appKey + "&" + dataJson)。
 */
@Component
public class DamaiAdapter implements PlatformAdapter {

    private static final Logger log = LoggerFactory.getLogger(DamaiAdapter.class);
    private static final MediaType FORM_MEDIA = MediaType.get("application/x-www-form-urlencoded");
    private static final String JSV = "2.6.2";
    private static final String API_TYPE = "originaljson";

    @Value("${damai.base-url:https://mtop.damai.cn}")
    private String baseUrl;

    private final OkHttpClient okHttpClient;
    private final Gson gson = new Gson();

    public DamaiAdapter(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    // ──────────────────────────────────────────────────────────────────
    // PlatformAdapter SPI
    // ──────────────────────────────────────────────────────────────────

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.DAMAI;
    }

    @Override
    public AuthContext login(String credentialJson) {
        DamaiCredential cred = gson.fromJson(credentialJson, DamaiCredential.class);
        if (cred.getH5Tk() != null && !cred.getH5Tk().isEmpty()) {
            AuthContext ctx = new AuthContext();
            ctx.setAccessToken(cred.getH5Tk());
            ctx.setDeviceId(cred.getDeviceId());
            ctx.setExpiresAt(LocalDateTime.now().plusDays(7));
            Map<String, String> extras = new HashMap<>();
            extras.put("h5TkEnc", cred.getH5TkEnc() != null ? cred.getH5TkEnc() : "");
            extras.put("appKey", cred.getAppKey() != null ? cred.getAppKey() : "");
            ctx.setExtras(extras);
            return ctx;
        }
        throw new BizException(ErrorCode.ACCOUNT_AUTH_EXPIRED, "请先完成大麦短信登录");
    }

    @Override
    public AuthContext refresh(AuthContext current) {
        if (current == null) {
            throw new BizException(ErrorCode.ACCOUNT_AUTH_EXPIRED, "认证上下文为空，需重新登录");
        }
        if (!needsRefresh(current)) {
            return current;
        }
        throw new BizException(ErrorCode.ACCOUNT_AUTH_EXPIRED, "大麦 h5Tk 已过期，请重新短信登录");
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
        String appKey = getAppKey(auth);
        String h5TkFull = auth.getAccessToken();

        String dataJson = "{\"projectId\":\"" + productId + "\",\"platform\":\"1\"}";
        JsonObject resp = postMtop("mtop.damai.ticket.project.get", "1.0", dataJson, appKey, h5TkFull, auth);
        checkMtopResult(resp, dataJson);

        ProductInfo info = new ProductInfo();
        info.setProductId(productId);
        info.setRawJson(resp.toString());

        if (resp.has("data") && !resp.get("data").isJsonNull()) {
            JsonObject data = resp.getAsJsonObject("data");
            // 检查 item status
            if (data.has("item") && !data.get("item").isJsonNull()) {
                JsonObject item = data.getAsJsonObject("item");
                info.setProductName(getStr(item, "itemName"));
                int status = item.has("status") ? item.get("status").getAsInt() : 0;
                info.setInStock(status == 1);
            }
            // 进一步检查 shows 的 prices inventoryType
            if (data.has("shows") && !data.get("shows").isJsonNull()) {
                JsonArray shows = data.getAsJsonArray("shows");
                for (int i = 0; i < shows.size(); i++) {
                    JsonObject show = shows.get(i).getAsJsonObject();
                    if (show.has("prices") && !show.get("prices").isJsonNull()) {
                        JsonArray prices = show.getAsJsonArray("prices");
                        for (int j = 0; j < prices.size(); j++) {
                            JsonObject price = prices.get(j).getAsJsonObject();
                            int invType = price.has("inventoryType") ? price.get("inventoryType").getAsInt() : 0;
                            if (invType == 1) {
                                info.setInStock(true);
                                break;
                            }
                        }
                    }
                    if (info.isInStock()) break;
                }
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
        AuthContext auth = request.getAuthContext();
        String appKey = getAppKey(auth);
        String h5TkFull = auth.getAccessToken();
        Map<String, String> extras = request.getExtras();

        String sessionId = extras != null ? extras.get("sessionId") : null;
        String skuId = extras != null ? extras.get("skuId") : null;
        int buyNum = request.getQuantity() > 0 ? request.getQuantity() : 1;
        if (extras != null && extras.containsKey("buyNum")) {
            try { buyNum = Integer.parseInt(extras.get("buyNum")); } catch (NumberFormatException ignored) {}
        }

        String dataJson = "{\"projectId\":\"" + request.getProductId() + "\","
                + "\"sessionId\":\"" + (sessionId != null ? sessionId : "") + "\","
                + "\"skuList\":[{\"skuId\":\"" + (skuId != null ? skuId : "") + "\",\"num\":" + buyNum + "}],"
                + "\"buyNow\":\"1\",\"platform\":\"1\"}";

        JsonObject resp = postMtop("mtop.damai.ticket.order.create", "1.0", dataJson, appKey, h5TkFull, auth);
        checkMtopResult(resp, dataJson);

        String orderToken = null;
        if (resp.has("data") && !resp.get("data").isJsonNull()) {
            JsonObject data = resp.getAsJsonObject("data");
            orderToken = getStr(data, "token");
        }
        if (orderToken != null && !orderToken.isEmpty()) {
            if (extras != null) {
                extras.put("_orderToken", orderToken);
            }
            log.info("大麦锁票成功，orderToken={}", orderToken);
        } else {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "大麦锁票未返回 token");
        }
    }

    @Override
    public OrderResult submitOrder(OrderRequest request) {
        Map<String, String> extras = request.getExtras();
        String orderToken = extras != null ? extras.get("_orderToken") : null;
        if (orderToken == null || orderToken.isEmpty()) {
            return OrderResult.fail(ExecutionResult.FAILED, "缺少锁票 token，请先调用 preOrderSetup", null);
        }

        AuthContext auth = request.getAuthContext();
        String appKey = getAppKey(auth);
        String h5TkFull = auth.getAccessToken();

        String buyerName = extras.getOrDefault("buyerName", "");
        String buyerPhone = extras.getOrDefault("buyerPhone", "");
        String buyerIdCard = extras.getOrDefault("buyerIdCard", "");

        String dataJson = "{\"token\":\"" + orderToken + "\","
                + "\"purchaser\":{\"name\":\"" + buyerName + "\","
                + "\"mobile\":\"" + buyerPhone + "\","
                + "\"idCardNo\":\"" + buyerIdCard + "\"},"
                + "\"platform\":\"1\"}";

        JsonObject resp;
        try {
            resp = postMtop("mtop.damai.ticket.order.confirm", "1.0", dataJson, appKey, h5TkFull, auth);
        } catch (BizException e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("SESSION_EXPIRED") || msg.contains("TOKEN_INVALID"))) {
                return OrderResult.fail(ExecutionResult.AUTH_EXPIRED, msg, null);
            }
            if (msg != null && (msg.contains("SOLD_OUT") || msg.contains("NO_TICKET"))) {
                return OrderResult.fail(ExecutionResult.NO_STOCK, msg, null);
            }
            return OrderResult.fail(ExecutionResult.FAILED, msg, null);
        }

        String retStr = getRet(resp);
        if (retStr != null && (retStr.contains("SESSION_EXPIRED") || retStr.contains("TOKEN_INVALID"))) {
            return OrderResult.fail(ExecutionResult.AUTH_EXPIRED, "认证已过期: " + retStr, resp.toString());
        }
        if (retStr != null && (retStr.contains("SOLD_OUT") || retStr.contains("NO_TICKET"))) {
            return OrderResult.fail(ExecutionResult.NO_STOCK, "已无库存: " + retStr, resp.toString());
        }
        if (retStr != null && !retStr.startsWith("SUCCESS")) {
            return OrderResult.fail(ExecutionResult.FAILED, "下单失败: " + retStr, resp.toString());
        }

        String orderId = null;
        if (resp.has("data") && !resp.get("data").isJsonNull()) {
            JsonObject data = resp.getAsJsonObject("data");
            orderId = getStr(data, "orderId");
        }
        log.info("大麦下单成功，orderId={}", orderId);
        return OrderResult.success(orderId != null ? orderId : "unknown", resp.toString());
    }

    @Override
    public String queryOrderStatus(String orderId, AuthContext auth) {
        String appKey = getAppKey(auth);
        String h5TkFull = auth.getAccessToken();
        String dataJson = "{\"orderId\":\"" + orderId + "\"}";
        JsonObject resp = postMtop("mtop.damai.ticket.order.detail", "1.0", dataJson, appKey, h5TkFull, auth);
        return resp.toString();
    }

    // ──────────────────────────────────────────────────────────────────
    // 公开方法：供 AccountService 调用的双步短信登录
    // ──────────────────────────────────────────────────────────────────

    /**
     * 发送大麦短信验证码（未登录，h5Tk 为空字符串）。
     */
    public void sendSmsCode(String phone, DamaiCredential cred) {
        String appKey = cred.getAppKey() != null ? cred.getAppKey() : "";
        String dataJson = "{\"mobile\":\"" + phone + "\",\"countryCode\":\"86\",\"type\":\"login\"}";
        JsonObject resp = postMtop("ali.user.smscode.send", "1.0.0", dataJson, appKey, "", null);
        checkMtopResult(resp, dataJson);
        log.info("大麦发送验证码成功，phone={}", phone);
    }

    /**
     * 使用短信验证码登录大麦，返回 AuthContext。
     * h5Tk 和 h5TkEnc 从响应 Set-Cookie 解析。
     */
    public AuthContext loginWithSms(String phone, String code, DamaiCredential cred) {
        String appKey = cred.getAppKey() != null ? cred.getAppKey() : "";
        String deviceId = cred.getDeviceId() != null ? cred.getDeviceId() : "";
        String umidToken = cred.getUmidToken() != null ? cred.getUmidToken() : "";

        String dataJson = "{\"mobile\":\"" + phone + "\","
                + "\"smsCode\":\"" + code + "\","
                + "\"countryCode\":\"86\","
                + "\"deviceId\":\"" + deviceId + "\","
                + "\"umidToken\":\"" + umidToken + "\"}";

        long t = System.currentTimeMillis();
        String sign = buildMtopSign("", t, appKey, dataJson);
        Headers headers = buildHeaders("", "", deviceId);

        HttpUrl url = buildMtopUrl("ali.user.login.smscode", "1.0.0", appKey, t, sign);
        RequestBody body = new FormBody.Builder().add("data", dataJson).build();
        Request request = new Request.Builder().url(url).headers(headers).post(body).build();

        String h5Tk = null;
        String h5TkEnc = null;
        String userId = null;
        String rawJson;

        try (Response response = okHttpClient.newCall(request).execute()) {
            // 从 Set-Cookie 提取 h5Tk / h5TkEnc
            for (String cookie : response.headers("Set-Cookie")) {
                if (cookie.startsWith("_m_h5_tk=")) {
                    h5Tk = cookie.split(";")[0].substring("_m_h5_tk=".length());
                } else if (cookie.startsWith("_m_h5_tk_enc=")) {
                    h5TkEnc = cookie.split(";")[0].substring("_m_h5_tk_enc=".length());
                }
            }
            ResponseBody respBody = response.body();
            rawJson = respBody != null ? respBody.string() : "{}";
        } catch (IOException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "大麦登录网络异常: " + e.getMessage());
        }

        JsonObject resp = JsonParser.parseString(rawJson).getAsJsonObject();
        checkMtopResult(resp, rawJson);

        if (resp.has("data") && !resp.get("data").isJsonNull()) {
            JsonObject data = resp.getAsJsonObject("data");
            userId = getStr(data, "userId");
        }

        AuthContext ctx = new AuthContext();
        ctx.setAccessToken(h5Tk != null ? h5Tk : "");
        ctx.setDeviceId(deviceId);
        ctx.setPlatformUserId(userId);
        ctx.setExpiresAt(LocalDateTime.now().plusDays(7));
        Map<String, String> extras = new HashMap<>();
        extras.put("h5TkEnc", h5TkEnc != null ? h5TkEnc : "");
        extras.put("appKey", appKey);
        ctx.setExtras(extras);
        log.info("大麦短信登录成功，userId={}", userId);
        return ctx;
    }

    // ──────────────────────────────────────────────────────────────────
    // 私有辅助方法
    // ──────────────────────────────────────────────────────────────────

    /**
     * 发送 mtop 接口请求，返回响应 JsonObject。
     * auth 为 null 时表示未登录（如发短信）。
     */
    private JsonObject postMtop(String api, String version, String dataJson,
                                String appKey, String h5TkFull, AuthContext auth) {
        long t = System.currentTimeMillis();
        String sign = buildMtopSign(h5TkFull, t, appKey, dataJson);

        String deviceId = (auth != null && auth.getDeviceId() != null) ? auth.getDeviceId() : "";
        String h5TkEnc = "";
        if (auth != null && auth.getExtras() != null) {
            h5TkEnc = auth.getExtras().getOrDefault("h5TkEnc", "");
        }

        Headers headers = buildHeaders(h5TkFull, h5TkEnc, deviceId);
        HttpUrl url = buildMtopUrl(api, version, appKey, t, sign);
        RequestBody body = new FormBody.Builder().add("data", dataJson).build();
        Request request = new Request.Builder().url(url).headers(headers).post(body).build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            ResponseBody respBody = response.body();
            String rawJson = respBody != null ? respBody.string() : "{}";
            return JsonParser.parseString(rawJson).getAsJsonObject();
        } catch (IOException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "大麦接口网络异常: " + e.getMessage());
        }
    }

    /**
     * MD5(h5TkFull.split("_")[0] + "&" + t + "&" + appKey + "&" + dataJson)
     * 未登录时 token 部分为空字符串。
     */
    public String buildMtopSign(String h5TkFull, long t, String appKey, String dataJson) {
        String token = "";
        if (h5TkFull != null && !h5TkFull.isEmpty()) {
            String[] parts = h5TkFull.split("_");
            token = parts[0];
        }
        String raw = token + "&" + t + "&" + appKey + "&" + dataJson;
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    private HttpUrl buildMtopUrl(String api, String version, String appKey, long t, String sign) {
        return HttpUrl.parse(baseUrl + "/h5/" + api + "/" + version + "/")
                .newBuilder()
                .addQueryParameter("jsv", JSV)
                .addQueryParameter("appKey", appKey)
                .addQueryParameter("t", String.valueOf(t))
                .addQueryParameter("sign", sign)
                .addQueryParameter("type", API_TYPE)
                .addQueryParameter("dataType", "json")
                .build();
    }

    private Headers buildHeaders(String h5TkFull, String h5TkEnc, String deviceId) {
        return new Headers.Builder()
                .add("User-Agent", "MTOPSDK/3.2.0 (iPhone; iOS 15.0) DaMai/10.x.x")
                .add("Cookie", "_m_h5_tk=" + (h5TkFull != null ? h5TkFull : "")
                        + "; _m_h5_tk_enc=" + (h5TkEnc != null ? h5TkEnc : "")
                        + "; cna=" + (deviceId != null ? deviceId : "") + ";")
                .add("Content-Type", "application/x-www-form-urlencoded")
                .add("Referer", "https://m.damai.cn/")
                .build();
    }

    private void checkMtopResult(JsonObject resp, String context) {
        if (!resp.has("ret") || resp.get("ret").isJsonNull()) {
            return;
        }
        JsonArray ret = resp.getAsJsonArray("ret");
        if (ret.size() == 0) return;
        String retStr = ret.get(0).getAsString();
        if (retStr.startsWith("SUCCESS")) return;
        if (retStr.contains("SESSION_EXPIRED") || retStr.contains("TOKEN_INVALID")) {
            throw new BizException(ErrorCode.ACCOUNT_AUTH_EXPIRED, "大麦认证已过期: " + retStr);
        }
        if (retStr.contains("SOLD_OUT") || retStr.contains("NO_TICKET")) {
            throw new BizException(ErrorCode.NO_STOCK, "大麦无库存: " + retStr);
        }
        throw new BizException(ErrorCode.INTERNAL_ERROR, "大麦接口错误: " + retStr);
    }

    private String getRet(JsonObject resp) {
        if (!resp.has("ret") || resp.get("ret").isJsonNull()) return null;
        JsonArray ret = resp.getAsJsonArray("ret");
        if (ret.size() == 0) return null;
        return ret.get(0).getAsString();
    }

    private String getAppKey(AuthContext auth) {
        if (auth != null && auth.getExtras() != null) {
            String appKey = auth.getExtras().get("appKey");
            if (appKey != null && !appKey.isEmpty()) return appKey;
        }
        return "";
    }

    private String getStr(JsonObject obj, String key) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsString()
                : null;
    }
}
