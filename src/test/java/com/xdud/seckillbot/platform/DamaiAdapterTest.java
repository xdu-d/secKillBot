package com.xdud.seckillbot.platform;

import com.google.gson.Gson;
import com.xdud.seckillbot.common.exception.BizException;
import com.xdud.seckillbot.domain.enums.ExecutionResult;
import com.xdud.seckillbot.platform.impl.damai.DamaiAdapter;
import com.xdud.seckillbot.platform.impl.damai.DamaiCredential;
import com.xdud.seckillbot.platform.model.AuthContext;
import com.xdud.seckillbot.platform.model.OrderRequest;
import com.xdud.seckillbot.platform.model.OrderResult;
import com.xdud.seckillbot.platform.model.ProductInfo;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class DamaiAdapterTest {

    private MockWebServer mockWebServer;
    private DamaiAdapter adapter;
    private final Gson gson = new Gson();

    private static final String TEST_APP_KEY = "12574478";
    private static final String TEST_H5_TK = "abc123def456abc123def456abc12345_1700000000000";
    private static final String TEST_H5_TK_ENC = "encryptedValue";
    private static final String TEST_DEVICE_ID = "test-device-uuid-001";

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        OkHttpClient client = new OkHttpClient.Builder().build();
        adapter = new DamaiAdapter(client);
        ReflectionTestUtils.setField(adapter, "baseUrl", mockWebServer.url("/").toString().replaceAll("/$", ""));
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testSendSmsCode_success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"ret\":[\"SUCCESS::调用成功\"],\"data\":{}}")
                .setHeader("Content-Type", "application/json"));

        DamaiCredential cred = new DamaiCredential("13800138000", TEST_DEVICE_ID, "umid-token", TEST_APP_KEY, null, null);
        assertDoesNotThrow(() -> adapter.sendSmsCode("13800138000", cred));

        RecordedRequest req = mockWebServer.takeRequest();
        assertTrue(req.getPath().contains("ali.user.smscode.send"), "请求路径应含 ali.user.smscode.send");
        assertTrue(req.getPath().contains("sign="), "URL 应含 sign 参数");
        assertTrue(req.getPath().contains("appKey="), "URL 应含 appKey 参数");
        assertTrue(req.getPath().contains("t="), "URL 应含 t 参数");
    }

    @Test
    void testLoginWithSms_success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"ret\":[\"SUCCESS::调用成功\"],\"data\":{\"userId\":\"u888\"}}")
                .setHeader("Content-Type", "application/json")
                .addHeader("Set-Cookie", "_m_h5_tk=" + TEST_H5_TK + "; Path=/; Domain=.damai.cn")
                .addHeader("Set-Cookie", "_m_h5_tk_enc=" + TEST_H5_TK_ENC + "; Path=/; Domain=.damai.cn"));

        DamaiCredential cred = new DamaiCredential("13800138000", TEST_DEVICE_ID, "umid-token", TEST_APP_KEY, null, null);
        AuthContext ctx = adapter.loginWithSms("13800138000", "654321", cred);

        assertNotNull(ctx);
        assertEquals(TEST_H5_TK, ctx.getAccessToken());
        assertEquals("u888", ctx.getPlatformUserId());
        assertEquals(TEST_DEVICE_ID, ctx.getDeviceId());
        assertNotNull(ctx.getExpiresAt());
        assertTrue(ctx.getExpiresAt().isAfter(LocalDateTime.now()));
        assertEquals(TEST_H5_TK_ENC, ctx.getExtras().get("h5TkEnc"));
    }

    @Test
    void testPreOrderSetup_success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"ret\":[\"SUCCESS::调用成功\"],\"data\":{\"token\":\"lock-token-xyz\"}}")
                .setHeader("Content-Type", "application/json"));

        AuthContext auth = buildAuth();
        OrderRequest req = new OrderRequest();
        req.setProductId("proj-001");
        req.setQuantity(1);
        req.setAuthContext(auth);
        req.setExtras(new HashMap<String, String>() {{
            put("sessionId", "sess-abc");
            put("skuId", "sku-yyy");
        }});

        assertDoesNotThrow(() -> adapter.preOrderSetup(req));

        // 锁票 token 应写入 extras
        assertEquals("lock-token-xyz", req.getExtras().get("_orderToken"));

        RecordedRequest recorded = mockWebServer.takeRequest();
        String body = recorded.getBody().readUtf8();
        assertTrue(body.contains("sessionId") || body.contains("sess-abc"), "请求 data 应含 sessionId");
        assertTrue(body.contains("skuId") || body.contains("sku-yyy"), "请求 data 应含 skuId");
    }

    @Test
    void testSubmitOrder_success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"ret\":[\"SUCCESS::调用成功\"],\"data\":{\"orderId\":\"order-9527\"}}")
                .setHeader("Content-Type", "application/json"));

        AuthContext auth = buildAuth();
        OrderRequest req = new OrderRequest();
        req.setProductId("proj-001");
        req.setQuantity(1);
        req.setAuthContext(auth);
        req.setExtras(new HashMap<String, String>() {{
            put("_orderToken", "lock-token-xyz");
            put("buyerName", "张三");
            put("buyerPhone", "13800138000");
            put("buyerIdCard", "110101199001011234");
        }});

        OrderResult result = adapter.submitOrder(req);

        assertEquals(ExecutionResult.SUCCESS, result.getResult());
        assertEquals("order-9527", result.getOrderId());
    }

    @Test
    void testSubmitOrder_soldOut() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"ret\":[\"SOLD_OUT::已售罄\"],\"data\":{}}")
                .setHeader("Content-Type", "application/json"));

        AuthContext auth = buildAuth();
        OrderRequest req = new OrderRequest();
        req.setProductId("proj-001");
        req.setQuantity(1);
        req.setAuthContext(auth);
        req.setExtras(new HashMap<String, String>() {{
            put("_orderToken", "lock-token-xyz");
        }});

        OrderResult result = adapter.submitOrder(req);
        assertEquals(ExecutionResult.NO_STOCK, result.getResult());
    }

    @Test
    void testSubmitOrder_authExpired() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"ret\":[\"SESSION_EXPIRED::会话过期\"],\"data\":{}}")
                .setHeader("Content-Type", "application/json"));

        AuthContext auth = buildAuth();
        OrderRequest req = new OrderRequest();
        req.setProductId("proj-001");
        req.setQuantity(1);
        req.setAuthContext(auth);
        req.setExtras(new HashMap<String, String>() {{
            put("_orderToken", "lock-token-xyz");
        }});

        OrderResult result = adapter.submitOrder(req);
        assertEquals(ExecutionResult.AUTH_EXPIRED, result.getResult());
    }

    @Test
    void testSignAlgorithm() {
        // 白盒验证：已知输入验证 MD5 输出
        String h5TkFull = "abcdef1234567890abcdef1234567890_1700000000000";
        long t = 1700000001000L;
        String appKey = "12574478";
        String dataJson = "{\"projectId\":\"123\"}";

        String token = "abcdef1234567890abcdef1234567890";
        String raw = token + "&" + t + "&" + appKey + "&" + dataJson;
        String expected = DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));

        String actual = adapter.buildMtopSign(h5TkFull, t, appKey, dataJson);
        assertEquals(expected, actual);
    }

    @Test
    void testLogin_withExistingH5Tk() {
        String credJson = gson.toJson(new DamaiCredential(
                "13800138000", TEST_DEVICE_ID, "umid-token", TEST_APP_KEY, TEST_H5_TK, TEST_H5_TK_ENC));

        AuthContext ctx = adapter.login(credJson);

        assertEquals(TEST_H5_TK, ctx.getAccessToken());
        assertNotNull(ctx.getExpiresAt());
        assertTrue(ctx.getExpiresAt().isAfter(LocalDateTime.now()));
        // 无 HTTP 调用
        assertEquals(0, mockWebServer.getRequestCount());
    }

    @Test
    void testQueryProduct_success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"ret\":[\"SUCCESS::调用成功\"],\"data\":{"
                        + "\"item\":{\"itemName\":\"周杰伦演唱会\",\"status\":1},"
                        + "\"shows\":[{\"prices\":[{\"inventoryType\":1,\"price\":580}]}]}}")
                .setHeader("Content-Type", "application/json"));

        AuthContext auth = buildAuth();
        ProductInfo info = adapter.queryProduct("proj-001", auth);

        assertTrue(info.isInStock());
        assertEquals("周杰伦演唱会", info.getProductName());
    }

    // ──────────────────────────────────────────────────────────────────
    // 辅助
    // ──────────────────────────────────────────────────────────────────

    private AuthContext buildAuth() {
        AuthContext auth = new AuthContext();
        auth.setAccessToken(TEST_H5_TK);
        auth.setDeviceId(TEST_DEVICE_ID);
        auth.setExpiresAt(LocalDateTime.now().plusDays(1));
        auth.setExtras(new HashMap<String, String>() {{
            put("h5TkEnc", TEST_H5_TK_ENC);
            put("appKey", TEST_APP_KEY);
        }});
        return auth;
    }
}
