package com.xdud.seckillbot.platform;

import com.google.gson.Gson;
import com.xdud.seckillbot.platform.impl.imoutai.ImoutaiAdapter;
import com.xdud.seckillbot.platform.model.AuthContext;
import com.xdud.seckillbot.platform.model.OrderRequest;
import com.xdud.seckillbot.platform.model.OrderResult;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ImoutaiAdapterTest {

    private MockWebServer mockWebServer;
    private ImoutaiAdapter adapter;
    private final Gson gson = new Gson();

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        OkHttpClient client = new OkHttpClient.Builder().build();
        adapter = new ImoutaiAdapter(client);
        ReflectionTestUtils.setField(adapter, "baseUrl", mockWebServer.url("/").toString().replaceAll("/$", ""));
        ReflectionTestUtils.setField(adapter, "appVersion", "1.3.7");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testSendSmsCode_success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"code\":2000,\"message\":\"ok\",\"data\":{}}")
                .setHeader("Content-Type", "application/json"));

        assertDoesNotThrow(() -> adapter.sendSmsCode("13800138000", UUID.randomUUID().toString()));

        RecordedRequest req = mockWebServer.takeRequest();
        assertTrue(req.getPath().contains("/xhr/front/user/register/mobileCode"));
        assertNotNull(req.getHeader("MT-Device-ID"));
        assertNotNull(req.getHeader("MT-Sign"));
        assertNotNull(req.getHeader("MT-Timestamp"));
    }

    @Test
    void testLoginWithSms_success() throws InterruptedException {
        String deviceId = UUID.randomUUID().toString();
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"code\":2000,\"message\":\"ok\",\"data\":{\"token\":\"tok123\",\"userId\":\"u456\"}}")
                .setHeader("Content-Type", "application/json"));

        AuthContext ctx = adapter.loginWithSms("13800138000", "123456", deviceId);

        assertNotNull(ctx);
        assertEquals("tok123", ctx.getAccessToken());
        assertEquals("u456", ctx.getPlatformUserId());
        assertEquals(deviceId, ctx.getDeviceId());
        assertNotNull(ctx.getExpiresAt());
        assertTrue(ctx.getExpiresAt().isAfter(LocalDateTime.now()));

        RecordedRequest req = mockWebServer.takeRequest();
        assertTrue(req.getPath().contains("/xhr/front/user/register/loginByMobile"));
    }

    @Test
    void testSubmitOrder_success() throws InterruptedException {
        // 1. mock 期次查询
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"code\":2000,\"data\":{\"sessionId\":\"s999\"}}")
                .setHeader("Content-Type", "application/json"));
        // 2. mock 预约成功
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"code\":2000,\"data\":{\"reservationId\":\"r001\"}}")
                .setHeader("Content-Type", "application/json"));

        AuthContext auth = buildAuth();
        OrderRequest req = new OrderRequest();
        req.setProductId("10023");
        req.setQuantity(1);
        req.setAuthContext(auth);
        req.setExtras(new HashMap<String, String>() {{ put("shopId", "shop001"); }});

        OrderResult result = adapter.submitOrder(req);

        assertEquals("r001", result.getOrderId());
        assertNotNull(result.getResult());
        assertEquals("success", result.getResult().getValue());
    }

    @Test
    void testSubmitOrder_noStock() throws InterruptedException {
        // 1. mock 期次查询
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"code\":2000,\"data\":{\"sessionId\":\"s999\"}}")
                .setHeader("Content-Type", "application/json"));
        // 2. mock 预约返回 4029 无库存
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"code\":4029,\"message\":\"已申购\"}")
                .setHeader("Content-Type", "application/json"));

        AuthContext auth = buildAuth();
        OrderRequest req = new OrderRequest();
        req.setProductId("10023");
        req.setQuantity(1);
        req.setAuthContext(auth);
        req.setExtras(new HashMap<String, String>() {{ put("shopId", "shop001"); }});

        OrderResult result = adapter.submitOrder(req);

        assertEquals("no_stock", result.getResult().getValue());
    }

    @Test
    void testLogin_tokenProvided() {
        String credJson = "{\"phone\":\"13800138000\",\"userId\":\"u001\",\"token\":\"mytoken\",\"deviceId\":\"dev001\"}";

        AuthContext ctx = adapter.login(credJson);

        assertEquals("mytoken", ctx.getAccessToken());
        assertEquals("u001", ctx.getPlatformUserId());
        assertEquals("dev001", ctx.getDeviceId());
        assertNotNull(ctx.getExpiresAt());
        // 无 HTTP 调用
        assertEquals(0, mockWebServer.getRequestCount());
    }

    private AuthContext buildAuth() {
        AuthContext auth = new AuthContext();
        auth.setAccessToken("testToken");
        auth.setDeviceId(UUID.randomUUID().toString());
        auth.setExpiresAt(LocalDateTime.now().plusDays(1));
        return auth;
    }
}
