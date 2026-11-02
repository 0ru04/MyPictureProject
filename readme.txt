# ImageCloud 云图管理平台

ImageCloud 是一个前后端分离的图片管理平台，提供公共图库、用户私有空间、图片审核、多维图片检索和 AI 扩图等能力。项目由 Vue 3 前端与 Spring Boot 后端组成，本目录为后端工程。

## 已实现功能

### 用户与权限

- 用户注册、登录、退出登录和当前登录用户查询。
- 普通用户与管理员角色区分。
- 基于 `@AuthCheck` 和拦截器的接口权限控制。
- 图片与私有空间归属校验，私有图片仅创建者本人可访问和操作。
- Long 类型 ID 统一序列化为字符串，避免前端 JavaScript 长整数精度丢失。

### 公共图库

- 支持文件上传和图片 URL 上传。
- 支持 JPG、JPEG、PNG、GIF、BMP、WebP 格式，单文件最大 10 MB。
- 上传时生成压缩图片和缩略图，并提取图片尺寸、比例、格式、大小和主色调。
- 公共图库分页展示，支持按名称、简介、标签、分类、格式、审核状态和时间范围查询。
- 支持按创建时间等字段排序。
- 图片详情查看、编辑、删除和链接分享。
- 公共图片支持待审核、审核通过和审核拒绝三种状态，仅审核通过的图片在公共图库展示。

### 私有空间

- 用户可创建、编辑和删除自己的私有空间。
- 私有空间支持普通版、专业版和旗舰版三种级别及对应的图片数量、存储容量限制。
- 支持上传文件到私有空间或将 URL 图片保存到私有空间。
- 私有空间图片支持分页、名称/简介搜索、标签筛选和颜色筛选。
- 私有空间图片不进入公共审核流程，直接在所属空间内展示。
- 私有空间图片仅空间创建者本人可查看、编辑、删除、替换和执行 AI 处理，管理员也不能越权访问。
- 空间详情展示已用图片数、总容量和使用进度。

### 图片检索

- 支持按图片名称、简介、分类、标签等条件组合检索。
- 支持以图搜图。
- 支持按主色调相似度搜索图片，返回相似度较高的图片。
- 公共图库和私有空间分别使用独立的空间范围与权限规则。

### AI 扩图

- 基于阿里云百炼 DashScope `wanx2.1-imageedit` 扩图能力实现 AI 扩图。
- 支持自定义扩图提示词。
- 支持分别设置上、下、左、右四个方向的扩展比例。
- 后端封装异步任务创建、任务状态查询和结果图片 URL 解析。
- 前端支持任务轮询、结果预览和一键应用扩图结果。
- 应用扩图结果时覆盖原图片，并保留原图片所属空间与归属关系。
- 针对 DashScope 输入限制实现图片等比缩放、WebP 兼容转换和 Base64 预处理。
- 对余额不足、参数错误等上游异常提供可读错误提示。

### 管理后台

- 用户分页查询、新增、编辑和删除。
- 公共图片分页查询、审核、编辑和删除。
- 批量创建公共图片，单次最多抓取 30 张。
- 空间分页查询、创建、编辑和删除。
- 管理员图片管理范围限制为公共图库，不展示或审核用户私有图片。

### 缓存与稳定性

- 图片列表支持 Caffeine 本地缓存与 Redis 分布式缓存。
- 缓存使用随机过期时间，降低缓存集中失效风险。
- 图片上传接口使用 Redisson 限流，默认单用户每秒最多 5 次。
- 全局异常处理与统一响应结构。
- 提供 `/api/health` 健康检查接口。

## 技术栈

### 后端

| 技术 | 用途 |
| --- | --- |
| Java 8 | 后端开发语言 |
| Spring Boot 2.7.6 | Web 应用与依赖管理 |
| MyBatis-Plus 3.5.9 | ORM、分页与逻辑删除 |
| MySQL | 用户、图片和空间数据存储 |
| Redis + Redisson | 分布式缓存、限流与上传控制 |
| Caffeine | 进程内本地缓存 |
| 腾讯云 COS | 图片对象存储、压缩与缩略图处理 |
| 阿里云百炼 DashScope | AI 扩图 |
| Jsoup | 批量图片抓取 |
| Maven | 构建与依赖管理 |

### 前端

| 技术 | 用途 |
| --- | --- |
| Vue 3 + TypeScript | 页面开发 |
| Vite | 开发服务器与构建 |
| Ant Design Vue | UI 组件 |
| Vue Router | 路由管理 |
| Pinia | 登录用户状态管理 |
| Axios | HTTP 请求 |
| Vue Cropper | 图片裁剪 |

## 项目结构

当前开发环境下，前后端工程分别位于：

- 后端：`D:\code\ImageCloud\ImageCloud-server`
- 前端：`D:\code\imagehub\imagehub-web`

后端主要目录：

```text
ImageCloud-server
├─ sql/                        数据库初始化脚本
├─ src/main/java/.../
│  ├─ annotation/              自定义权限注解
│  ├─ aop/                     权限校验切面
│  ├─ api/                     阿里云 AI外部接口封装
│  ├─ common/                  通用请求与响应对象
│  ├─ config/                  MyBatis-Plus、JSON、CORS、COS 等配置
│  ├─ controller/              用户、图片、空间接口
│  ├─ exception/               业务异常和全局异常处理
│  ├─ manager/                 COS、文件与图片上传管理
│  ├─ mapper/                  MyBatis-Plus Mapper
│  ├─ model/                   DTO、VO、实体类和枚举
│  ├─ service/                 业务接口与实现
│  └─ utils/                   工具类
├─ src/main/resources/
│  ├─ application.yml          应用配置
│  └─ generator/mapper/        Mapper XML
├─ src/test/                   单元测试与接口绑定测试
├─ pom.xml
└─ readme.txt
```

前端主要页面：

- 首页公共图库
- 用户登录、注册
- 创建图片、批量创建图片
- 图片详情
- 我的空间、空间详情、创建空间
- 用户管理、图片管理、空间管理

## 环境要求

- JDK 8 或更高版本
- Maven 3.8+
- Node.js 20+
- npm 10+
- MySQL 8+
- Redis 6+
- 腾讯云 COS 存储桶
- 阿里云百炼 API Key

## 快速开始

### 1. 初始化数据库

执行 [sql/create_table.sql](sql/create_table.sql) 创建 `imagecloud` 数据库及数据表。

可在 MySQL 客户端中执行：

```sql
source sql/create_table.sql;
```

### 2. 配置后端

修改 `src/main/resources/application.yml`，至少确认以下配置：

```yaml
server:
  port: 8123
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/imagecloud
    username: <数据库用户名>
    password: <数据库密码>
  redis:
    host: <Redis 地址>
    port: 6379
    password: <Redis 密码>

cos:
  client:
    host: <COS 访问域名>
    secret-id: <腾讯云 SecretId>
    secret-key: <腾讯云 SecretKey>
    region: <COS 地域>
    bucket: <COS 存储桶>

aliYunAi:
  apiKey: <阿里云百炼 API Key>
```

请勿将真实密钥提交到版本库，建议使用本地配置文件、环境变量或密钥管理服务。

### 3. 启动后端

在 IDEA 中运行 `ImageCloudServerApplication`，或使用 Maven：

```bash
mvn spring-boot:run -Dspring-boot.run.skip=false
```

启动后可访问：

- 后端服务：`http://localhost:8123/api`
- 健康检查：`http://localhost:8123/api/health`

### 4. 启动前端

```bash
cd D:\code\imagehub\imagehub-web
npm install
npm run dev
```

Vite 默认使用 `http://localhost:5173`。前端 Axios 默认请求地址为 `http://localhost:8123`，接口路径中已包含 `/api` 前缀。

## 关键数据约定

- `picture.space_id IS NULL`：公共图库图片。
- `picture.space_id IS NOT NULL`：私有空间图片。
- 公共图片审核状态：`0` 待审核、`1` 审核通过、`2` 审核拒绝。
- 私有图片无需公共审核。
- 所有 Long 类型 ID 返回前端时为字符串，避免长整数精度丢失。

## 接口约定

接口统一返回：

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

- `code = 0` 表示成功。
- `code != 0` 表示业务异常或参数异常。
- 接口前缀统一为 `/api`。
- 主要接口模块：`/api/user`、`/api/picture`、`/api/space`。

## 测试

运行全部单元测试：

```bash
mvn test
```

项目测试包括图片查询范围、私有空间权限、管理员访问边界、图片上传参数绑定、AI 扩图请求契约和异常码映射等。
