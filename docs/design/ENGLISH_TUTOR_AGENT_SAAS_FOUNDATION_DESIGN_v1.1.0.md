# 08. English Tutor Agent SaaS Foundation 设计与实施指导

> 文档定位：**面向 Codex / Cursor / AI Coding Agent 的实现级设计文档**  
> 版本：V1.1.0  
> 状态：Ready for implementation planning  
> 适用范围：现有 `english-tutor-agent` 仓库的 SaaS Foundation 改造  
> UI 参考：`docs/ui/ENGLISH_TUTOR_AGENT_SAAS_UI_PROTOTYPE_v1.1.0.html`

---

## 0. 文档使用说明

本文件不是新的独立产品，而是对现有 English Tutor Agent 的 **SaaS Foundation 增量改造设计**。

AI Coding Agent 在开始实现前必须同时阅读：

1. 当前仓库代码；
2. `01_PRODUCT_REQUIREMENTS.md`；
3. `02_AGENT_DESIGN.md`；
4. `03_TECHNICAL_DESIGN.md`；
5. `04_DEVELOPMENT_ROADMAP.md`；
6. 本文件；
7. `english-tutor-saas-ui-prototype-v1.html`。

### 0.1 冲突处理优先级

如旧文档与本文件冲突，按以下优先级处理：

```text
本次已确认的 SaaS 产品需求
        ↓
08_SAAS_FOUNDATION_DESIGN.md
        ↓
当前仓库实际代码 / Schema
        ↓
旧 PRD / Agent Design / Technical Design / Roadmap
```

特别说明：

- 旧设计中的 `userKey` / `X-User-Key` 只能视为开发阶段临时身份机制；
- SaaS 化后，**普通用户身份必须来自后端认证上下文**；
- 原有英语学习核心业务、Agent、学习档案、计划、训练会话等应尽量复用，不得为了 SaaS 化整体重写；
- 本轮不实现付费订阅与 Billing。

### 0.2 Codex 实施规则

Codex 不得一次性“重写整个系统”。必须：

1. 先完成当前代码影响分析；
2. 按本文 Milestone 顺序实现；
3. 每个 Milestone 只修改必要模块；
4. 每个 Milestone 结束时运行自动化测试；
5. Flyway 只能新增 Migration，不得修改已发布的 V1–V10；
6. 对外 API 改造必须有兼容/迁移说明；
7. 不得把 API Key、密码、Refresh Token 写入日志、测试快照或 Git；
8. 不得让普通客户端通过任意 `userId` / `userKey` 访问其他用户数据。

---

# 1. 当前系统基线

## 1.1 当前后端技术基线

当前仓库后端为 Maven 模块化单体：

```text
server/
├── tutor-domain
├── tutor-application
├── tutor-api
├── tutor-agent
├── tutor-infrastructure
├── tutor-observability
├── tutor-test-support
└── tutor-bootstrap
```

当前技术栈以仓库为准：

```text
Java 21
Spring Boot 4.1.x
Spring AI 2.0.x
MySQL
Redis
Flyway
Testcontainers
ArchUnit
```

必须保持现有模块边界和 ArchUnit 规则。

### 1.2 当前用户模型

当前数据库已经存在：

```text
app_user
user_learning_profile
```

`app_user` 当前核心字段：

```text
id
user_key
status
timezone
locale
created_at_utc
updated_at_utc
version
```

`user_learning_profile.user_id` 已经通过 FK 关联：

```text
app_user.id
```

因此本次 SaaS 改造**不得重新创建另一套独立 user 主表**。

设计决策：

> 继续以 `app_user.id` 作为所有业务数据的内部用户主键，在 `app_user` 上增加账号认证字段，并增加角色、认证会话、额度等 SaaS 表。

### 1.3 当前身份机制的问题

当前 Web：

```text
浏览器 localStorage
   ↓
生成 userKey
   ↓
X-User-Key Header
   ↓
Backend
```

这是开发阶段便利机制，不具备 SaaS 身份可信性。

存在的问题：

1. userKey 由客户端生成；
2. 客户端可以伪造其他 userKey；
3. 无登录和密码；
4. 无管理员；
5. 无账号禁用后的登录态失效；
6. 无真实多用户安全边界。

SaaS 改造后必须变成：

```text
Email + Password
      ↓
Authentication
      ↓
SecurityContext / CurrentActor
      ↓
app_user.id
      ↓
Learning Application Services
```

### 1.4 当前 Web / Android 基线

Web：

```text
React 19
TypeScript
Vite
pnpm
```

Android：

```text
Kotlin
Jetpack Compose + Material 3
Hilt
ViewModel + StateFlow
单向数据流
```

SaaS 身份、额度和语言能力需要同时支持：

```text
Web Learner
Android Learner
Web Admin Console
```

V1 不要求 Android 原生实现 Admin Console。

---

# 2. SaaS 改造目标架构

## 2.1 目标结构

```text
                         ┌─────────────────────┐
                         │  Web / Android      │
                         └─────────┬───────────┘
                                   │
                          Access Token / Auth
                                   │
                         ┌─────────▼───────────┐
                         │ Spring Security     │
                         │ Authentication      │
                         └─────────┬───────────┘
                                   │
                         ┌─────────▼───────────┐
                         │ CurrentActor        │
                         │ userId / roles      │
                         └──────┬───────┬──────┘
                                │       │
                      USER flow │       │ ADMIN flow
                                │       │
             ┌──────────────────▼─┐   ┌─▼──────────────────┐
             │ Learning Services  │   │ Admin Services     │
             │ existing business  │   │ User / Quota / AI │
             └─────────┬──────────┘   └──────────┬─────────┘
                       │                         │
             ┌─────────▼──────────┐    ┌────────▼──────────┐
             │ QuotaGuard         │    │ System Config     │
             │ Usage Reservation  │    │ Audit             │
             └─────────┬──────────┘    └────────┬──────────┘
                       │                         │
                       └────────────┬────────────┘
                                    │
                           ┌────────▼─────────┐
                           │ AI Orchestration │
                           │ ProviderRegistry │
                           └────────┬─────────┘
                                    │
                          Runtime Provider Config
                                    │
                              LLM / ASR / TTS
```

## 2.2 设计原则

### P0 原则

1. **Authentication 与 Learning Domain 解耦**
2. **Authorization 在后端强制执行**
3. **普通用户不能指定任意 userId**
4. **额度按业务请求次数，不按 LLM Token**
5. **用户额度与模型成本统计完全解耦**
6. **API Secret 加密存储**
7. **AI Provider 可以运行时切换**
8. **管理员操作必须审计**
9. **保留现有业务数据和 app_user.id**
10. **Web / Android 使用同一套后端身份和额度语义**

---

# 3. 角色与权限设计

## 3.1 V1 角色

```text
USER
ADMIN
```

### USER

允许：

- 登录、退出；
- 查看/修改自己的基础账号信息；
- 英语学习；
- 今日计划；
- 训练会话；
- 对话；
- 纠错；
- 历史；
- 学习档案；
- 查看自己的额度。

禁止：

- `/api/v1/admin/**`
- 查询其他用户
- 修改 Provider
- 查看 API Key
- 修改全局设置
- 修改其他用户额度

### ADMIN

除普通账号能力外，允许：

- 用户查询；
- 用户启用/禁用；
- 额度查询/覆盖/Reset/临时加额；
- 用户角色管理；
- Provider 管理；
- Model / Base URL 管理；
- API Secret 替换；
- 系统设置；
- 运营 Dashboard；
- Audit Log。

## 3.2 RBAC 扩展设计

V1 UI 不实现复杂权限编辑器，但后端不要将所有逻辑硬编码成：

```java
if (role == ADMIN)
```

推荐 Schema：

```text
app_role
app_permission
app_role_permission
app_user_role
```

V1 Seed：

### USER

普通学习权限由“已认证用户”获得，不需要大量权限码。

### ADMIN

Seed 权限：

```text
DASHBOARD_READ

USER_READ
USER_UPDATE
USER_STATUS_MANAGE
USER_QUOTA_MANAGE
USER_ROLE_MANAGE

AI_PROVIDER_READ
AI_PROVIDER_MANAGE

SYSTEM_SETTING_READ
SYSTEM_SETTING_MANAGE

AUDIT_READ
```

Admin Controller 优先使用：

```java
@PreAuthorize("hasAuthority('USER_QUOTA_MANAGE')")
```

而不是只有：

```java
hasRole("ADMIN")
```

这样未来可以自然增加：

```text
SUPER_ADMIN
OPERATOR
CUSTOMER_SERVICE
```

---

# 4. 用户账号与认证设计

## 4.1 Email 规则

产品登录唯一标识：

```text
email
```

内部数据主键：

```text
app_user.id
```

公共不可变标识：

```text
app_user.user_key
```

### user_key 的新定位

保留 `user_key`：

- 兼容既有数据；
- 可作为外部 opaque user reference；
- 新用户由**服务端**生成。

但：

> `user_key` 不再作为认证凭据，不得由客户端自行生成后获得某个账号身份。

### Email Normalization

注册时：

```text
trim
↓
Unicode / ASCII email 基础规范化
↓
lowercase
```

V1 保存：

```text
email
email_normalized
```

唯一约束：

```text
UNIQUE(email_normalized)
```

例如：

```text
Steven@example.com
steven@example.com
```

视为同一个账号。

> V1 不做 Gmail 点号、`+alias` 等 Provider-specific 合并。

## 4.2 app_user 增量字段

建议通过 V11+ Migration 增加：

```text
email
email_normalized
password_hash
email_verified_at_utc
last_login_at_utc
disabled_at_utc
deleted_at_utc
auth_version
```

继续使用已有：

```text
status
timezone
locale
version
```

状态枚举：

```text
ACTIVE
DISABLED
DELETED
LEGACY
```

### auth_version

用于快速使旧 Token 失效。

例如管理员禁用、改密码、强制退出时：

```text
auth_version = auth_version + 1
```

Access Token 中包含：

```text
uid
auth_version
```

鉴权时校验账号状态和 auth_version。

---

# 5. Token / Session 设计

## 5.1 Access Token

采用短期 Access Token：

```text
格式：JWT
TTL：建议 15 分钟
```

Claims：

```json
{
  "sub": "usr_xxx",
  "uid": 12345,
  "roles": ["USER"],
  "authorities": [],
  "auth_version": 3,
  "iat": 0,
  "exp": 0
}
```

JWT 不保存：

- 密码；
- API Key；
- 用户敏感学习数据。

签名密钥必须通过生产 Secret 提供，不提交 Git。

## 5.2 Refresh Token

采用：

```text
Opaque random token
```

数据库只保存：

```text
SHA-256(token)
```

不保存原始 refresh token。

字段建议：

```text
id
user_id
token_hash
client_type
device_name
expires_at_utc
revoked_at_utc
created_at_utc
last_used_at_utc
replaced_by_id
```

Refresh Token：

- 登录时创建；
- Refresh 时 rotation；
- Logout 时 revoke；
- 改密码 / Disable 时批量 revoke。

## 5.3 Web Token 存储

Web：

```text
Access Token → 内存
Refresh Token → HttpOnly + Secure + SameSite=Lax Cookie
```

禁止：

```text
localStorage 保存 Refresh Token
```

如果生产尚未 HTTPS：

- 开发环境允许 Secure=false；
- 正式 SaaS 上线必须 HTTPS + Secure Cookie。

## 5.4 Android Token 存储

Android：

```text
Access Token → 内存
Refresh Token → Android Keystore 保护的安全存储
```

不得写入：

```text
普通 SharedPreferences 明文
Logcat
Crash log
```

---

# 6. Auth API

统一使用：

```text
/api/v1/auth
```

## 6.1 Register

```http
POST /api/v1/auth/register
```

Request：

```json
{
  "email": "alex@example.com",
  "password": "..."
}
```

Response：

```json
{
  "user": {
    "userKey": "usr_xxx",
    "email": "alex@example.com",
    "status": "ACTIVE",
    "roles": ["USER"],
    "locale": "zh-CN"
  },
  "accessToken": "...",
  "expiresIn": 900
}
```

规则：

- 普通注册永远只获得 USER；
- email_normalized 冲突 → `409 EMAIL_ALREADY_REGISTERED`；
- public registration 关闭 → `403 REGISTRATION_DISABLED`；
- Email verification 是否强制由系统设置决定。

## 6.2 Login

```http
POST /api/v1/auth/login
```

失败不要暴露：

```text
邮箱存在 / 不存在
```

统一：

```text
INVALID_CREDENTIALS
```

## 6.3 Refresh

```http
POST /api/v1/auth/refresh
```

必须：

- rotation；
- 检测 revoked；
- 检测过期；
- 检测 user status；
- 检测 auth_version。

## 6.4 Logout

```http
POST /api/v1/auth/logout
```

撤销当前 refresh session。

可扩展：

```http
POST /api/v1/auth/logout-all
```

## 6.5 Current User

```http
GET /api/v1/me
```

Response：

```json
{
  "userKey": "usr_xxx",
  "email": "alex@example.com",
  "status": "ACTIVE",
  "roles": ["USER"],
  "locale": "zh-CN",
  "timezone": "Asia/Shanghai"
}
```

---

# 7. CurrentActor 与数据隔离

## 7.1 新的身份入口

新增应用层抽象，例如：

```java
public interface CurrentActor {
    long userId();
    String userKey();
    String email();
    Set<String> roles();
    Set<String> authorities();
}
```

具体 SecurityContext 适配器放在外层模块。

业务 Application Service 接收：

```text
currentActor.userId
```

而不是信任客户端：

```text
X-User-Key
?userId=
"userId": ...
```

## 7.2 Controller 原则

错误：

```java
@GetMapping("/plans/today")
LearningPlan get(@RequestHeader("X-User-Key") String userKey)
```

目标：

```java
@GetMapping("/plans/today")
LearningPlan get() {
    return service.getTodayPlan(currentActor.userId());
}
```

## 7.3 数据 Repository 原则

所有用户资源查询必须包含：

```text
user_id = current_user_id
```

例如 session：

```sql
SELECT ...
FROM training_session
WHERE session_id = ?
  AND user_id = ?
```

不能：

1. 只按 session_id 查询；
2. 查询后再在 Controller 层比较 user；
3. 接受前端传来的 user_id 作为可信条件。

## 7.4 Admin 访问

管理员访问其他用户必须走：

```text
/api/v1/admin/**
```

并显式指定目标：

```http
GET /api/v1/admin/users/{userKey}
```

普通学习 API 永远表示：

```text
“当前登录用户”
```

---

# 8. Legacy X-User-Key 迁移

当前 Web 依赖：

```text
X-User-Key
```

SaaS 改造需要删除该身份机制。

为降低开发期间前后端部署顺序风险，可以提供**临时兼容开关**：

```text
TUTOR_AUTH_LEGACY_USER_KEY_ENABLED
```

规则：

- dev/test 可临时 true；
- production 默认 false；
- `/api/v1/admin/**` 永远不支持 Legacy；
- 兼容层只用于迁移，不作为长期功能；
- Web / Android 完成认证改造后删除 Legacy client code；
- 最终删除兼容开关和相关 Resolver。

---

# 9. 每日额度领域设计

## 9.1 产品语义

用户看到：

```text
Daily quota = 50 requests
Used = 18
Remaining = 32
```

用户额度不等于模型 Token。

一次成功触发 AI 生成的业务动作：

```text
consume = 1
```

例如：

- AI Conversation；
- Grammar Correction；
- Expression Optimization；
- AI Evaluation；
- AI Generated Practice。

不扣额度：

- 查看历史；
- 查看已生成总结；
- 查看 Profile；
- 查看计划；
- 查看额度；
- 本地 UI 操作。

## 9.2 Provider Token 与用户额度解耦

系统内部仍记录：

```text
input_tokens
output_tokens
provider
model
estimated_cost
latency
```

但用户 quota 只看：

```text
business request count
```

---

# 10. Quota 数据模型

建议新增：

```text
user_quota_policy
daily_quota_usage
quota_consumption
```

## 10.1 user_quota_policy

用户级覆盖配置。

```sql
user_id             BIGINT PK/FK
daily_limit_override INT NULL
unlimited           BOOLEAN NOT NULL DEFAULT FALSE
updated_by_user_id  BIGINT NULL
updated_at_utc      DATETIME(3)
version             BIGINT
```

含义：

```text
override != null → 使用 override
override == null → 使用 system default
unlimited = true → 不限制
```

## 10.2 daily_quota_usage

每日快照。

```text
id
user_id
quota_date
base_limit
bonus_limit
used_count
timezone
created_at_utc
updated_at_utc
version
```

唯一键：

```text
UNIQUE(user_id, quota_date)
```

`quota_date`：

> 使用系统设置的 Quota Reset Timezone 计算，不使用数据库服务器本地日期。

## 10.3 quota_consumption

每次 AI 业务请求一条。

```text
id
request_id
idempotency_key
user_id
quota_date
quota_cost
status
feature_code
reserved_at_utc
committed_at_utc
refunded_at_utc
expires_at_utc
failure_code
```

状态：

```text
RESERVED
COMMITTED
REFUNDED
```

唯一约束：

```text
UNIQUE(request_id)
UNIQUE(user_id, idempotency_key)
```

---

# 11. Quota 原子扣减流程

## 11.1 正常请求

```text
Client
  ↓
Authenticated Request
  ↓
Idempotency-Key
  ↓
QuotaService.reserve()
  ↓
Atomic used_count + 1
  ↓
quota_consumption = RESERVED
  ↓
Invoke AI
  ↓
Valid result delivered
  ↓
COMMITTED
```

## 11.2 并发要求

例如：

```text
Remaining = 1
```

同时 5 个请求：

```text
只能 1 个 reserve 成功
其余返回 DAILY_QUOTA_EXCEEDED
```

推荐使用数据库条件更新：

```sql
UPDATE daily_quota_usage
SET used_count = used_count + 1,
    version = version + 1
WHERE id = ?
  AND used_count < (base_limit + bonus_limit);
```

检查 affected rows。

> 不得只在 Java 中先 read remaining，再 update。

## 11.3 AI 失败退款

如果：

```text
reserve 成功
↓
AI 在产生任何有效用户结果前失败
```

则：

```text
used_count - 1
quota_consumption → REFUNDED
```

### Streaming 特殊规则

Conversation SSE：

- 在调用 Provider 前 Reserve；
- 如果**尚未发送任何有效 AI 内容**即失败 → Refund；
- 如果已经向用户发送了有效部分结果 → 本次计为已使用，不 Refund；
- 客户端自动重试必须复用相同 Idempotency-Key，避免重复扣额度。

## 11.4 Crash Recovery

为 RESERVED 记录提供：

```text
expires_at_utc
```

定时 Reconciliation Job：

```text
查找超时 RESERVED
↓
确认没有成功 AI result / commit
↓
refund
```

保证 JVM Crash 后不会永久误扣。

---

# 12. Quota API

## 12.1 User

```http
GET /api/v1/me/quota
```

Response：

```json
{
  "quotaDate": "2026-08-14",
  "dailyLimit": 50,
  "used": 18,
  "bonus": 0,
  "remaining": 32,
  "unlimited": false,
  "resetAt": "2026-08-15T00:00:00+08:00"
}
```

额度用尽：

```http
429 Too Many Requests
```

Problem Code：

```text
DAILY_QUOTA_EXCEEDED
```

Problem detail 包含：

```text
dailyLimit
used
remaining
resetAt
```

## 12.2 Admin

```http
GET  /api/v1/admin/users/{userKey}/quota
PUT  /api/v1/admin/users/{userKey}/quota
POST /api/v1/admin/users/{userKey}/quota/reset
POST /api/v1/admin/users/{userKey}/quota/bonus
```

### Override

```json
{
  "dailyLimitOverride": 100,
  "unlimited": false
}
```

### Reset

```text
used_count → 0
```

只影响当前 quota period。

### Bonus

```json
{
  "amount": 20,
  "reason": "manual_support_adjustment"
}
```

只影响当前周期，不修改 future daily limit。

所有 Admin quota 操作写 Audit Log。

---

# 13. System Settings

## 13.1 表

```text
system_setting
```

字段：

```text
setting_key
value_json
value_type
updated_by_user_id
updated_at_utc
version
```

V1 Keys：

```text
quota.default_daily_limit
quota.reset_timezone
registration.enabled
email_verification.required
```

### 配置原则

- 不在代码里硬编码业务数值；
- Production 启动时必须能读取有效配置；
- dev/test 可以 Seed 合理测试值；
- API Key 不允许放 system_setting。

## 13.2 Cache

System settings 可缓存：

```text
Caffeine / local cache
```

或当前架构合适的轻量方案。

管理员更新时：

```text
update DB
↓
evict cache
```

V1 不需要复杂分布式配置中心。

---

# 14. AI Provider Runtime Configuration

## 14.1 目标

当前 `application.yml` 中 Provider 选择属于静态部署配置。

SaaS 后目标：

```text
Admin UI
  ↓
Database Configuration
  ↓
ProviderConfigService
  ↓
ProviderRegistry
  ↓
AI request
```

管理员修改：

- Provider enabled；
- Default provider；
- Base URL；
- Model；
- API Key；

无需：

```text
修改 application.yml
Jenkins Build
重启应用
```

## 14.2 Provider 表

```text
ai_provider_config
```

字段建议：

```text
id
provider_code
provider_type
display_name
base_url
default_model
enabled
is_default
config_json
created_at_utc
updated_at_utc
version
```

约束：

```text
UNIQUE(provider_code)
```

V1 Provider Type 可支持：

```text
OPENAI_COMPATIBLE
OPENAI
DEEPSEEK
GEMINI
QWEN
CUSTOM
```

具体 Adapter 只实现当前项目实际需要的 Provider。

## 14.3 Provider Secret

单独表：

```text
ai_provider_secret
```

字段：

```text
provider_id
secret_type
ciphertext
nonce
key_version
masked_hint
updated_by_user_id
updated_at_utc
```

`secret_type`：

```text
API_KEY
```

未来可扩展：

```text
CLIENT_SECRET
SERVICE_ACCOUNT
```

---

# 15. API Key 加密设计

## 15.1 Master Key

生产环境提供：

```text
TUTOR_SECRET_ENCRYPTION_KEY
```

要求：

- 32-byte random key；
- Base64 编码；
- 存储在部署 Secret / environment；
- 不写 DB；
- 不写 Git；
- 不写日志。

## 15.2 加密

推荐：

```text
AES-256-GCM
```

每次加密：

```text
random nonce
ciphertext
auth tag
key_version
```

数据库不存在明文 Secret。

## 15.3 API 返回

Provider detail：

```json
{
  "providerCode": "openai",
  "apiKeyConfigured": true,
  "apiKeyMasked": "sk-••••••••8F2A"
}
```

永远禁止：

```json
{
  "apiKey": "sk-real-secret"
}
```

编辑操作：

```http
PUT /api/v1/admin/ai-providers/{providerCode}/secret
```

语义：

> Replace secret，而不是读取后编辑。

## 15.4 Logging

必须全局禁止输出：

```text
Authorization
Cookie
Set-Cookie
password
refreshToken
apiKey
provider secret
```

HTTP request logging / error serialization 需要脱敏。

---

# 16. Provider Runtime Resolution

建议应用层接口：

```java
public interface AiProviderConfigurationService {

    ActiveProviderConfiguration getDefaultLlmProvider();

    Optional<ActiveProviderConfiguration> getByCode(String providerCode);

    List<ProviderSummary> list();
}
```

Agent 不直接访问数据库。

现有 Model Provider / Agent abstractions 保持。

Provider Adapter 负责：

```text
Config → Client
```

可做短 TTL cache。

配置变更：

```text
Admin update
↓
Version changes
↓
Cache invalidate
↓
next request uses new provider
```

测试环境继续保留：

```text
fake provider
```

不要让数据库 Provider 配置破坏现有单元测试的 Fake 模型能力。

---

# 17. Admin API

统一前缀：

```text
/api/v1/admin
```

## 17.1 Dashboard

```http
GET /api/v1/admin/dashboard
```

返回：

```text
totalUsers
activeUsersToday
newUsersToday
aiRequestsToday
usersReachedQuotaLimit
activeDefaultProvider
```

V1 Dashboard 可以直接 SQL 聚合。

不要求建设复杂 BI。

## 17.2 Users

```http
GET /api/v1/admin/users
GET /api/v1/admin/users/{userKey}
PATCH /api/v1/admin/users/{userKey}/status
PUT /api/v1/admin/users/{userKey}/roles
```

Query：

```text
q=email/userKey
status=
role=
page=
size=
```

响应必须分页。

## 17.3 Provider

```http
GET  /api/v1/admin/ai-providers
POST /api/v1/admin/ai-providers
GET  /api/v1/admin/ai-providers/{providerCode}
PUT  /api/v1/admin/ai-providers/{providerCode}
PUT  /api/v1/admin/ai-providers/{providerCode}/secret
POST /api/v1/admin/ai-providers/{providerCode}/enable
POST /api/v1/admin/ai-providers/{providerCode}/disable
POST /api/v1/admin/ai-providers/{providerCode}/make-default
```

必须保证：

```text
同一 Provider 类型可扩展
至多一个 default LLM provider
```

## 17.4 Settings

```http
GET /api/v1/admin/settings
PUT /api/v1/admin/settings/{key}
```

---

# 18. Audit Log

## 18.1 表

```text
admin_audit_log
```

字段：

```text
id
actor_user_id
action_code
target_type
target_key
before_json
after_json
request_id
ip_address
user_agent
created_at_utc
```

禁止记录：

```text
password hash
password
API Key ciphertext
API Key plaintext
Refresh Token
Authorization Header
```

Provider Secret 替换只记录：

```json
{
  "apiKeyConfigured": true,
  "maskedHint": "****8F2A"
}
```

## 18.2 V1 action_code

```text
USER_STATUS_CHANGED
USER_ROLE_CHANGED
USER_QUOTA_CHANGED
USER_QUOTA_RESET
USER_QUOTA_BONUS_ADDED

AI_PROVIDER_CREATED
AI_PROVIDER_UPDATED
AI_PROVIDER_ENABLED
AI_PROVIDER_DISABLED
AI_PROVIDER_DEFAULT_CHANGED
AI_PROVIDER_SECRET_REPLACED

SYSTEM_SETTING_CHANGED
```

---

# 19. AI Usage / 成本观测

新增或扩展模型调用记录：

```text
ai_call_usage
```

字段建议：

```text
request_id
user_id
feature_code
provider_code
model
input_tokens
output_tokens
estimated_cost
latency_ms
status
created_at_utc
```

注意：

```text
ai_call_usage token
```

只用于：

- Admin cost monitoring；
- Provider usage；
- Observability。

不得用于：

```text
user daily quota calculation
```

---

# 20. Password Security

V1 使用 Spring Security `PasswordEncoder`。

建议：

```text
BCrypt
```

Cost 必须可配置，并通过测试验证。

要求：

- 数据库仅保存 hash；
- Register / Reset 后立即 hash；
- 日志中不得输出 password；
- Login 错误信息不区分邮箱是否存在；
- 可增加登录失败 Rate Limit（P1）。

---

# 21. Email Verification / Forgot Password

产品已要求支持，但可放在 P1。

## 21.1 Forgot Password

```http
POST /api/v1/auth/password/forgot
POST /api/v1/auth/password/reset
```

Reset Token：

- random opaque token；
- DB 保存 hash；
- 有过期时间；
- 单次使用；
- 成功改密码后 revoke refresh sessions；
- auth_version + 1。

## 21.2 Email Verification

如果：

```text
email_verification.required = true
```

未验证用户：

- 可以登录；
- 可以进入 Account / Verification UI；
- AI 学习请求拒绝；
- 静态历史是否允许查看由产品策略决定，V1 建议允许。

邮件 Provider/SMTP 配置属于部署配置或后续 Admin 配置，不是本轮 P0 的核心。

---

# 22. Admin Bootstrap

普通注册接口永远不能创建 ADMIN。

生产首个管理员：

```text
one-time bootstrap
```

建议环境：

```text
TUTOR_BOOTSTRAP_ADMIN_ENABLED
TUTOR_BOOTSTRAP_ADMIN_EMAIL
TUTOR_BOOTSTRAP_ADMIN_PASSWORD
```

流程：

```text
启动
↓
enabled=true
↓
ADMIN 不存在
↓
创建 / 绑定管理员
↓
日志只输出 email + success，不输出 password
↓
部署后关闭 bootstrap flag
```

要求：

- 已存在同 email 时不重复创建；
- Bootstrap 后必须指导关闭开关；
- 不允许默认 `admin/admin`。

如 Codex 有更安全且不增加过多运维复杂度的 one-shot CLI 方案，也可以采用，但必须保留“一次性初始化、无默认弱密码”的原则。

---

# 23. Existing Data Migration

## 23.1 Flyway 版本

当前已有：

```text
V1 ... V10
```

SaaS Migration 必须从：

```text
V11
```

开始。

禁止：

```text
修改已存在 V1–V10
```

## 23.2 app_user Legacy 数据

现有 `app_user` 可能只有：

```text
user_key
```

没有 Email。

建议两阶段迁移：

### Phase 1

V11：

- 新增 auth 字段，暂允许 email/password_hash NULL；
- 新增 auth / RBAC / quota 表；
- 新注册用户必须写完整账号字段；
- 旧记录标识为 LEGACY 或通过字段判断 Legacy。

### Phase 2

提供一次“Claim legacy user”方案：

```text
选定当前真实开发用户 app_user.id
↓
绑定正式 email
↓
设置 password hash
↓
ACTIVE
↓
保留原 app_user.id
```

这样已有：

- learning profile；
- assessment；
- learning plan；
- training session；
- evidence；

无需迁移 foreign key。

其他无用 legacy user：

```text
DISABLED / LEGACY
```

后续数据清理后再考虑 email NOT NULL。

---

# 24. Web 架构改造

## 24.1 删除临时身份

当前：

```text
getOrCreateUserKey()
localStorage userKey
X-User-Key
```

目标：

```text
AuthStore
AccessToken
CurrentUser
```

`userKey` 仅从 `/api/v1/me` 返回，不由 Web 创建。

## 24.2 Web State

建议：

```text
AuthProvider / AuthStore
LocaleProvider
ApiClient
```

Auth State：

```ts
type AuthState =
  | { status: "loading" }
  | { status: "anonymous" }
  | { status: "authenticated"; user: CurrentUser; accessToken: string };
```

## 24.3 ApiClient

删除：

```ts
ApiClientOptions.userKey
RequestOptions.userKey
X-User-Key
```

增加：

```text
Authorization: Bearer <accessToken>
```

统一 401 处理：

```text
401
↓
refresh once
↓
retry request once
↓
仍失败 → logout
```

SSE：

- stream 建立前 token 必须有效；
- 建立失败 401 可以 refresh + retry once；
- stream 已开始后不自动重建。

## 24.4 Route

Public：

```text
/login
/register
/forgot-password
/reset-password
```

Learner：

```text
/
/practice
/history
/progress
/account
```

Admin：

```text
/admin
/admin/users
/admin/providers
/admin/settings
/admin/audit
```

Admin route：

```text
Frontend Guard
+
Backend Authorization
```

Frontend Guard 仅用于 UX，不是安全边界。

---

# 25. Web / Android 双语

## 25.1 UI Language

支持：

```text
zh-CN
en
```

默认：

```text
zh-CN
```

用户切换后：

- 登录前：本地保存；
- 登录后：同步 `app_user.locale`；
- Web / Android 登录同账号时使用账号 locale；
- 客户端可再次切换。

## 25.2 本地化边界

本地化：

- 菜单；
- 按钮；
- 登录注册；
- Quota；
- Admin；
- Settings；
- Error Messages；
- 操作提示。

英语学习核心内容：

```text
Conversation
Practice Prompt
User English
Natural Expression
```

保持英文为主。

解释：

```text
Grammar explanation
Correction explanation
```

可以根据 locale 输出中文或英文。

### 不允许

把整个学习对话翻译成中文，导致失去英语训练目的。

## 25.3 Web i18n 实现

正式代码不要照抄 HTML 原型中的 DOM translation script。

应使用 React i18n abstraction，例如：

```text
LocaleContext
translation dictionaries
t("quota.remaining")
```

目录示例：

```text
web/src/shared/i18n/
├── en.ts
├── zhCN.ts
├── LocaleProvider.tsx
└── useI18n.ts
```

如项目决定引入成熟轻量 i18n library，可以使用，但不要为两个语言构建过重框架。

## 25.4 Android i18n

使用 Android resources：

```text
res/values/strings.xml
res/values-zh-rCN/strings.xml
```

运行时语言切换按 Android 推荐方式实现。

---

# 26. UI 页面映射

视觉参考：

```text
english-tutor-saas-ui-prototype-v1.html
```

原型只作为：

```text
information architecture
visual hierarchy
responsive behavior
interaction reference
```

不得把原型 HTML 直接复制进 React / Compose 生产代码。

## 26.1 Learner Web / Android

### Login / Register

显示：

- Email；
- Password；
- Remember / Keep signed in；
- Forgot Password；
- 中文 / English。

### Today

显示：

```text
Today practice
Daily quota
Remaining
Learning plan
Current streak
Speaking turns
Corrections
```

### Practice

保留当前英语训练核心：

```text
Conversation
Instant correction
Natural expression
Quota summary
```

Quota 不要成为训练页面视觉主体。

### History

读取历史不扣额度。

### Progress

展示：

```text
Speaking confidence
Grammar accuracy
Natural expression
Practice consistency
Priority areas
```

### Account & Quota

显示：

```text
Email
Account status
Locale
Daily quota
Used
Remaining
Reset time
```

普通用户不能自行修改管理员分配的 quota。

## 26.2 Admin Console

V1：

> Web-only implementation，响应式支持手机浏览器。

不要求 Android Native Admin。

页面：

```text
Overview
Users
AI Providers
System Settings
Audit Log
```

---

# 27. Admin UI Interaction

## 27.1 Users

列表：

```text
Email
UserKey
Role
Status
Used Today
Daily Quota
Last Active
```

Manage Drawer / Modal：

```text
Quota override
Reset today
Temporary bonus
Disable / Enable
Role
```

危险操作需要二次确认：

```text
Disable
Delete
Role -> ADMIN
```

## 27.2 Provider

Card / Form：

```text
Display Name
Provider Type
Base URL
Model
Enabled
Default
API Key configured
Masked hint
Replace API Key
```

Secret 保存后不得回显。

## 27.3 Settings

```text
Default Daily Quota
Quota Reset Timezone
Public Registration
Email Verification
```

修改成功：

- 保存 DB；
- 清理 cache；
- Audit；
- UI Toast。

---

# 28. API Error Contract

统一 Problem Details。

示例：

```json
{
  "type": "https://english-tutor/errors/daily-quota-exceeded",
  "title": "Daily quota exceeded",
  "status": 429,
  "code": "DAILY_QUOTA_EXCEEDED",
  "detail": "Today's AI learning quota has been used up.",
  "requestId": "req_xxx",
  "extensions": {
    "dailyLimit": 50,
    "used": 50,
    "remaining": 0,
    "resetAt": "..."
  }
}
```

核心 codes：

```text
AUTH_REQUIRED
INVALID_CREDENTIALS
ACCOUNT_DISABLED
ACCOUNT_DELETED
EMAIL_ALREADY_REGISTERED
REGISTRATION_DISABLED
EMAIL_VERIFICATION_REQUIRED
FORBIDDEN
DAILY_QUOTA_EXCEEDED
PROVIDER_UNAVAILABLE
PROVIDER_CONFIGURATION_INVALID
```

前端按 `code` 本地化，不解析英文 `detail` 做逻辑。

---

# 29. Module Placement

遵守现有 modular monolith。

## 29.1 tutor-domain

新增纯业务领域：

```text
identity/
  UserRole
  UserStatus
  AccountPolicy

quota/
  DailyQuota
  QuotaPolicy
  QuotaConsumptionStatus

admin/
  PermissionCode

provider/
  ProviderConfiguration
```

Domain 不依赖 Spring。

如现有 package 约束更适合放在 `shared`，Codex 可以调整 package，但必须更新 ArchUnit 并保持 domain 无 Spring。

## 29.2 tutor-application

新增：

```text
AuthApplicationService
CurrentActor contracts
QuotaApplicationService
AdminUserApplicationService
AdminQuotaApplicationService
ProviderConfigurationApplicationService
SystemSettingApplicationService
AuditApplicationService
```

Ports：

```text
UserAccountRepository
RoleRepository
RefreshSessionRepository
QuotaRepository
ProviderConfigurationRepository
SecretCipher
SystemSettingRepository
AuditLogRepository
```

## 29.3 tutor-api

新增：

```text
AuthController
MeController
AdminUserController
AdminQuotaController
AdminProviderController
AdminSettingController
AdminAuditController
```

DTO + Problem mapping。

## 29.4 tutor-infrastructure

新增：

```text
Spring Security adapters
JWT encoder / decoder
MySQL repositories
Refresh token persistence
AES-GCM SecretCipher
Provider config persistence
System settings persistence
Audit persistence
```

## 29.5 tutor-agent

继续负责：

```text
Agent / Provider execution
```

不要把：

```text
User authentication
Admin CRUD
Quota database
```

塞进 tutor-agent。

## 29.6 tutor-observability

只做：

```text
metrics
tracing
model call telemetry
```

不得依赖业务 Controller。

---

# 30. Database Migration 建议

Migration 名称示意：

```text
V11__saas_account_identity.sql
V12__saas_rbac.sql
V13__saas_auth_session.sql
V14__saas_quota.sql
V15__saas_system_settings.sql
V16__saas_ai_provider_config.sql
V17__saas_admin_audit.sql
V18__saas_ai_call_usage.sql
```

实际 Codex 可在实现时调整拆分，但：

- 不得超过一个 Migration 包含过多无关领域；
- FK / unique / index 必须完整；
- Migration 必须通过现有 MySQL Testcontainers smoke test。

---

# 31. Index / Constraint 要求

至少：

```text
app_user.email_normalized UNIQUE

app_user_role(user_id, role_id) UNIQUE

auth_refresh_session.token_hash UNIQUE
auth_refresh_session(user_id, revoked_at_utc)

daily_quota_usage(user_id, quota_date) UNIQUE

quota_consumption.request_id UNIQUE
quota_consumption(user_id, idempotency_key) UNIQUE

ai_provider_config.provider_code UNIQUE

admin_audit_log(actor_user_id, created_at_utc)
admin_audit_log(target_type, target_key, created_at_utc)

ai_call_usage(user_id, created_at_utc)
ai_call_usage(provider_code, created_at_utc)
```

---

# 32. Security Configuration

Spring Security 路由规则：

```text
permitAll:
  POST /api/v1/auth/register
  POST /api/v1/auth/login
  POST /api/v1/auth/refresh
  POST /api/v1/auth/password/forgot
  POST /api/v1/auth/password/reset
  /actuator/health

authenticated:
  /api/v1/**

authority protected:
  /api/v1/admin/**
```

更细权限通过：

```text
@PreAuthorize
```

Actuator：

- health 可按当前部署需求暴露；
- 其他 sensitive actuator 不公开。

---

# 33. Account Disable 语义

Admin Disable：

```text
status = DISABLED
disabled_at_utc = now
auth_version += 1
revoke all refresh tokens
```

效果：

- 新登录失败；
- Refresh 失败；
- 旧 Access Token 最迟在短 TTL 后失效；
- 如请求级校验 user status，则立即失效。

V1 推荐 Security Filter / CurrentActor resolution 时读取 lightweight account state cache，以支持较快禁用。

---

# 34. Delete 语义

V1 默认：

```text
Soft Delete
```

```text
status = DELETED
deleted_at_utc = now
```

不得立即 Cascade 删除学习数据。

真正 GDPR / data purge / account deletion workflow：

```text
后续独立设计
```

---

# 35. Registration 控制

系统设置：

```text
registration.enabled
```

关闭后：

```http
POST /auth/register
→ 403 REGISTRATION_DISABLED
```

Admin 创建用户 / Invite 属于后续可扩展能力。

UI 原型中的 Invite 可以在 V1 先不实现实际邮件邀请，如果产品未要求。

---

# 36. Quota Reset 不需要批量凌晨任务

不要每天 00:00：

```text
UPDATE 全体用户 used_today=0
```

推荐：

> 使用 `quota_date` 按日建 usage row。

新的一天：

```text
第一次请求
↓
自动创建新的 daily_quota_usage
```

因此“Reset”是自然发生的，不需要扫描全部用户。

`resetAt` 通过：

```text
quota.reset_timezone
```

计算。

这种设计更适合多用户 SaaS。

---

# 37. API Provider Failure 与 Quota

Provider 失败：

### 情况 A

```text
没有产生可用输出
```

Refund。

### 情况 B

```text
已经向用户返回有效内容
后续 streaming 中断
```

不 Refund。

### 情况 C

系统自动 Provider fallback：

```text
OpenAI fail
↓
DeepSeek fallback
↓
成功
```

仍然只算：

```text
1 quota
```

因为用户只发起了一次业务请求。

`request_id` 必须贯穿 Provider fallback。

---

# 38. Idempotency

现有系统已有 `Idempotency-Key` 概念。

SaaS 改造必须继续使用。

所有消耗额度的 mutation：

```text
Idempotency-Key REQUIRED
```

同一 key：

```text
不得重复创建 session
不得重复产生 quota consumption
不得重复扣额度
```

Quota Consumption 与业务 command 尽量共享：

```text
request_id / idempotency_key
```

---

# 39. Test Strategy

## 39.1 Backend Unit

必须覆盖：

```text
Email normalization
Password encoder
Role / authority mapping
Quota effective limit
Quota reset date
Quota bonus
Unlimited
Secret encryption/decryption
Masking
Problem mapping
```

## 39.2 Backend Integration + Testcontainers

必须覆盖真实 MySQL：

```text
V11+ Flyway migration
Register
Duplicate email
Login
Refresh rotation
Logout
User isolation
Admin authorization
Quota reserve
Quota reset
Quota concurrency
Quota idempotency
Provider configuration
Audit persistence
```

### Quota concurrency P0 test

准备：

```text
remaining = 1
```

并发：

```text
10~20 requests
```

Assert：

```text
success reserve == 1
used increment == 1
others quota exceeded
```

## 39.3 Security Tests

必须证明：

```text
USER -> /admin = 403
anonymous -> user API = 401
User A cannot access User B session
disabled user cannot refresh
API key not returned
refresh token hash only
```

## 39.4 Web

Vitest：

```text
AuthStore
401 refresh flow
Locale
Quota display
Route guard
Error code localization
```

Playwright：

```text
Register/Login
Learner Today
Quota exceeded UX
Admin Login
User management
Quota reset modal
Provider secret masked
zh/en switch
responsive mobile viewport
```

## 39.5 Android

```text
Auth Repository
Token storage
401 refresh
Current user
Quota state
Locale state
Compose login/account/quota
```

---

# 40. UI 验收

以双语 UI 原型作为视觉参考。

## 40.1 Desktop

验证：

```text
Login
Learner Today
Practice
History
Progress
Account
Admin Overview
Users
Providers
Settings
Audit
```

## 40.2 Mobile

验证：

```text
390px width
bottom navigation
single-column cards
no horizontal overflow in learner flow
admin table may use controlled horizontal scroll
```

## 40.3 Language

验证：

```text
zh-CN ↔ en
```

切换后：

- App chrome 即时更新；
- Account 保存 locale；
- 重登保持；
- Web / Android 同步；
- 英语学习正文保持英文。

---

# 41. Deployment / Environment

新增 Production Secret：

```text
JWT signing key / key path
TUTOR_SECRET_ENCRYPTION_KEY
Bootstrap admin secret (one-time only)
```

可配置：

```text
TUTOR_AUTH_LEGACY_USER_KEY_ENABLED
```

P1 Email：

```text
SMTP / Email Provider credentials
```

不得将 Secret 写入：

```text
application.yml
Dockerfile
Jenkinsfile
GitHub
```

生产使用现有 `/opt/english-tutor-agent/shared/production.env` 或更安全 secret mechanism。

---

# 42. 发布顺序

为了避免 Web / Backend 独立 Jenkins 发布造成短暂不兼容：

## Step A

Backend：

- Schema；
- Auth；
- CurrentActor；
- Quota；
- Admin；
- Legacy compatibility flag（仅迁移期）。

## Step B

Web：

- Login/Register；
- Access Token；
- 删除 userKey 生成；
- Quota；
- Admin Console；
- i18n。

## Step C

Android：

- Auth；
- Account；
- Quota；
- locale。

## Step D

关闭：

```text
TUTOR_AUTH_LEGACY_USER_KEY_ENABLED=false
```

并删除旧 Web `X-User-Key` 逻辑。

## Step E

安全验收后再对公网正式开放。

---

# 43. Milestone 计划

以下为 Codex 实施顺序。

---

## SaaS-M1：Identity Schema + Auth Backend

### 目标

建立真实账号与认证。

### Tasks

1. V11 Account Migration；
2. RBAC Schema；
3. PasswordEncoder；
4. Register；
5. Login；
6. Access JWT；
7. Refresh Session；
8. Logout；
9. `/me`；
10. Bootstrap Admin；
11. Auth tests。

### Gate

必须证明：

```text
Email 唯一
USER 登录成功
ADMIN 登录成功
Invalid credentials 不泄露账号存在性
Refresh rotation 正常
```

---

## SaaS-M2：CurrentActor + Multi-user Isolation

### 目标

移除客户端自报身份。

### Tasks

1. CurrentActor abstraction；
2. SecurityContext adapter；
3. 所有已有 learner API audit；
4. userKey header compatibility adapter；
5. Repository 增加 user_id constraint；
6. Session ownership tests；
7. Profile ownership tests；
8. Plan ownership tests；
9. Training ownership tests；
10. Conversation ownership tests。

### Gate

必须有自动化测试证明：

```text
User A 无法读取 / 修改 User B 的任何学习资源
```

---

## SaaS-M3：Daily Quota Engine

### 目标

实现原子每日额度。

### Tasks

1. V14 quota schema；
2. effective policy；
3. quota date/timezone；
4. reserve；
5. commit；
6. refund；
7. stale reservation reconciliation；
8. idempotency integration；
9. `/me/quota`；
10. quota exception；
11. concurrency tests。

### Gate

```text
remaining=1
20 concurrent requests
only 1 succeeds
```

---

## SaaS-M4：Runtime AI Provider + Secret

### 目标

Admin 可运行时配置 Provider。

### Tasks

1. Provider schema；
2. AES-GCM SecretCipher；
3. Config Repository；
4. Provider resolution；
5. Default provider；
6. Enable/Disable；
7. Secret replace；
8. Masking；
9. Fake provider compatibility；
10. Provider tests。

### Gate

证明：

```text
修改 default provider
无需重启
下一次请求使用新 provider
API response/log 不出现完整 secret
```

---

## SaaS-M5：Admin Backend

### 目标

运营管理 API。

### Tasks

1. User list/search；
2. User detail；
3. Enable/Disable；
4. Role；
5. Quota override；
6. Quota reset；
7. Bonus；
8. System Settings；
9. Dashboard；
10. Audit Log；
11. permission tests。

### Gate

```text
USER -> admin API = 403
ADMIN operations audited
```

---

## SaaS-M6：Web Learner SaaS UX + i18n

### 目标

Web 从匿名 userKey 变成真实 SaaS 用户。

### Tasks

1. AuthStore；
2. Login/Register；
3. Token refresh；
4. ApiClient Bearer；
5. remove local userKey；
6. Today quota；
7. Account quota；
8. error handling；
9. zh-CN/en；
10. responsive；
11. Playwright。

### Gate

用户完整闭环：

```text
register
↓
login
↓
onboarding
↓
practice
↓
quota consumed
↓
history
↓
logout/login
↓
data preserved
```

---

## SaaS-M7：Web Admin Console

### 目标

实现 UI 原型管理端。

### Tasks

1. Admin Route；
2. Overview；
3. Users；
4. Quota modal/drawer；
5. Providers；
6. Settings；
7. Audit；
8. bilingual；
9. responsive；
10. E2E。

### Gate

UI 与 API 完整联通，不使用 mock production data。

---

## SaaS-M8：Android Learner Auth + Quota + i18n

### 目标

Android 普通学习用户 SaaS 化。

### Tasks

1. Login/Register；
2. Auth Repository；
3. Secure refresh token；
4. authenticated API；
5. Account；
6. Quota；
7. quota error UX；
8. zh/en；
9. logout；
10. tests。

### Gate

Android 与 Web 登录同邮箱：

```text
看到同一个学习用户
同一 quota
同一学习数据
```

---

## SaaS-M9：Hardening + Legacy Cleanup

### 目标

生产可用。

### Tasks

1. 关闭 Legacy X-User-Key；
2. Claim existing user；
3. Security review；
4. Secret scan；
5. DB backup/rollback runbook；
6. Quota reconciliation test；
7. Provider failover test；
8. Load / concurrency smoke；
9. Deployment docs；
10. Update README / PRD / Roadmap。

### Gate

```text
No legacy identity
No plaintext secrets
No cross-user access
Quota concurrency passed
Admin audit passed
Web + Android smoke passed
```

---

# 44. 明确不做

本次 SaaS Foundation 不实现：

```text
Stripe
PayPal
支付宝
微信支付

Subscription Billing
Plan Purchase
Auto Renewal
Coupon
Invoice

Organization
Workspace
Team Billing

Enterprise Tenant Isolation

复杂 RBAC 可视化编辑器

Marketing CRM
Affiliate
Referral
```

未来 Billing 应建立在：

```text
Account
Role
Quota
Usage
Provider Cost
```

之上，不要提前混入本轮。

---

# 45. Definition of Done

本次 SaaS Foundation 只有同时满足以下条件才算完成：

### Identity

- [ ] Email 注册/登录；
- [ ] Email 唯一；
- [ ] USER / ADMIN；
- [ ] Password hash；
- [ ] Refresh rotation；
- [ ] Logout / disable invalidation。

### Isolation

- [ ] 所有用户资源从 CurrentActor 获取 user_id；
- [ ] 无客户端可信 userId；
- [ ] Cross-user 自动化测试通过。

### Quota

- [ ] 每日额度；
- [ ] 系统默认；
- [ ] 用户 override；
- [ ] Reset；
- [ ] Bonus；
- [ ] Unlimited；
- [ ] 并发原子；
- [ ] Retry 不重复扣；
- [ ] Provider fail before output 可 refund。

### Admin

- [ ] User Management；
- [ ] Quota；
- [ ] Provider；
- [ ] System Settings；
- [ ] Audit；
- [ ] USER 无 Admin 权限。

### Secret

- [ ] DB 无明文 API Key；
- [ ] API 不回显；
- [ ] Log 不泄露；
- [ ] Encryption key 不进 Git。

### UI

- [ ] Web Learner；
- [ ] Web Admin；
- [ ] Android Learner；
- [ ] zh-CN / en；
- [ ] responsive；
- [ ] quota UX。

### Migration

- [ ] Flyway V11+；
- [ ] existing data retained；
- [ ] existing real user claim strategy；
- [ ] Legacy X-User-Key 最终关闭。

---

# 46. Codex 开工 Prompt

建议在完成本文件提交后，使用以下提示启动实现：

```text
请实现 English Tutor Agent 的 SaaS Foundation 改造。

在开始写代码前，必须阅读：
- 01_PRODUCT_REQUIREMENTS.md
- 02_AGENT_DESIGN.md
- 03_TECHNICAL_DESIGN.md
- 04_DEVELOPMENT_ROADMAP.md
- 08_SAAS_FOUNDATION_DESIGN.md
- docs/ui/english-tutor-saas-ui-prototype-v1.html（如果仓库中已添加）

当前仓库代码是最终技术事实来源。

实施要求：
1. 严格遵守 08_SAAS_FOUNDATION_DESIGN.md 的模块边界、数据隔离和安全规则。
2. 不修改已有 Flyway V1-V10，只新增 migration。
3. 不重写现有英语学习核心业务。
4. 按 SaaS-M1 → SaaS-M9 顺序开发。
5. 一次只完成一个 Milestone。
6. 每个 Milestone 内可自主完成所有子任务、测试和必要的小修复。
7. 每个 Milestone 结束必须：
   - 运行相关测试；
   - 给出修改文件清单；
   - 给出数据库 migration 清单；
   - 给出 API 变化；
   - 给出尚未完成事项；
   - 确认是否满足该 Milestone Gate。
8. 如果发现设计与当前代码冲突：
   - 优先保留产品语义和安全边界；
   - 基于现有架构做最小适配；
   - 不擅自做大范围架构重写；
   - 在结果中说明冲突和采用的处理方式。

现在先执行 SaaS-M1：Identity Schema + Auth Backend。
不要提前实现后续 Milestone。
```

---

# 47. 最终架构摘要

```text
                         ┌──────────────────────┐
                         │ Web / Android        │
                         │ zh-CN / en           │
                         └──────────┬───────────┘
                                    │
                              Auth / JWT
                                    │
                       ┌────────────▼────────────┐
                       │ Security + CurrentActor │
                       └───────┬─────────┬───────┘
                               │         │
                            USER       ADMIN
                               │         │
                  ┌────────────▼─┐   ┌──▼───────────────┐
                  │Learning Core │   │SaaS Operations   │
                  │existing      │   │Users / Settings  │
                  └──────┬───────┘   └─────┬────────────┘
                         │                 │
                    QuotaGuard       Audit / RBAC
                         │                 │
                         └───────┬─────────┘
                                 │
                       ProviderConfiguration
                                 │
                       Encrypted Provider Secret
                                 │
                        Agent / Model Provider
                                 │
                          LLM / ASR / TTS
```

核心变化只有一句话：

> **把原来的“匿名 userKey + 英语学习应用”，升级成“可信身份 + 多用户隔离 + 每日额度 + 管理后台 + 运行时 AI 配置”的 SaaS 平台，同时不破坏原有英语学习核心。**
