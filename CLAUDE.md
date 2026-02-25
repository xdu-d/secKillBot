# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

secKillBot 是一个基于 Spring Boot 2.7.18 的秒杀机器人 Web 系统，支持 i茅台、大麦网、猫眼三个平台，含 Vue 3 前端管理界面。项目托管于 GitHub：https://github.com/xdu-d/secKillBot.git

## 技术栈

- **语言**: Java 8 (JDK 1.8)
- **构建工具**: Maven 3.6.3
- **框架**: Spring Boot 2.7.18 + Undertow
- **ORM**: MyBatis-Plus 3.5.7
- **数据库**: MySQL 8（生产）+ H2（开发）
- **迁移**: Flyway 8.5.13
- **缓存/锁**: Redis + Redisson 3.23.5
- **认证**: Spring Security + JWT (jjwt 0.11.5)
- **HTTP**: OkHttp 4.12.0
- **加密**: AES-GCM（AesGcmCrypto）
- **工具**: Lombok + commons-lang3

## 包结构

```
com.xdud.seckillbot/
├── SecKillBotApplication.java      # 启动类
├── config/                         # Spring 配置（SecurityConfig/RedisConfig/MybatisPlusConfig/ThreadPoolConfig）
├── api/
│   ├── controller/                 # REST API（TaskController/AccountController/PlatformController/AuthController）
│   ├── dto/request/                # 请求 DTO（LoginRequest/TaskCreateRequest/AccountCreateRequest）
│   ├── dto/response/               # 响应 DTO（ApiResponse 统一格式/LoginResponse）
│   └── advice/                     # 全局异常处理（GlobalExceptionHandler）
├── domain/
│   ├── entity/                     # MyBatis-Plus 实体（Task/Account/Platform/SysUser/TaskAccount/ExecutionLog）
│   ├── enums/                      # TaskStatus/PlatformType/ExecutionResult/ExecutionMode/AccountStatus
│   └── mapper/                     # MyBatis-Plus Mapper（6个）
├── service/                        # 业务接口（TaskService/AccountService/PlatformService）+ impl 实现
├── platform/
│   ├── spi/                        # PlatformAdapter/AuthProvider/StockChecker/OrderSubmitter
│   ├── model/                      # AuthContext/OrderRequest/OrderResult/ProductInfo
│   ├── registry/                   # PlatformRegistry（IoC 自动收集适配器）
│   └── impl/
│       ├── imoutai/                # ImoutaiAdapter（骨架，Phase 2 填充）
│       ├── damai/                  # DamaiAdapter（骨架，Phase 3 填充）
│       └── maoyan/                 # MaoyanAdapter（骨架，Phase 3 填充）
├── scheduler/                      # PrecisionScheduler（毫秒级精准调度，基于 ScheduledExecutorService）
├── executor/                       # TaskExecutor（并发/顺序下单逻辑）
├── notification/                   # NotificationChannel 接口 + LogNotification/ServerChanNotification 实现
├── security/                       # JwtTokenProvider/JwtAuthenticationFilter/AesGcmCrypto/UserDetailsServiceImpl
└── common/                         # BizException/ErrorCode/Constants/MybatisMetaObjectHandler
```

## 测试

```
src/test/java/com/xdud/seckillbot/
├── SecKillBotApplicationTest.java      # Spring 上下文加载测试（H2 内嵌库）
└── scheduler/PrecisionSchedulerTest.java   # 精准调度器单元测试
```

## 构建与运行

Maven 本地仓库配置于 `C:\Users\10355\Program Data\maven-local-repository`，settings 文件位于 `C:\Users\10355\.m2\settings.xml`。

**WSL 环境下**需指定 `JAVA_HOME` 和仓库路径（Maven 在 Windows 路径下）：

```bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
MVN="/mnt/c/Program Files/Maven/apache-maven-3.6.3/bin/mvn"
REPO="/mnt/c/Users/10355/Program Data/maven-local-repository"

$MVN clean compile -Dmaven.repo.local="$REPO"                    # 编译
$MVN clean package -DskipTests -Dmaven.repo.local="$REPO"        # 打包（跳过测试）
$MVN spring-boot:run -Dmaven.repo.local="$REPO"                  # 运行（dev profile，H2 内嵌数据库）
$MVN test -Dmaven.repo.local="$REPO"                             # 运行所有测试
$MVN test -Dtest=ClassName -Dmaven.repo.local="$REPO"            # 运行单个测试类
```

**注意**：运行前需确保 Redis 已启动（`sudo service redis-server start`，dev 默认 127.0.0.1:6379）。

## 重要配置

- **AES 密钥**：`application.yml` 中 `app.crypto.aes-key` 需替换为 Base64 编码的 32 字节随机密钥
- **JWT 密钥**：`app.jwt.secret` 生产环境需替换为随机长字符串（至少 32 字符）
- **默认管理员**：用户名 `admin`，密码 `admin123`（首次登录后请立即修改）
- **环境切换**：运行时加 `--spring.profiles.active=prod` 切换生产环境

## 实现阶段

- **Phase 1（已完成）**：完整骨架实现 —— CRUD API、JWT 登录、Spring Security、MyBatis-Plus、Flyway 迁移、AES-GCM 加密、Redis 配置、精准调度器框架、平台 SPI 接口、通知框架、三平台适配器骨架、单元测试全部就绪
- **Phase 2（计划）**：i茅台适配器完整实现（认证/库存查询/下单）+ PrecisionScheduler 实际调度集成 + TaskExecutor 端到端验证
- **Phase 3（计划）**：大麦/猫眼适配器 + Token 自动刷新 + ServerChan 通知完整实现 + Vue 3 前端管理界面

## 权限配置

`.claude/settings.local.json` 中已配置 Claude Code 权限，允许 `cd`、`mvn` 等命令执行。
