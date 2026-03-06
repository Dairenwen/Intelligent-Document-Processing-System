# DocAI 智能文档处理系统 -- 详细项目说明书

---

## 一、项目背景

本项目为中国大学生服务外包创新创业大赛参赛作品。针对企业日常文档处理中人工录入效率低、格式不统一、重复填表易出错等问题，设计并实现了基于大语言模型的智能文档处理系统。

系统名称：DocAI -- Intelligent Document Processing System

---

## 二、目标与指标

### 功能目标
1. 支持 Word、Excel、TXT、Markdown 四种格式文档的上传与解析
2. 基于 AI 大模型自动提取文档关键信息（数值、实体、事实等）
3. 上传模板文件后自动从用户文档库中匹配数据并填充
4. 提供 AI 对话、智能写作、文本润色、数据分析等辅助功能
5. 多用户系统，数据按用户隔离

### 性能指标
| 指标 | 要求 |
|------|------|
| 填充准确率 | >= 80% |
| 单文档处理时间 | <= 90 秒 |
| 并发支持 | 3 个 AI 请求同时处理 |
| 上下文窗口 | 200K tokens (GLM-4.7-Flash) |
| 文档大小上限 | 50MB |

---

## 三、系统模块

### 模块一：文档智能编辑操作交互

该模块提供基于 AI 的文档编辑辅助功能。

**功能清单：**
- AI 智能对话：支持关联已上传文档进行上下文问答
- 对话历史持久化：按用户和文档独立保存，刷新自动恢复
- 智能写作：支持通知、报告、请示、函、总结、方案、会议纪要、规章制度共 8 种公文类型
- 文本润色：AI 优化文档质量和表达
- 内容导出：生成内容一键下载为 .docx 文件
- 内容回写：AI 结果可直接保存到关联文档

**技术实现：**
- 后端 AIService 集成智谱 GLM-4.7-Flash 模型
- 采用 Semaphore(3) 控制并发，避免 API 过载
- 指数退避重试策略（最多 3 次，初始延迟 2 秒）
- 120 秒超时保护
- 长文档自动截断至 15000 字符

### 模块二：非结构化文档信息提取

该模块负责多格式文档解析和 AI 关键信息抽取。

**功能清单：**
- 支持 .docx / .xlsx / .txt / .md 四种格式
- 上传即解析：文档上传时自动执行文本解析和 AI 信息提取
- AI 结构化提取：从原始文本中提取数值数据、实体、关键事实
- 用户数据隔离：每个用户只能查看自己上传的文档
- 批量上传：支持多文件同时上传

**技术实现：**
- DocParseService 统一解析抽象层
- Apache POI 5.5.1 处理 Word/Excel 文档
- 自定义解析器处理 TXT 和 Markdown
- AI 提取使用专门优化的 Prompt，确保数值精确、实体完整
- 文件存储在服务器 uploads/ 目录，数据库记录元信息

### 模块三：自定义表格数据自动填写（核心功能）

该模块是比赛评分核心功能，实现端到端的自动填表流水线。

**功能清单：**
- 单模板填充：指定源文档 + 上传模板 -> AI 自动匹配填充
- 自动填充：上传模板后自动从用户所有文档中聚合数据
- 批量填充：一次上传多个模板，返回 ZIP 压缩包
- 模板识别：支持占位符模式和空单元格模式
- 预览功能：填充后可在线预览结果

**处理流程：**
1. 上传源文档（docx/xlsx/txt/md）
2. 系统解析文档并通过 AI 提取结构化信息
3. 上传模板文件（Word 或 Excel）
4. AI 分析模板结构，识别待填充字段
5. AI 从用户文档提取信息中匹配数据
6. 自动填入模板对应位置
7. 返回填充完成的文件供下载

**模板格式说明：**
- Word 模板：使用 {{字段名}} 标记需要填充的位置
- Excel 模板：留空的单元格会被自动识别为待填充目标
- 两种模式可混合使用

---

## 四、技术栈详情

### 后端

| 组件 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 运行时环境 |
| Spring Boot | 3.5.11 | 核心框架 |
| Spring Security | 最新 | JWT 认证与 CORS |
| MyBatis-Plus | 3.5.9 | ORM 框架（含分页插件） |
| Apache POI | 5.5.1 | Word/Excel 文档读写 |
| oapi-java-sdk | v4-2.3.0 | 智谱 AI 官方 Java SDK |
| MySQL Connector | 最新 | 数据库驱动 |
| jjwt | 0.12.6 | JWT Token 生成与验证 |

### 前端

| 组件 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5.25 | UI 框架（组合式 API） |
| Vite | 7.3.1 | 构建工具 |
| Element Plus | 2.13.3 | UI 组件库 |
| Pinia | 3.0.4 | 状态管理 |
| Axios | 1.13.6 | HTTP 客户端 |
| Vue Router | 4.6.4 | 路由管理 |

### AI 模型

| 模型 | 上下文 | 费用 | 说明 |
|------|--------|------|------|
| GLM-4.7-Flash | 200K | 免费 | 当前使用，智谱最新基座模型免费版 |
| GLM-4-Flash | 128K | 免费 | 旧版本（已升级） |
| GLM-4.7 | 200K | 2元/百万tokens | 高智商付费版本（备选） |

### 数据库

- MySQL 8.0
- 表结构：users（用户表）、documents（文档表）
- 初始化脚本：docai/docai/src/main/resources/init.sql
- 启动时 DatabaseInitializer 自动检查并创建表

---

## 五、安全设计

### 认证与授权
- 基于 JWT 的无状态认证
- JwtAuthFilter 拦截所有请求，从 Authorization Header 提取 token
- token 中包含 userId，用于数据隔离
- /api/auth/** 路径放行，其余路径要求认证

### 数据隔离
- 文档查询：SQL 层面按 user_id 过滤
- 自动填表：数据源仅限当前用户的文档
- 统计信息：按用户独立统计

### 密码安全
- BCryptPasswordEncoder 单向哈希存储
- 注册时自动加密，登录时安全比对

### 错误处理
- GlobalExceptionHandler 统一捕获异常
- 前端 Axios 拦截器处理 401/429/503 等状态码
- AI 服务失败自动重试，避免单次网络波动影响用户体验

---

## 六、项目结构

```
Intelligent-Document-Processing-System/
|
|-- docai/docai/                        # 后端项目
|   |-- pom.xml                         # Maven 依赖配置
|   |-- src/main/java/com/team/docai/
|   |   |-- DocaiApplication.java       # Spring Boot 启动类
|   |   |-- common/Result.java          # 统一响应体
|   |   |-- config/                     # 配置类
|   |   |   |-- SecurityConfig.java     # 安全配置（JWT + CORS）
|   |   |   |-- JwtAuthFilter.java      # JWT 过滤器
|   |   |   |-- MybatisPlusConfig.java  # 分页插件配置
|   |   |   |-- GlobalExceptionHandler  # 全局异常处理
|   |   |   |-- DatabaseInitializer     # 数据库自动初始化
|   |   |   |-- PasswordEncoderConfig   # BCrypt 配置
|   |   |   +-- MyMetaObjectHandler     # 自动填充创建/更新时间
|   |   |-- controller/                 # REST API 接口
|   |   |   |-- AuthController          # 认证（注册/登录）
|   |   |   |-- AIController            # AI 功能接口
|   |   |   |-- DocumentController      # 文档管理接口
|   |   |   +-- AutoFillController      # 自动填表接口
|   |   |-- entity/                     # 数据实体
|   |   |   |-- Document.java           # 文档实体
|   |   |   +-- User.java               # 用户实体
|   |   |-- mapper/                     # MyBatis 数据访问层
|   |   +-- service/                    # 业务逻辑层
|   |       |-- AIService.java          # AI 服务（核心）
|   |       |-- AutoFillService.java    # 自动填表引擎
|   |       |-- DocParseService.java    # 文档解析引擎
|   |       +-- UserService.java        # 用户服务
|   +-- src/main/resources/
|       |-- application.yml             # 主配置
|       +-- init.sql                    # 建表脚本
|
|-- docai-frontend/                     # 前端项目
|   |-- src/
|   |   |-- api/                        # API 请求层
|   |   |   |-- index.js                # 接口定义（30+ API）
|   |   |   +-- request.js              # Axios 封装
|   |   |-- layout/MainLayout.vue       # 主布局
|   |   |-- router/index.js             # 路由（含认证守卫）
|   |   |-- views/                      # 页面组件
|   |   |   |-- Login.vue               # 登录/注册
|   |   |   |-- Dashboard.vue           # 工作台
|   |   |   |-- DocumentList.vue        # 文档管理
|   |   |   |-- AutoFill.vue            # 自动填表
|   |   |   |-- AIChat.vue              # AI 对话
|   |   |   +-- AIGenerate.vue          # 智能写作
|   |   +-- style.css                   # 设计系统变量
|   |-- package.json
|   +-- vite.config.js                  # Vite 配置（代理）
|
|-- docs/                               # 文档目录
|   |-- PROJECT-DETAIL.md               # 本文件
|   |-- PPT-OUTLINE.md                  # 答辩 PPT 大纲
|   |-- DEMO-GUIDE.md                   # 演示指南
|   |-- PROJECT-PLAN.md                 # 项目计划
|   +-- GIT-COOP.md                     # Git 协作规范
|
+-- README.md                           # 项目概述
```

---

## 七、启动与部署

### 开发环境要求
- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+

### 启动步骤

1. **数据库**：创建 docai 数据库，应用启动时会自动执行 init.sql 建表
2. **后端**：进入 docai/docai 目录，执行 `mvn spring-boot:run`，启动在 8080 端口
3. **前端**：进入 docai-frontend 目录，执行 `npm install` 和 `npm run dev`，启动在 5173 端口
4. **访问**：浏览器打开 http://localhost:5173，注册账号后即可使用

### 生产部署

**方案一：单 JAR 部署**
```
cd docai-frontend && npm run build
cp -r dist/* ../docai/docai/src/main/resources/static/
cd ../docai/docai && mvn package -DskipTests
java -jar target/docai-0.0.1-SNAPSHOT.jar
```

**方案二：Nginx 反向代理**
- 前端静态文件部署到 Nginx
- /api/ 路径反向代理到后端 8080 端口

---

## 八、API 接口一览

### 认证接口
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/register | 用户注册 |
| POST | /api/auth/login | 用户登录（返回 JWT） |
| GET | /api/auth/me | 获取当前用户信息 |

### 文档管理接口
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/documents/upload | 单文件上传（自动 AI 提取） |
| POST | /api/documents/upload/batch | 批量上传 |
| GET | /api/documents | 分页查询文档列表 |
| GET | /api/documents/{id} | 文档详情 |
| GET | /api/documents/{id}/summary | AI 文档摘要 |
| GET | /api/documents/{id}/download | 下载原始文档 |
| GET | /api/documents/stats | 统计信息 |
| DELETE | /api/documents/{id} | 删除文档 |
| DELETE | /api/documents/batch | 批量删除 |

### AI 功能接口
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/ai/chat | AI 对话 |
| POST | /api/ai/generate | 公文生成 |
| POST | /api/ai/generate-word | 生成 Word 文档 |
| POST | /api/ai/polish | 文本润色 |
| POST | /api/ai/analyze-data | 数据分析 |
| POST | /api/ai/extract-info | 关键信息提取 |

### 自动填表接口
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/autofill/auto | 自动填充 |
| POST | /api/autofill/auto-batch | 批量自动填充（ZIP） |
| POST | /api/autofill/single | 单模板填充 |
| POST | /api/autofill/batch | 批量模板填充 |
| POST | /api/autofill/preview | 预览填充结果 |

---

## 九、创新点总结

1. **AI 驱动的智能填表引擎**：突破传统基于规则的表格填充方式，利用大语言模型理解文档语义，实现从非结构化文档到结构化模板的智能映射。支持 JSON 解析 + 正则表达式双重提取策略，兼顾准确性和容错性。

2. **上传即提取的信息抽取机制**：文档上传时即刻通过 AI 提取结构化关键信息，而非简单存储原始文本。经过优化的 Prompt 设计确保数值精确、实体完整，为下游自动填表提供高质量数据源。

3. **多格式统一解析框架**：设计统一解析抽象层，四种格式通过同一接口输出标准化文本。Apache POI 处理 Office 文件，自定义解析器处理纯文本和 Markdown，消除格式差异对 AI 分析的影响。

4. **企业级容错与稳定性机制**：AI 服务采用指数退避重试（最多 3 次）、并发信号量控制（Semaphore(3)）、120 秒超时保护、长文档自动截断。前端配合 429/503 状态码优雅降级处理。

5. **用户数据安全隔离**：基于 JWT 认证体系，所有数据操作均按用户 ID 隔离，确保多用户环境下的数据安全和隐私保护。
