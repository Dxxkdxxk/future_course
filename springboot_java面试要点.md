## 项目 Spring Boot + Java 面试要点（实战/底层逻辑向）

> 面试准备目标：说清楚“为什么这么做”、以及“Spring 内部/底层机制如何落到本项目实现”。

### 1. 技术栈速览（从 `pom.xml` 推导）
- Spring Boot：`spring-boot-starter-web`、`spring-boot-starter-security`、`spring-boot-starter-websocket`
- 安全认证与鉴权：JWT（`io.jsonwebtoken:jjwt`）、BCrypt（`BCryptPasswordEncoder`）
- 数据持久化：MyBatis-Plus（`mybatis-plus-boot-starter`）、MyBatis XML（`src/main/resources/mapper/*.xml`）、MySQL（`mysql-connector-java`）
- 缓存：Redis（`spring-boot-starter-data-redis` + 自定义 `RedisTemplate` 序列化）
- 图谱：Neo4j（`org.neo4j.driver:neo4j-java-driver` + APOC 语法）
- 对象存储：MinIO（`MinioClient`、预签名 URL）
- 实时能力：WebSocket（`TextWebSocketHandler`）
- 异步与定时：`@Async`、`@Scheduled`
- 文档解析与 OCR：Apache POI（Word）、PDFBox（PDF文本/渲染）、Tesseract（OCR）、OpenCV（图像预处理）、Jsoup（解析 HOCR）、ZXing（二维码生成）
- 外部智能体/AI：Dify（`DifyService`）与 DeepSeek（`DeepSeekService`），HTTP 使用 `RestTemplate`
- 工程与接口文档：`spring-boot-starter-test`、`knife4j-openapi3-jakarta-spring-boot-starter`（依赖已引入，具体注解使用需结合代码补充）

### 2. Spring Boot 启动与工程组织
1. 启动入口
   - `SpringSecurityJwt`：`@SpringBootApplication` + `@MapperScan("com.lzlz.springboot.security.mapper")`
   - 面试追问点：`@MapperScan` 让 MyBatis mapper 接口被注册；结合 MyBatis-Plus 的 `ServiceImpl`，可解释“通用 CRUD + 自定义 SQL XML”如何并存。
2. 配置管理
   - `application.yaml` 管理：`server.port`、MySQL `spring.datasource.*`、Redis `spring.data.redis.*`、Neo4j `neo4j.*`、MinIO `minio.*`、AI（`dify.api.*`）、上传大小、缓存 TTL（`cache.ttl.*`）等。
   - 面试追问点：为什么把 TTL、图谱限制（`graph.limit.*`）放配置？便于动态调参与环境隔离。
3. 循环依赖
   - `spring.main.allow-circular-references: true` 存在循环依赖可能风险。
   - 面试追问点：生产环境通常不建议长期依赖循环引用；应尽量调整依赖方向或引入接口抽象。

### 3. Spring Security（JWT 无状态鉴权）——高频面试点
本项目核心安全链路（认证与授权）可按“Filter Chain -> token解析 -> SecurityContext 写入 -> 授权匹配”讲清楚。

1. 过滤器链与无状态策略
   - `SecurityConfig#securityFilterChain(HttpSecurity)`：
     - `csrf.disable()`、`httpBasic.disable()`
     - `SessionCreationPolicy.STATELESS`：强制无会话
     - `addFilterBefore(customFilter, UsernamePasswordAuthenticationFilter.class)`：JWT 过滤器在 Spring 默认用户名密码过滤器之前执行
   - 面试追问点：`requestMatchers` 的匹配顺序与“更具体/更通用规则”叠加时的行为。
2. JWT 解析与认证
   - `JwtTokenAuthenticationFilter`：
     - 对 `/api/v1/auth/` 放行
     - 其他请求：从 `Authorization: Bearer <token>` 解析并验证
     - token有效则 `SecurityContextHolder.getContext().setAuthentication(auth)`
   - `JwtTokenProvider`：
     - `createToken()`：HS256 签名 + claims（`subject`=username，`roles`写入但最终权限来源仍依赖 `UserDetails`）
     - `validateToken()`：重点校验过期时间，非法 token 抛 `InvalidJwtAuthenticationException`
     - `getAuthentication()`：加载用户并创建 `UsernamePasswordAuthenticationToken(userDetails, "", authorities)`
   - 面试追问点：
     - token claim 里角色与实际权限如何一致（本项目权限从 DB 的 `UserDetails`Authorities 走）。
     - token过期后的处理策略（本项目只做 401；是否需要 refresh token 需进一步讨论）。
3. 密码与用户加载
   - `SecurityConfig#passwordEncoder()`：`BCryptPasswordEncoder`
   - `CustomUserDetailsService`：
     - `loadUserByUsername()`：基于 `UserMapper` 查询 `username`
     - `createUser()`：写入前对密码进行 BCrypt 编码
   - 面试追问点：`loadUserByUsername` 返回 `null` 的正确性/异常处理（更推荐抛 `UsernameNotFoundException`，便于链路定位）。
4. 认证失败响应
   - `JwtAuthenticationEntryPoint`：统一 `401 Unauthorized` 返回错误信息
   - `GlobalExceptionHandler`：对业务异常（如 `ResourceNotFoundException`、`CustomGraphException`）返回自定义 `ApiResponse`

### 4. Controller 实战写法（接口组织、DTO、返回体）
1. 统一返回体与异常翻译
   - `GlobalExceptionHandler` 把异常转换为 `ApiResponse.error(code,msg)` 并设置对应 HTTP 状态码。
2. 典型 REST 风格控制器
   - `AuthController`：`/api/v1/auth/login`、`/register`
   - 业务控制器大量使用：路径参数（`@PathVariable`）、请求体（`@RequestBody`）、文件上传（`MultipartFile`）、以及 `@AuthenticationPrincipal` + `CurrentUserResolver`
3. 访问用户获取方式
   - `CurrentUserResolver#requireUser()`：从 `SecurityContextHolder` 取 `principal`，未认证抛 `401`
   - 面试追问点：`AuthenticationPrincipal` 注入时 principal 类型与 `UserDetails` 绑定如何保证一致。

### 5. 持久化层：MyBatis-Plus + XML Mapper（实战与底层逻辑）
1. 数据访问两种路径并存
   - 通用 CRUD：`QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question>`
   - 自定义查询：`src/main/resources/mapper/*.xml`
     - `SelfAssessmentTaskMapper.xml`：带 `NOT EXISTS` 的“学生未做过任务”的筛选逻辑
     - `ExtendReadingMapper.xml`、`AnnotationMapper.xml`：按 student/resource/chapter 等组合条件查询
2. MyBatis-Plus 查询构造器
   - `QueryWrapper`/`LambdaQueryWrapper`：链式构造条件、排序、in/like 等。
   - 面试追问点：`wrapper.and(w -> ...)` 如何避免条件优先级/括号歧义。
3. 事务边界与一致性
   - 多处使用 `@Transactional(rollbackFor = Exception.class)`：
     - 如 `GraphBuildServiceImpl#buildGraphFromDocument`
     - `HomeworkServiceImpl#createHomework`、`QuestionServiceImpl#importQuestions` 等
   - 面试追问点（重要）：MySQL事务与 Neo4j写入是不同资源管理器；本项目使用 `@Transactional` 主要保证 MySQL 数据回滚，但 Neo4j 的最终一致性需要额外关注（可能需要补偿/重试/幂等策略）。

### 6. Redis 缓存（序列化、TTL、失效策略）——实战高频
1. RedisTemplate 与 JSON 序列化
   - `RedisConfig`：自定义 `RedisTemplate<String,Object>`，使用 `GenericJackson2JsonRedisSerializer` + `JavaTimeModule`
   - 面试追问点：对象存取的兼容性（泛型列表要用 `TypeReference`）、时间类型如何序列化。
2. 缓存抽象封装
   - `RedisCacheService`：
     - `set(key,value,Duration ttl)`：写入 TTL
     - `get(key,Class<T>)` 与 `get(key,TypeReference<T>)`：支持单对象与泛型集合
     - `deleteByPrefix(prefix)`：通过 `redisTemplate.keys()` 删除前缀匹配（需要注意性能风险，面试可提替代如 scan）。
3. Key 规范
   - `RedisKeys`：集中定义 key 生成规则（如 `course:detail:<id>`、`question:list:course:<id>:q:<signature>`）
4. 缓存“读-写穿透”的实战用法
   - `CourseServiceImpl#getCourseById()`：查缓存 -> 未命中查库 -> 回填 TTL
   - `QuestionServiceImpl#getQuestions()`：基于筛选参数拼“签名”（`buildQuerySignature`）生成 key，保证不同查询条件互不污染缓存
   - `QuestionServiceImpl#createQuestion()`、`importQuestions()`：写入成功后执行 `redisCacheService.deleteByPrefix(...)` 实现失效
5. 面试追问点（很常见）
   - 如何避免缓存击穿/雪崩（本项目目前是 TTL 回填与失效策略为主，未看到互斥锁/逻辑过期；可以在答题时提改进方向）。
   - `keys(prefix*)` 在大规模场景的风险与替代方案。

### 7. WebSocket 实时推送（解析结果回传）
1. WebSocket 注册与跨域
   - `WebSocketConfig`：`@EnableWebSocket` + `registry.addHandler(new TextbookWebSocketHandler(), "/ws/textbook")`
   - `.setAllowedOrigins("*")` / `.setAllowedOriginPatterns("*")`：强调局域网跨 IP 场景。
2. 会话管理与并发
   - `TextbookWebSocketHandler`：
     - `ConcurrentHashMap<String, WebSocketSession> SESSION_MAP` 保存连接
     - `afterConnectionEstablished` 以 `clientIp + sessionId` 作为 key
     - 支持按目标 IP 推送（由消息中的 `targetIp` 决定）
   - `ParseCallbackHandler`：
     - 通过连接参数 `uploaderId` 存储会话并推送 `TextbookParseResult`
3. 面试追问点
   - session key 设计、断线清理（`afterConnectionClosed`）、以及线程安全。
   - 如何与 `@Async`/解析任务解耦（解析完成后通过 handler 通知前端）。

### 8. 异步与定时任务（Async + Scheduling）
1. 异步执行
   - `AsyncConfig`：`@EnableAsync` + `TaskExecutor taskExecutor()`（线程池参数：core/max/queue/CallerRunsPolicy）
   - `DocumentParseService#asyncParseChapter()`：`@Async` 异步解析教材并更新数据库状态（PARSING/SUCCESS/FAIL）
2. 定时状态维护
   - `SignTaskSchedule#updateExpiredSignTask()`：`@EnableScheduling` + `@Scheduled(cron="0 */1 * * * ?")` 更新过期签到
   - `SelfAssessmentSchedule#updateExpiredTask()`：每日凌晨 `cron="0 0 0 * * ?"` 更新过期自评任务
3. 面试追问点
   - `@Async` 返回值与异常处理（本项目异步方法是 void；异常是否能被上层捕获需要说明）。
   - 线程池与任务堆积风险，CallerRunsPolicy 可能导致调用线程阻塞（要会解释“为什么这么选”）。

### 9. 对象存储 MinIO（上传与下载）
1. MinIO 客户端配置
   - `MinIOConfig`：使用 `MinioClient.builder().endpoint(...).credentials(...)`
2. 文件上传
   - `MinIOService#uploadFile()`：
     - bucket 不存在则创建
     - `UUID + suffix` 作为 objectName，返回 objectName
3. 文件访问
   - `MinIOService#getPresignedUrl()`：GET 预签名 URL，控制过期时间
4. 面试追问点
   - 为什么用预签名 URL（避免服务端做代理流、提升安全与性能）。
   - 上传大小与异常处理（本项目与 `spring.servlet.multipart.*` 配套）。

### 10. 文档解析与 OCR（实战最容易追问的链路）
本项目把“解析”做成可异步执行、可 WebSocket 回传的流水线，通常面试会按“输入 -> 处理 -> 输出 -> 保障”问。

1. Word/ PDF 解析入口
   - `DocumentParseService#parseLocalFile()`（本地测试）与 `#asyncParseChapter()`（业务入口）
   - Word：Apache POI（`XWPFDocument`/`XWPFParagraph`），通过字号/样式 + 正则规则识别章节（支持章/节/小节三级）
   - PDF：
     - 先抽样检测是否存在文本层（`PDFTextStripper`）
     - 无文本层：走增强 OCR 分支（OpenCV + Tesseract）
2. OCR 增强版策略
   - 图像增强：将 PDF 页面渲染为高 DPI 图片，必要时降噪/二值化/自适应阈值
   - 关键工程点：设置 DPI 元数据以减少 Tesseract 警告并提升识别稳定性
   - HOCR 解析：Tesseract 输出 HOCR 后用 Jsoup 解析 `div.ocr_line`、bbox 坐标等排版特征
   - 标题识别：结合正则（1/2/3级标题格式）与布局特征（居中/顶部区域/字号）
3. 解析结果落库
   - 生成 `Chapter` 节点（MySQL），并更新教材 `status` 状态流转
4. 面试追问点（非常常见）
   - 如何保证解析的可解释性与可调参（阈值、正则、字体字号阈值、TOC过滤等）。
   - OCR 性能与幂等：并发解析如何避免重复写入。
   - 异步任务的状态回传：WebSocket vs 轮询的取舍。

### 11. Neo4j 图谱构建（APOC + 限额 + 结构化）
1. 驱动与连通性校验
   - `Neo4jConfig#neo4jDriver()`：读取 `neo4j.uri/username/password`，调用 `verifyConnectivity()`
2. 图谱写入仓库（核心）
   - `GraphRepository#saveGraph()`：
     - 使用 `session.writeTransaction(tx -> ...)`
     - 批量 `UNWIND` 节点创建 + `apoc.create.node`
     - 批量边创建 + `apoc.create.relationship`
   - `GraphRepository#getGraphComponents()`：返回 nodes 与 edges，节点与关系均带 `graphId` 约束。
3. 节点/边增删改与校验
   - `createNodeAndLink()`：对节点总数与层级深度做校验（基于 `graph.limit.*`）
   - `updateNodeProperties()`：使用 Cypher + APOC validate 检查重名
   - `deleteNode()`：先检查是否有子节点，再级联删除资源与节点（`DETACH DELETE`）
4. 从文档构建图谱（MySQL + Neo4j“双存”）
   - `GraphBuildServiceImpl#buildGraphFromDocument()`：
     - 从 XLSX 读取 Sheet：`知识树`（层级 contains），`关系`（网状边）
     - 生成节点 `node-<uuid>` 与边 `edge-<uuid>`，并做去重
     - MySQL 落 `GraphMetadata` 后拿到 `graphId`，再调用 `GraphRepository#saveGraph`
5. 面试追问点
   - Neo4j 的事务在本项目如何与 MySQL 一致性协作（再次强调：不是同一个事务资源管理器）。
   - APOC 的使用目的：动态标签/动态关系类型创建能力。
   - “限额熔断”如何保护系统稳定性（maxDepth/maxNodes）。

### 12. 外部 AI 接口集成（Dify/DeepSeek）
1. Dify（文件上传到知识库 + chat-messages）
   - `DifyService`：
     - `uploadFileToDify()`：`/files/upload`，multipart 上传，拿到 `file_id` 和 `source_url`
     - `sendChatRequest()`：`/chat-messages`，输入 `course_id`、`query`、`user` 等
     - `sendFileToDatabase()`：`/datasets/<dbId>/document/create-by-file` 上传到知识库
2. DeepSeek（chat completions）
   - `DeepSeekService`：直接调用 `https://api.deepseek.com/v1/chat/completions`
3. 控制器入口
   - `DifyController`：批改作业、批改实验、生成题库、生成教材大纲/富媒体/重难点等
4. 面试追问点
   - HTTP 集成的健壮性：超时、重试、幂等、对外依赖降级（项目里目前以 try/catch 返回字符串为主，可在答题时提出改进）。
   - 返回内容结构解析：JSON 数组与字段缺失的处理（本项目用 Jackson `JsonNode`）。

### 13. 安全与工程整改建议（面试加分点）
1. 凭证与密钥安全
   - `application.yaml` 中包含密钥/密码（JWT/Redis/Neo4j/MinIO/Dify 等），`DeepSeekService` 里也有硬编码 API Key。
   - 建议：改为环境变量/Secret 管理（面试可直接说“不要把密钥放到仓库或明文配置中”）。
2. 性能风险点
   - `RedisCacheService#deleteByPrefix()` 使用 `redisTemplate.keys()`：在大 key 空间下风险高，建议用 scan 替代（面试可提出）。
3. 权限匹配与规则顺序
   - `SecurityConfig` 内存在较宽泛的 `permitAll` 规则与更细粒度的 `hasAuthority` 规则并存，需要严格确认 match 顺序与效果。

### 14. 建议你如何用这份文档面试作答（简版）
- 每讲一个模块，按顺序说：`本项目为什么要做` -> `关键类/配置在哪` -> `底层机制如何工作` -> `面试追问怎么答（一致性/性能/安全）`。

