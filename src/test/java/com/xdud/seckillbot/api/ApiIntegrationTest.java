package com.xdud.seckillbot.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xdud.seckillbot.api.dto.request.AccountCreateRequest;
import com.xdud.seckillbot.api.dto.request.TaskCreateRequest;
import com.xdud.seckillbot.domain.enums.ExecutionMode;
import com.xdud.seckillbot.domain.enums.PlatformType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class ApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String cachedToken;

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    /** 登录并缓存 token */
    private String getAdminToken() throws Exception {
        if (cachedToken != null) {
            return cachedToken;
        }
        Map<String, String> login = new HashMap<>();
        login.put("username", "admin");
        login.put("password", "admin123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(login, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl("/api/auth/login"), request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode root = objectMapper.readTree(response.getBody());
        assertEquals(200, root.get("code").asInt());
        cachedToken = root.get("data").get("token").asText();
        assertNotNull(cachedToken);
        assertFalse(cachedToken.isEmpty());
        return cachedToken;
    }

    @BeforeEach
    void resetToken() {
        cachedToken = null;
    }

    @Test
    void loginSuccess() throws Exception {
        String token = getAdminToken();
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void loginFailure() throws Exception {
        Map<String, String> login = new HashMap<>();
        login.put("username", "admin");
        login.put("password", "wrongpassword");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(login, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl("/api/auth/login"), request, String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        // 密码错误，业务码 2003
        assertEquals(2003, root.get("code").asInt());
    }

    @Test
    void protectedWithoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl("/api/accounts"), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void protectedWithToken() throws Exception {
        String token = getAdminToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl("/api/accounts"), HttpMethod.GET, request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode root = objectMapper.readTree(response.getBody());
        assertEquals(200, root.get("code").asInt());
        assertNotNull(root.get("data").get("records"));
    }

    @Test
    void createAndGetAccount() throws Exception {
        String token = getAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        // 创建账号
        AccountCreateRequest createReq = new AccountCreateRequest();
        createReq.setPlatformType(PlatformType.IMOUTAI);
        createReq.setName("测试账号");
        createReq.setPhone("13800138000");
        createReq.setCredentialJson("{\"token\":\"test-secret\"}");
        createReq.setRemark("集成测试");

        HttpEntity<AccountCreateRequest> createEntity = new HttpEntity<>(createReq, headers);
        ResponseEntity<String> createResp = restTemplate.exchange(
                baseUrl("/api/accounts"), HttpMethod.POST, createEntity, String.class);

        assertEquals(HttpStatus.OK, createResp.getStatusCode());
        JsonNode createRoot = objectMapper.readTree(createResp.getBody());
        assertEquals(200, createRoot.get("code").asInt());
        long accountId = createRoot.get("data").get("id").asLong();

        // 获取账号，验证 credentialJson 已脱敏（为 null 或被掩码）
        HttpEntity<Void> getEntity = new HttpEntity<>(headers);
        ResponseEntity<String> getResp = restTemplate.exchange(
                baseUrl("/api/accounts/" + accountId), HttpMethod.GET, getEntity, String.class);

        assertEquals(HttpStatus.OK, getResp.getStatusCode());
        JsonNode getRoot = objectMapper.readTree(getResp.getBody());
        assertEquals(200, getRoot.get("code").asInt());
        JsonNode accountData = getRoot.get("data");
        assertEquals(accountId, accountData.get("id").asLong());
        // credentialJson 不应暴露原始明文（要么为 null，要么不含原始 token 字符串）
        JsonNode credentialNode = accountData.get("credentialJson");
        assertTrue(credentialNode == null || credentialNode.isNull()
                || !credentialNode.asText().contains("test-secret"),
                "credentialJson 不应暴露原始明文凭据");
    }

    @Test
    void createAndScheduleTask() throws Exception {
        String token = getAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        // 创建任务
        TaskCreateRequest createReq = new TaskCreateRequest();
        createReq.setName("集成测试任务");
        createReq.setPlatformType(PlatformType.IMOUTAI);
        createReq.setProductId("test-product-001");
        createReq.setProductName("测试商品");
        createReq.setTriggerAt(LocalDateTime.now().plusDays(1));
        createReq.setAdvanceMs(100);
        createReq.setExecutionMode(ExecutionMode.PARALLEL);
        createReq.setRemark("集成测试");
        createReq.setAccountIds(Collections.emptyList());

        HttpEntity<TaskCreateRequest> createEntity = new HttpEntity<>(createReq, headers);
        ResponseEntity<String> createResp = restTemplate.exchange(
                baseUrl("/api/tasks"), HttpMethod.POST, createEntity, String.class);

        assertEquals(HttpStatus.OK, createResp.getStatusCode());
        JsonNode createRoot = objectMapper.readTree(createResp.getBody());
        assertEquals(200, createRoot.get("code").asInt());
        long taskId = createRoot.get("data").get("id").asLong();
        assertEquals("DRAFT", createRoot.get("data").get("status").asText());

        // 调度任务
        HttpEntity<Void> scheduleEntity = new HttpEntity<>(headers);
        ResponseEntity<String> scheduleResp = restTemplate.exchange(
                baseUrl("/api/tasks/" + taskId + "/schedule"),
                HttpMethod.POST, scheduleEntity, String.class);

        assertEquals(HttpStatus.OK, scheduleResp.getStatusCode());
        JsonNode scheduleRoot = objectMapper.readTree(scheduleResp.getBody());
        assertEquals(200, scheduleRoot.get("code").asInt());

        // 验证任务状态变为 SCHEDULED
        ResponseEntity<String> getResp = restTemplate.exchange(
                baseUrl("/api/tasks/" + taskId), HttpMethod.GET, scheduleEntity, String.class);

        assertEquals(HttpStatus.OK, getResp.getStatusCode());
        JsonNode getRoot = objectMapper.readTree(getResp.getBody());
        assertEquals(200, getRoot.get("code").asInt());
        assertEquals("SCHEDULED", getRoot.get("data").get("status").asText());
    }
}
