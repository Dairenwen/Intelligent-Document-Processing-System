# DocAI - 智能文档处理系统

> 🏆 中国大学生服务外包创新创业大赛参赛项目

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5-green?style=flat-square&logo=spring-boot" />
  <img src="https://img.shields.io/badge/Vue-3-brightgreen?style=flat-square&logo=vue.js" />
  <img src="https://img.shields.io/badge/AI-GLM--4--Flash-blue?style=flat-square" />
</p>

---

## 📌 项目概述

**DocAI（智能文档处理系统）** 是一款面向企业级文档处理场景的智能化平台，融合大语言模型（LLM）与文档解析技术，实现对多种格式文档的智能编辑、信息提取与自动填表。

### 🎯 核心价值
- **降低人工成本**：自动从非结构化文档中提取关键信息，减少 80% 人工录入工作
- **提升填表效率**：上传源文档后一键自动填写模板表格，单文档 ≤90s
- **智能写作辅助**：AI 驱动的公文生成与润色，支持多种公文体裁

---

## 🏗️ 系统架构

```
┌────────────────────────────────────────────────────┐
│                   前端 (Vue 3 + Vite)               │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐ │
│  │ 文档管理  │ │ AI对话   │ │ 智能写作  │ │自动填表│ │
│  └──────────┘ └──────────┘ └──────────┘ └────────┘ │
└───────────────────┬────────────────────────────────┘
                    │ HTTP / REST API
┌───────────────────┴────────────────────────────────┐
│              后端 (Spring Boot 3.5)                 │
│  ┌──────────────────────────────────────────────┐  │
│  │ Controller Layer (REST API)                   │  │
│  ├──────────────────────────────────────────────┤  │
│  │ Service Layer                                 │  │
│  │  ├── AIService (智谱GLM-4-Flash + 重试机制)   │  │
│  │  ├── DocParseService (多格式解析+模板填充)    │  │
│  │  └── AutoFillService (自动填表编排引擎)       │  │
│  ├──────────────────────────────────────────────┤  │
│  │ Data Layer (MyBatis-Plus + MySQL)             │  │
│  └──────────────────────────────────────────────┘  │
└───────────────────┬────────────────────────────────┘
                    │
         ┌──────────┴──────────┐
         │  智谱AI GLM-4-Flash │
         │  (大语言模型)        │
         └─────────────────────┘
```

---

## 🔥 三大核心模块

### 模块一：文档智能编辑操作交互

- **AI 对话**：基于文档上下文的智能问答，可关联已上传文档进行深度分析
- **智能写作**：支持通知、报告、请示、函等 8 种公文类型一键生成
- **文本润色**：AI 优化文档质量，提升文字表达水平
- **Word 导出**：生成内容一键下载为 .docx 文件

### 模块二：非结构化文档信息提取

- **多格式支持**：Word (.docx)、Excel (.xlsx)、纯文本 (.txt)、Markdown (.md)
- **智能解析**：自动识别文档结构，提取段落、表格、标题等元素
- **关键信息提取**：通过 AI 大模型自动识别并提取文档关键数据
- **批量上传**：支持同时上传多个文档，自动解析入库

### 模块三：自定义表格数据自动填写 ⭐

> **比赛核心功能 — 评分关键**

- **智能填表流程**：
  1. 上传多个源文档（docx/xlsx/txt/md）
  2. 上传模板文件（Word 或 Excel 模板）
  3. AI 自动提取源文档关键信息
  4. 智能匹配并填充模板占位符/空单元格
  5. 下载填充完成的文件
- **模板识别**：支持 `{{占位符}}` 模式和空表格单元格自动识别
- **批量填充**：一次性上传多个模板文件，返回 ZIP 压缩包
- **性能指标**：填充率 ≥80%，单文档处理时间 ≤90s

---

## 🛠️ 技术栈

| 层级 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **前端** | Vue 3 | 3.x | 组合式 API (Composition API) |
| | Vite | 7.x | 构建工具 |
| | Element Plus | 最新 | UI 组件库 |
| | Axios | 最新 | HTTP 客户端（含重试） |
| **后端** | Spring Boot | 3.5.11 | 核心框架 |
| | MyBatis-Plus | 最新 | ORM 框架 |
| | Apache POI | 5.5.1 | Office 文档读写 |
| | Spring Security | 最新 | 安全框架 (CORS) |
| **AI** | 智谱 GLM-4-Flash | - | 免费大语言模型 |
| | oapi-java-sdk | 最新 | 智谱AI官方SDK |
| **数据库** | MySQL | 8.0 | 关系型数据库 |

---

## 🚀 快速启动

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+

### 1. 数据库初始化

```sql
-- 执行 docai/init.sql
CREATE DATABASE IF NOT EXISTS docai DEFAULT CHARSET utf8mb4;
USE docai;

CREATE TABLE IF NOT EXISTS documents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    file_type VARCHAR(20),
    file_path VARCHAR(500),
    file_size BIGINT DEFAULT 0,
    content_text LONGTEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_file_type (file_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2. 启动后端

```bash
cd docai
# 修改 src/main/resources/application.yml 中的数据库连接信息
mvn spring-boot:run
# 后端启动在 http://localhost:8080
```

### 3. 启动前端

```bash
cd docai-frontend
npm install
npm run dev
# 前端启动在 http://localhost:5173
# Vite 自动代理 /api -> http://localhost:8080
```

### 4. 访问系统

浏览器打开 `http://localhost:5173`

---

## 📡 API 接口文档

### 文档管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/documents/upload` | 单文件上传 |
| POST | `/api/documents/upload/batch` | 批量上传 |
| GET | `/api/documents` | 分页查询文档列表 |
| GET | `/api/documents/{id}` | 文档详情 |
| GET | `/api/documents/{id}/summary` | AI文档摘要 |
| GET | `/api/documents/stats` | 统计信息 |
| DELETE | `/api/documents/{id}` | 删除文档 |
| DELETE | `/api/documents/batch` | 批量删除 |

### AI 功能

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/chat` | AI 对话 |
| POST | `/api/ai/generate` | 公文生成 |
| POST | `/api/ai/polish` | 文本润色 |
| POST | `/api/ai/analyze-data` | 数据分析 |
| POST | `/api/ai/extract-info` | 关键信息提取 |

### 自动填表

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/autofill/single` | 单模板填充（返回填充文件） |
| POST | `/api/autofill/batch` | 批量模板填充（返回ZIP） |
| POST | `/api/autofill/preview` | 预览填充结果 |

---

## 📂 项目结构

```
Intelligent-Document-Processing-System/
├── docai/                          # 后端 Spring Boot 项目
│   ├── src/main/java/com/team/docai/
│   │   ├── DocaiApplication.java       # 启动类
│   │   ├── common/Result.java          # 统一响应
│   │   ├── config/
│   │   │   ├── MybatisPlusConfig.java  # MyBatis-Plus 配置
│   │   │   ├── SecurityConfig.java     # 安全 & CORS 配置
│   │   │   └── GlobalExceptionHandler.java  # 全局异常处理
│   │   ├── controller/
│   │   │   ├── AIController.java       # AI 功能接口
│   │   │   ├── AutoFillController.java # 自动填表接口
│   │   │   └── DocumentController.java # 文档管理接口
│   │   ├── entity/Document.java        # 文档实体
│   │   ├── mapper/DocumentMapper.java  # 数据访问层
│   │   └── service/
│   │       ├── AIService.java          # AI 服务 (智谱GLM)
│   │       ├── AutoFillService.java    # 自动填表引擎
│   │       └── DocParseService.java    # 文档解析引擎
│   ├── src/main/resources/
│   │   ├── application.yml             # 配置文件
│   │   └── application.properties
│   ├── init.sql                        # 数据库初始化脚本
│   └── pom.xml
│
├── docai-frontend/                 # 前端 Vue 3 项目
│   ├── src/
│   │   ├── api/
│   │   │   ├── index.js             # API 接口定义
│   │   │   └── request.js           # Axios 封装
│   │   ├── layout/MainLayout.vue    # 主布局
│   │   ├── router/index.js          # 路由配置
│   │   ├── views/
│   │   │   ├── Dashboard.vue        # 首页概览
│   │   │   ├── DocumentList.vue     # 文档管理
│   │   │   ├── AutoFill.vue         # 自动填表 ⭐
│   │   │   ├── AIChat.vue           # AI 对话
│   │   │   └── AIGenerate.vue       # 智能写作
│   │   ├── App.vue
│   │   ├── main.js
│   │   └── style.css                # 企业级设计系统
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
│
└── README.md
```

---

## 🏆 竞赛创新点

### 创新点一：AI 驱动的智能填表引擎
基于大语言模型（GLM-4-Flash）构建端到端自动填表流水线：源文档解析 → AI 结构化提取 → 模板智能匹配 → 自动填充。突破传统基于规则的填表方式，能够理解自然语言文档内容并精准映射到模板字段。

### 创新点二：多格式统一解析框架
设计了统一的文档解析抽象层，支持 Word、Excel、TXT、Markdown 四种格式的无差异化解析。通过 Apache POI 处理 Office 文档，自定义解析器处理纯文本和 Markdown，最终统一输出为结构化文本，为下游 AI 分析提供标准化输入。

### 创新点三：容错与稳定性保障机制
- AI 服务采用指数退避重试策略（最多 3 次），应对网络波动和 API 限流
- 全局异常处理 + 前端超时重试，确保系统在压力测试下保持稳定
- 长文档自动分段摘要，避免超出模型 token 限制

---

## 📋 评测指南

### 测试文件要求
| 文件类型 | 数量 | 最小大小 |
|----------|------|----------|
| .docx | ≥ 5 个 | ≥ 500KB |
| .md | ≥ 3 个 | ≥ 15KB |
| .xlsx | ≥ 5 个 | ≥ 500KB |
| .txt | ≥ 3 个 | ≥ 15KB |

### 模板文件
- 准备 5 个 Word/Excel 模板文件
- 模板中使用 `{{字段名}}` 标记待填充位置
- 或使用空单元格的 Excel 表格

### 评测流程
1. 上传所有测试文档到「文档管理」
2. 进入「自动填表」→ 选择已上传文档 → 上传模板
3. 系统自动填充并返回结果文件
4. 下载后验证填充率（≥80%）和准确性

---

## 📊 部署建议

### 开发/演示环境（推荐）
前后端联合开发：
- 后端：`mvn spring-boot:run`（端口 8080）
- 前端：`npm run dev`（端口 5173，自动代理）

### 生产环境
**方案 A：单 JAR 部署**
```bash
# 构建前端
cd docai-frontend && npm run build
# 将 dist/ 内容复制到 Spring Boot 的 static/ 目录
cp -r dist/* ../docai/src/main/resources/static/
# 构建 JAR
cd ../docai && mvn package -DskipTests
# 运行
java -jar target/docai-0.0.1-SNAPSHOT.jar
```

**方案 B：Nginx 反向代理**
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


