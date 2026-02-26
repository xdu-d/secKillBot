package com.xdud.seckillbot.platform;

import com.google.gson.Gson;
import com.xdud.seckillbot.domain.enums.ExecutionResult;
import com.xdud.seckillbot.platform.impl.maoyan.MaoyanAdapter;
import com.xdud.seckillbot.platform.impl.maoyan.MaoyanCredential;
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

class MaoyanAdapterTest {

    private MockWebServer mockWebServer;
    private MaoyanAdapter adapter;
    private final Gson gson = new Gson();

    private static final String TEST_APP_KEY = "maoyan-app-key-test";
    private static final String TEST_APP_SECRET = "maoyan-app-secret-test";
    private static final String TEST_APP_VERSION = "9.2.0";
    private static final String TEST_DEVICE_ID = "maoyan-device-uuid-001";
    private static final String TEST_TOKEN = "maoyan-bearer-token-xyz";

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        OkHttpClient client = new OkHttpClient.Builder().build();
        adapter = new MaoyanAdapter(client);
        ReflectionTestUtils.setField(adapter, "baseUrl", mockWebServer.url("/").toString().replaceAll("/$", ""));
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testSendSmsCode_success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"status\":0,\"message\":\"ok\"}")
                .setHeader("Content-Type", "application/json"));

        MaoyanCredential cred = new MaoyanCredential(
                "13800138000", TEST_DEVICE_ID, TEST_APP_VERSION, TEST_APP_KEY, TEST_APP_SECRET, null, null);
        assertDoesNotThrow(() -> adapter.sendSmsCode("13800138000", cred));

        RecordedRequest req = mockWebServer.takeRequest();
        assertNotNull(req.getHeader("x-app-key"), "应含 x-app-key 请求头");
        assertNotNull(req.getHeader("x-sign"), "应含 x-sign 请求头");
        assertNotNull(req.getHeader("x-timestamp"), "应含 x-timestamp 请求头");
        assertNotNull(req.getHeader("x-device-id"), "应含 x-device-id 请求头");
    }

    @Test
    void testLoginWithSms_success() throws InterruptedException {
        long expireMs = System.currentTimeMillis() + 7L * 24 * 3600 * 1000;
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"status\":0,\"data\":{\"token\":\"" + TEST_TOKEN + "\","
                        + "\"userId\":\"u-maoyan-001\","
                        + "\"expireTime\":" + expireMs + "}}")
                .setHeader("Content-Type", "application/json"));

        MaoyanCredential cred = new MaoyanCredential(
                "13800138000", TEST_DEVICE_ID, TEST_APP_VERSION, TEST_APP_KEY, TEST_APP_SECRET, null, null);
        AuthContext ctx = adapter.loginWithSms("13800138000", "112233", cred);

        assertNotNull(ctx);
        assertEquals(TEST_TOKEN, ctx.getAccessToken());
        assertEquals("u-maoyan-001", ctx.getPlatformUserId());
        assertNotNull(ctx.getExpiresAt());
        assertTrue(ctx.getExpiresAt().isAfter(LocalDateTime.now()));
        assertEquals(TEST_APP_KEY, ctx.getExtras().get("appKey"));
    }

    @Test
    void testSubmitOrder_success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"status\":0,\"data\":{\"orderId\":\"order-maoyan-001\"}}")
                .setHeader("Content-Type", "application/json"));

        OrderResult result = adapter.submitOrder(buildOrderRequest());

        assertEquals(ExecutionResult.SUCCESS, result.getResult());
        assertEquals("order-maoyan-001", result.getOrderId());
    }

    @Test
    void testSubmitOrder_noTicket() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"status\":30001,\"message\":\"已售罄\"}")
                .setHeader("Content-Type", "application/json"));

        OrderResult result = adapter.submitOrder(buildOrderRequest());
        assertEquals(ExecutionResult.NO_STOCK, result.getResult());
    }

    @Test
    void testSubmitOrder_authExpired() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"status\":10001,\"message\":\"认证过期\"}")
                .setHeader("Content-Type", "application/json"));

        OrderResult result = adapter.submitOrder(buildOrderRequest());
        assertEquals(ExecutionResult.AUTH_EXPIRED, result.getResult());
    }

    @Test
    void testSubmitOrder_missingExtras() {
        AuthContext auth = buildAuth();
        OrderRequest req = new OrderRequest();
        req.setProductId("proj-001");
        req.setQuantity(1);
        req.setAuthContext(auth);
        req.setExtras(new HashMap<>()); // 缺 showId / priceId

        OrderResult result = adapter.submitOrder(req);

        assertEquals(ExecutionResult.FAILED, result.getResult());
        // 无 HTTP 调用
        assertEquals(0, mockWebServer.getRequestCount());
    }

    @Test
    void testPreOrderSetup_isNoop() {
        AuthContext auth = buildAuth();
        OrderRequest req = new OrderRequest();
        req.setProductId("proj-001");
        req.setQuantity(1);
        req.setAuthContext(auth);
        req.setExtras(new HashMap<>());

        // 无异常，无 HTTP 调用
        assertDoesNotThrow(() -> adapter.preOrderSetup(req));
        assertEquals(0, mockWebServer.getRequestCount());
    }

    @Test
    void testQueryProduct_withStock() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"status\":0,\"data\":{"
                        + "\"project\":{\"projectName\":\"五月天演唱会\",\"status\":1},"
                        + "\"shows\":[{\"prices\":[{\"inventoryType\":1,\"price\":680}]}]}}")
                .setHeader("Content-Type", "application/json"));

        ProductInfo info = adapter.queryProduct("proj-001", buildAuth());

        assertTrue(info.isInStock());
        assertEquals("五月天演唱会", info.getProductName());
    }

    @Test
    void testQueryProduct_noStock() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"status\":0,\"data\":{"
                        + "\"project\":{\"projectName\":\"五月天演唱会\",\"status\":1},"
                        + "\"shows\":[{\"prices\":[{\"inventoryType\":0},{\"inventoryType\":0}]}]}}")
                .setHeader("Content-Type", "application/json"));

        ProductInfo info = adapter.queryProduct("proj-001", buildAuth());

        assertFalse(info.isInStock());
    }

    @Test
    void testLogin_withToken() {
        String credJson = gson.toJson(new MaoyanCredential(
                "13800138000", TEST_DEVICE_ID, TEST_APP_VERSION,
                TEST_APP_KEY, TEST_APP_SECRET, TEST_TOKEN, "u-maoyan-001"));

        AuthContext ctx = adapter.login(credJson);

        assertEquals(TEST_TOKEN, ctx.getAccessToken());
        assertEquals("u-maoyan-001", ctx.getPlatformUserId());
        assertNotNull(ctx.getExpiresAt());
        // 无 HTTP 调用
        assertEquals(0, mockWebServer.getRequestCount());
    }

    // ──────────────────────────────────────────────────────────────────
    // 辅助
    // ──────────────────────────────────────────────────────────────────

    private AuthContext buildAuth() {
        AuthContext auth = new AuthContext();
        auth.setAccessToken(TEST_TOKEN);
        auth.setDeviceId(TEST_DEVICE_ID);
        auth.setExpiresAt(LocalDateTime.now().plusDays(1));
        auth.setExtras(new HashMap<String, String>() {{
            put("appKey", TEST_APP_KEY);
            put("appSecret", TEST_APP_SECRET);
            put("appVersion", TEST_APP_VERSION);
        }});
        return auth;
    }

    private OrderRequest buildOrderRequest() {
        AuthContext auth = buildAuth();
        OrderRequest req = new OrderRequest();
        req.setProductId("proj-001");
        req.setQuantity(2);
        req.setAuthContext(auth);
        req.setExtras(new HashMap<String, String>() {{
            put("showId", "show-xxx");
            put("priceId", "price-yyy");
            put("buyerName", "李四");
            put("buyerPhone", "13900139000");
            put("buyerIdCard", "110101199002022345");
        }});
        return req;
    }
}
