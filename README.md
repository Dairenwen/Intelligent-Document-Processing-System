# DocAI - 智能文档处理系统

> 中国大学生服务外包创新创业大赛参赛项目

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5-green?style=flat-square&logo=spring-boot" />
  <img src="https://img.shields.io/badge/Vue-3-brightgreen?style=flat-square&logo=vue.js" />
  <img src="https://img.shields.io/badge/AI-GLM--4--Flash-blue?style=flat-square" />
</p>

---

## 项目概述

**DocAI（智能文档处理系统）** 是一款面向企业级文档处理场景的智能化平台，融合大语言模型（LLM）与文档解析技术，实现对多种格式文档的智能编辑、信息提取与自动填表。

### 核心价值
- **降低人工成本**：自动从非结构化文档中提取关键信息，减少 80% 人工录入工作
- **提升填表效率**：上传源文档后一键自动填写模板表格，单文档处理时间不超过90秒
- **智能写作辅助**：AI 驱动的公文生成与润色，支持多种公文体裁
- **用户数据隔离**：基于JWT认证的多用户系统，每个用户仅能访问自己的文档

---

## 系统架构

```
+----------------------------------------------------+
|                   前端 (Vue 3 + Vite)               |
|  +----------+ +----------+ +----------+ +--------+ |
|  | 文档管理  | | AI对话   | | 智能写作  | |自动填表| |
|  +----------+ +----------+ +----------+ +--------+ |
+-------------------+--------------------------------+
                    | HTTP / REST API
+-------------------+--------------------------------+
|              后端 (Spring Boot 3.5)                 |
|  +----------------------------------------------+  |
|  | Controller Layer (REST API)                   |  |
|  |----------------------------------------------+  |
|  | Service Layer                                 |  |
|  |  |-- AIService (智谱GLM-4-Flash + 并发控制)   |  |
|  |  |-- DocParseService (多格式解析+模板填充)    |  |
|  |  +-- AutoFillService (自动填表编排引擎)       |  |
|  |----------------------------------------------+  |
|  | Security Layer (JWT + 用户文档隔离)            |  |
|  |----------------------------------------------+  |
|  | Data Layer (MyBatis-Plus + MySQL)             |  |
|  +----------------------------------------------+  |
+-------------------+--------------------------------+
                    |
         +----------+----------+
         |  智谱AI GLM-4-Flash |
         |  (大语言模型)        |
         +---------------------+
```

---

## 三大核心模块

### 模块一：文档智能编辑操作交互

- **AI 对话**：基于文档上下文的智能问答，可关联已上传文档进行深度分析
- **对话历史持久化**：每个用户的聊天记录按文档独立保存，刷新页面自动恢复
- **智能写作**：支持通知、报告、请示、函等 8 种公文类型一键生成
- **文本润色**：AI 优化文档质量，提升文字表达水平
- **Word 导出**：生成内容一键下载为 .docx 文件

### 模块二：非结构化文档信息提取

- **多格式支持**：Word (.docx)、Excel (.xlsx)、纯文本 (.txt)、Markdown (.md)
- **智能解析**：自动识别文档结构，提取段落、表格、标题等元素
- **AI 信息提取**：上传文档时自动通过AI大模型提取结构化关键信息（而非存储原始文本）
- **优化提取 Prompt**：经过精心设计的提取提示词，确保数值数据精确、实体信息完整
- **用户隔离**：每个用户仅能查看和操作自己上传的文档
- **批量上传**：支持同时上传多个文档，自动解析入库

### 模块三：自定义表格数据自动填写（核心功能）

> 比赛核心功能 -- 评分关键

- **智能填表流程**：
  1. 上传多个源文档（docx/xlsx/txt/md）
  2. 上传模板文件（Word 或 Excel 模板）
  3. AI 自动从用户文档中提取关键信息
  4. 智能匹配并填充模板占位符/空单元格
  5. 预览并下载填充完成的文件
- **模板识别**：支持 `{{占位符}}` 模式和空表格单元格自动识别
- **批量填充**：一次性上传多个模板文件，返回 ZIP 压缩包
- **用户数据隔离**：填表数据源仅限当前用户的文档
- **性能指标**：填充率大于等于80%，单文档处理时间不超过90秒

---

## 技术栈

| 层级 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **前端** | Vue 3 | 3.5.x | 组合式 API (Composition API) |
| | Vite | 7.x | 构建工具 |
| | Element Plus | 2.13.x | UI 组件库 |
| | Pinia | 3.x | 状态管理 |
| | Axios | 1.13.x | HTTP 客户端（含重试和限流处理） |
| **后端** | Spring Boot | 3.5.11 | 核心框架 |
| | MyBatis-Plus | 最新 | ORM 框架 |
| | Spring Security | 最新 | JWT 认证 + CORS |
| | Apache POI | 5.5.1 | Office 文档读写 |
| **AI** | 智谱 GLM-4-Flash | - | 免费大语言模型 |
| | oapi-java-sdk | 最新 | 智谱AI官方SDK |
| **数据库** | MySQL | 8.0 | 关系型数据库 |

---

## 快速启动

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+

### 1. 数据库初始化

```sql
-- 执行 docai/docai/src/main/resources/init.sql
CREATE DATABASE IF NOT EXISTS docai DEFAULT CHARSET utf8mb4;
USE docai;
-- 表结构会通过 init.sql 自动创建
```

### 2. 启动后端

```bash
cd docai/docai
# 修改 src/main/resources/application.yml 中的数据库连接信息（可选）
mvn spring-boot:run
# 后端启动在 http://localhost:8080
```

快速验证：浏览器访问 `http://localhost:8080/api/documents`，返回 200 状态码表示正常。

### 3. 启动前端

```bash
cd docai-frontend
npm install
npm run dev
# 前端启动在 http://localhost:5173
# Vite 自动代理 /api -> http://localhost:8080
```

### 4. 访问系统

浏览器打开 `http://localhost:5173`，注册账号后即可使用。

---

## API 接口文档

### 用户认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 用户注册 |
| POST | `/api/auth/login` | 用户登录（返回JWT） |
| GET | `/api/auth/me` | 获取当前用户信息 |

### 文档管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/documents/upload` | 单文件上传（自动AI提取） |
| POST | `/api/documents/upload/batch` | 批量上传 |
| GET | `/api/documents` | 分页查询文档列表（按用户隔离） |
| GET | `/api/documents/{id}` | 文档详情 |
| GET | `/api/documents/{id}/summary` | AI文档摘要 |
| GET | `/api/documents/{id}/download` | 下载原始文档 |
| GET | `/api/documents/stats` | 统计信息（按用户隔离） |
| DELETE | `/api/documents/{id}` | 删除文档 |
| DELETE | `/api/documents/batch` | 批量删除 |

### AI 功能

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/chat` | AI 对话（支持关联文档） |
| POST | `/api/ai/generate` | 公文生成 |
| POST | `/api/ai/generate-word` | 生成Word文档下载 |
| POST | `/api/ai/polish` | 文本润色 |
| POST | `/api/ai/analyze-data` | 数据分析 |
| POST | `/api/ai/extract-info` | 关键信息提取 |

### 自动填表

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/autofill/auto` | 自动填充（数据源：用户所有文档） |
| POST | `/api/autofill/auto-batch` | 批量自动填充（返回ZIP） |
| POST | `/api/autofill/single` | 单模板填充（可指定源文档） |
| POST | `/api/autofill/batch` | 批量模板填充（可指定源文档） |
| POST | `/api/autofill/preview` | 预览填充结果 |

---

## 项目结构

```
Intelligent-Document-Processing-System/
|-- docai/                          # 后端 Spring Boot 项目
|   +-- docai/
|       |-- src/main/java/com/team/docai/
|       |   |-- DocaiApplication.java       # 启动类
|       |   |-- common/Result.java          # 统一响应封装
|       |   |-- config/
|       |   |   |-- SecurityConfig.java     # JWT认证 + CORS 配置
|       |   |   |-- JwtAuthFilter.java      # JWT过滤器（提取userId）
|       |   |   |-- MybatisPlusConfig.java  # MyBatis-Plus 分页配置
|       |   |   |-- GlobalExceptionHandler.java  # 全局异常处理
|       |   |   +-- DatabaseInitializer.java     # 数据库自动初始化
|       |   |-- controller/
|       |   |   |-- AuthController.java     # 用户认证接口
|       |   |   |-- AIController.java       # AI 功能接口
|       |   |   |-- AutoFillController.java # 自动填表接口
|       |   |   +-- DocumentController.java # 文档管理接口（含用户隔离）
|       |   |-- entity/
|       |   |   |-- Document.java           # 文档实体
|       |   |   +-- User.java               # 用户实体
|       |   |-- mapper/
|       |   |   |-- DocumentMapper.java     # 文档数据访问
|       |   |   +-- UserMapper.java         # 用户数据访问
|       |   +-- service/
|       |       |-- AIService.java          # AI 服务（并发控制+重试）
|       |       |-- AutoFillService.java    # 自动填表引擎
|       |       |-- DocParseService.java    # 文档解析引擎
|       |       +-- UserService.java        # 用户服务（JWT生成/验证）
|       |-- src/main/resources/
|       |   |-- application.yml             # 主配置文件
|       |   +-- init.sql                    # 数据库初始化脚本
|       +-- pom.xml
|
|-- docai-frontend/                 # 前端 Vue 3 项目
|   |-- src/
|   |   |-- api/
|   |   |   |-- index.js             # API 接口定义
|   |   |   +-- request.js           # Axios 封装（含限流错误处理）
|   |   |-- layout/MainLayout.vue    # 主布局（侧边栏+顶栏）
|   |   |-- router/index.js          # 路由配置（含鉴权守卫）
|   |   |-- views/
|   |   |   |-- Login.vue            # 登录/注册页
|   |   |   |-- Dashboard.vue        # 首页概览
|   |   |   |-- DocumentList.vue     # 文档管理
|   |   |   |-- AutoFill.vue         # 自动填表（核心功能）
|   |   |   |-- AIChat.vue           # AI 对话（含历史持久化）
|   |   |   +-- AIGenerate.vue       # 智能写作
|   |   |-- App.vue
|   |   |-- main.js
|   |   +-- style.css                # 企业级设计系统变量
|   |-- index.html
|   |-- package.json
|   +-- vite.config.js
|
+-- README.md
```

---

## 竞赛创新点

### 创新点一：AI 驱动的智能填表引擎
基于大语言模型（GLM-4-Flash）构建端到端自动填表流水线：源文档解析 -> AI 结构化提取 -> 模板智能匹配 -> 自动填充。突破传统基于规则的填表方式，能够理解自然语言文档内容并精准映射到模板字段。

### 创新点二：上传即提取的智能信息抽取
文档上传时即刻通过 AI 大模型从原始文本中提取结构化关键信息（数值数据、实体信息、关键事实等），而非简单存储原始文本。经过优化的 Prompt 设计确保提取数据的精确性和完整性，为后续自动填表提供高质量数据源。

### 创新点三：多格式统一解析框架
设计了统一的文档解析抽象层，支持 Word、Excel、TXT、Markdown 四种格式的无差异化解析。通过 Apache POI 处理 Office 文档，自定义解析器处理纯文本和 Markdown，最终统一输出为结构化文本，为下游 AI 分析提供标准化输入。

### 创新点四：容错与稳定性保障机制
- AI 服务采用指数退避重试策略（最多 3 次）+ 并发信号量控制（最多 3 个并发请求），应对网络波动和 API 限流
- 全局异常处理 + 前端超时重试 + 429/503 状态码优雅降级
- 长文档自动截断和分段摘要，避免超出模型 token 限制

### 创新点五：用户数据安全隔离
基于 JWT 认证体系，所有文档操作（上传、查询、统计、自动填表数据源）均按用户 ID 严格隔离，确保多用户环境下的数据安全。

---

## 评测指南

### 测试文件要求
| 文件类型 | 数量 | 最小大小 |
|----------|------|----------|
| .docx | 5 个以上 | 500KB 以上 |
| .md | 3 个以上 | 15KB 以上 |
| .xlsx | 5 个以上 | 500KB 以上 |
| .txt | 3 个以上 | 15KB 以上 |

### 模板文件
- 准备 5 个 Word/Excel 模板文件
- 模板中使用 `{{字段名}}` 标记待填充位置
- 或使用空单元格的 Excel 表格

### 评测流程
1. 注册账号并登录系统
2. 上传所有测试文档到「文档管理」，系统自动执行AI信息提取
3. 进入「智能填表」-> 上传模板文件
4. 系统自动从用户文档库中匹配数据并填充
5. 下载后验证填充率（大于等于80%）和准确性

---

## 部署建议

### 开发/演示环境
前后端联合开发：
- 后端：运行 `mvn spring-boot:run`（端口 8080）
- 前端：`npm run dev`（端口 5173，自动代理到后端）

### 生产环境

方案 A：单 JAR 部署
```bash
cd docai-frontend && npm run build
cp -r dist/* ../docai/docai/src/main/resources/static/
cd ../docai/docai && mvn package -DskipTests
java -jar target/docai-0.0.1-SNAPSHOT.jar
```

方案 B：Nginx 反向代理
```nginx
server {
    listen 80;
    location / {
        root /var/www/docai-frontend/dist;
        try_files $uri $uri/ /index.html;
    }
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
    }
}
```

---


