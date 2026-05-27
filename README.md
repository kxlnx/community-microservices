# Community Microservices

校园交流项目，基于 Spring Boot 3.4.4 + Spring Cloud 2024.0.0 + JDK 21。

---

## 一、整体架构

```
                          ┌─────────────────────┐
                          │   Nginx / 浏览器      │
                          └─────────┬───────────┘
                                    │
                          ┌─────────▼───────────┐
                          │   Gateway :8000     │
                          │   JWT 鉴权 + 路由    │
                          └──┬──┬──┬──┬──┬─────┘
                             │  │  │  │  │
              ┌──────────────┘  │  │  │  └──────────────┐
              ▼                 │  │  │                 ▼
         ┌─────────┐            │  │  │            ┌─────────┐
         │  user   │◄──Feign───┘  │  │            │ search  │
         │ :8086   │              │  │            │ :8085   │
         │   A     │────Kafka─────┼──┼──Kafka─────│   B     │
         └────┬────┘              │  │            └────┬────┘
              │                   │  │                 │
    ┌─────────▼─────────┐  ┌──────▼──▼──────┐  ┌───────▼───────┐
    │  MySQL user       │  │    Kafka       │  │  MySQL search │
    │  Redis            │  │  comment/like  │  │  ES           │
    │  ES               │  │  follow/publish│  │               │
    └───────────────────┘  │  delete/share  │  └───────────────┘
                           └────────────────┘
         ┌─────────┐              ▲  ▲              ┌─────────┐
         │  post   │──Feign───────┘  │              │ message │
         │ :8087   │                 │              │ :8084   │
         │   A     │───────Feign─────┘──Kafka──────│   B     │
         └────┬────┘                                └────┬────┘
              │                                          │
    ┌─────────▼─────────┐              ┌─────────────────▼──┐
    │  MySQL post       │              │  MySQL interact    │
    │  Caffeine (L1)    │              │  MySQL message     │
    │  Quartz           │              │                    │
    └───────────────────┘              └────────────────────┘

      ┌─────────┐
      │interact │
      │ :8083   │────Kafka──► Topic: comment, like
      │   B     │
      └────┬────┘
           │
  ┌────────▼─────────┐
  │ MySQL interact   │
  │ Redis (远程 A)    │
  └──────────────────┘
```

---

## 二、模块目录

```
community-microservices/
├── pom.xml                         父 POM
│
├── community-common/               共享库
│   ├── entity/        User, DiscussPost, Comment, Message, LoginTicket, Event, Page
│   ├── util/          CommunityConstant, CommunityUtil, RedisKeyUtil, HostHolder,
│   │                  SensitiveFilter, MailClient, BusinessException, Result
│   ├── feign/         PostClient, UserClient, CommentClient, SearchClient
│   ├── config/        RedisConfig
│   ├── event/         EventProducer
│   └── handler/       GlobalExceptionHandler
│
├── community-gateway/              API 网关 :8000
│   └── config/         JwtGatewayProperties, JwtAuthFilter
│
├── community-user/                 用户服务 :8086
│   ├── controller/    AuthController, UserController, FollowController,
│   │                  UserInternalController
│   ├── service/       UserService, FollowService
│   ├── dao/           UserMapper
│   ├── config/        SecurityConfig
│   └── util/          CaptchaUtil, JwtUtil
│
├── community-post/                 帖子服务 :8087
│   ├── controller/    HomeController, DiscussPostController,
│   │                  DiscussPostInternalController, SearchController
│   ├── service/       DiscussPostService, UserService (Feign封装),
│   │                  CommentService (Feign封装), LikeService (Redis查询)
│   ├── dao/           DiscussPostMapper
│   └── quartz/        PostScoreRefreshJob
│
├── community-interact/             互动服务 :8083
│   ├── controller/    CommentController, LikeController,
│   │                  CommentInternalController, LikeInternalController
│   ├── service/       CommentService, LikeService, DiscussPostService
│   └── dao/           CommentMapper
│
├── community-message/             消息服务 :8084
│   ├── controller/    MessageController
│   ├── service/       MessageService
│   ├── event/         EventConsumer
│   └── dao/           MessageMapper
│
└── community-search/              搜索服务 :8085
    ├── controller/     SearchController
    ├── service/        ElasticsearchService
    └── event/          EventConsumer
```

---

## 三、分布式特征

```
┌──────────────────────────────────────────────────────────┐
│                     数据隔离                              │
│                                                          │
│  user ─── MySQL community_user    (user, login_ticket)   │
│  post ─── MySQL community_post    (discuss_post, qrtz_*) │
│  interact ─ MySQL community_interact (comment)           │
│  message ── MySQL community_message  (message)           │
│                                                          │
│  每个服务只能访问自己的数据库                               │
│  跨服务查数据必须通过 Feign 或 Kafka                       │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│                     服务通信                              │
│                                                          │
│  ┌──────────── Feign (同步 HTTP) ────────────┐           │
│  │ post ──→ interact  (查评论/点赞)           │           │
│  │ post ──→ search    (搜索API)              │           │
│  │ user ──→ interact  (查评论/点赞数)         │           │
│  │ message → user     (查用户信息)             │           │
│  │ interact→ post     (加评论数/查帖子)       │           │
│  │ search ─→ post     (ES同步查完整帖子)      │           │
│  └──────────────────────────────────────────┘           │
│                                                          │
│  ┌──────────── Kafka (异步事件) ─────────────┐           │
│  │ interact ──→ publish  ──→ search 同步 ES  │          │
│  │ post    ──→ publish  ──→ search 同步 ES  │          │
│  │ post    ──→ delete   ──→ search 删 ES   │          │
│  │ interact ──→ comment  ──→ message 发通知 │          │
│  │ interact ──→ like     ──→ message 发通知 │          │
│  │ user    ──→ follow   ──→ message 发通知 │          │
│  └──────────────────────────────────────────┘           │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│                     技术组件                              │
│                                                          │
│  Redis   ──  点赞 (Set) · 关注 (ZSet) · 缓存 · 验证码     │
│  ES      ──  帖子全文搜索 · 高亮                          │
│  Kafka   ──  评论/点赞/关注通知 · 搜索索引同步              │
│  Caffeine──  帖子列表 L1 缓存 (post)                      │
│  Quartz  ──  帖子热度定时计算 (post)                      │
│  JWT     ──  无状态认证 (Cookie + Gateway 注入 Header)    │
└──────────────────────────────────────────────────────────┘
```

---

## 四、双机部署

```
机器 A (Windows 11, 机器A_IP)     机器 B (Ubuntu, 机器B_IP)
┌────────────────────────────┐       ┌──────────────────────────────┐
│ MySQL user (3306)          │       │ MySQL interact (Docker:3309) │
│ MySQL post (3306)          │       │ MySQL message  (Docker:3310) │
│ Redis (6379)               │       │ ZooKeeper + Kafka (9092)     │
│ ES (9200)                  │       │                              │
│                            │       │ interact :8083               │
│ user :8086                 │       │ message  :8084               │
│ post :8087                 │       │ search   :8085               │
│ Gateway :8000              │       │                              │
└────────────────────────────┘       └──────────────────────────────┘
```

---

## 五、配置体系

```
application.yml (提交)
  └── 全用 ${ENV_VAR} 占位符，无默认值，无私密信息

application-local.yml (gitignored)
  └── 本地开发默认值 (DB_PASSWORD, JWT_SECRET 等)

.env.example (提交)
  └── 环境变量模板，无真实密码

.env (gitignored)
  └── 生产/双机部署真实配置
```

---

## 六、启动（单机）

```bash
mvn install -DskipTests

# 三个终端分别启动（所有服务默认连 localhost）
mvn -pl community-gateway spring-boot:run   # :8000
mvn -pl community-user spring-boot:run      # :8086
mvn -pl community-post spring-boot:run      # :8087
mvn -pl community-interact spring-boot:run  # :8083
mvn -pl community-message spring-boot:run   # :8084
mvn -pl community-search spring-boot:run    # :8085
```

浏览器 `http://localhost:8000/`。

## 七、双机部署

详细指南见 `docx/双机部署指南.md`（本地文档，不提交 Git）。

核心思路：

```
机器 A（对外入口）              机器 B（异步 + 数据）
Gateway + user + post          interact + message + search
MySQL + Redis + ES             MySQL + Kafka
```

**A 启动：**

```bash
mvn install -DskipTests
mvn -pl community-user spring-boot:run
mvn -pl community-post spring-boot:run
mvn -pl community-gateway spring-boot:run
```

**B 启动：**

```bash
# 1. 中间件用 Docker
docker-compose -f docker-compose-b.yml up -d mysql-interact mysql-message zookeeper kafka

# 2. 微服务
mvn install -N -DskipTests && mvn install -pl community-common -DskipTests
FEIGN_CLIENT_POST_URL=http://机器A_IP:8087 SPRING_DATA_REDIS_HOST=机器A_IP mvn -pl community-interact spring-boot:run
mvn -pl community-message spring-boot:run
SPRING_ELASTICSEARCH_URIS=http://机器A_IP:9200 mvn -pl community-search spring-boot:run
```

**需设环境变量（B 机器）：**

| 变量 | 值（示例） |
|------|-----------|
| `FEIGN_CLIENT_POST_URL` | `http://机器A_IP:8087` |
| `SPRING_DATA_REDIS_HOST` | `机器A_IP` |
| `SPRING_ELASTICSEARCH_URIS` | `http://机器A_IP:9200` |

## 八、配置外部化

所有敏感信息和跨服务地址通过环境变量注入，yaml 中不存默认值：

```yaml
# 提交到 Git 的写法（无默认值，无敏感信息）
password: ${DB_PASSWORD}
secret: ${JWT_SECRET}
bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}

# 本地开发：复制 application-local.yml.example → application-local.yml
# 生产部署：设置环境变量或 .env 文件
```

---

## 技术栈

`Spring Boot 3.4.4` `Spring Cloud Gateway` `OpenFeign` `Spring Security` `Spring Data Redis/ES` `MyBatis 3.0` `Kafka` `MySQL 8.0` `Thymeleaf` `JWT (jjwt 0.12)` `Quartz` `Caffeine` `Lombok`
