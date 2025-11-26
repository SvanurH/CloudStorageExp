# CloudStorageExplorer



**CloudStorageExplorer** 是一款基于 Burp Suite Montoya API 开发的插件，旨在帮助安全研究人员直接在 Burp Suite 中对云存储服务（OSS/COS/BOS）进行探测和利用。

它解决了在渗透测试过程中，获取到云存储 AccessKey/SecretKey (AK/SK) 后，需要切换到外部工具（如 ossutil、命令行或 Python 脚本）进行验证的繁琐问题。通过此插件，你可以直接生成签名请求，查看响应，并将流量无缝集成到 Burp 的工作流中。

## ✨ 功能特性 (Features)

  * **多厂商支持**：
      * ✅ 阿里云 OSS (Alibaba Cloud Object Storage Service) - 深度支持
      * 🚧 腾讯云 COS (Tencent Cloud) - *开发中*
      * 🚧 百度云 BOS (Baidu Cloud) - *开发中*
  * **自动签名计算**：无需手动计算复杂的 HMAC-SHA1 签名，插件自动根据 AK/SK 处理 `Authorization` 头。
  * **STS 令牌支持**：完美支持临时访问凭证 (Security Token Service)，适用于移动端或小程序泄露临时密钥的场景。
  * **智能端点生成**：根据输入的 Region 和 Bucket 名称，自动推断并生成正确的 Endpoint URL。
  * **原生 UI 集成**：
      * **Request/Response 预览**：集成 Burp 原生编辑器，支持语法高亮和搜索。
      * **历史记录面板**：记录所有发出的请求，支持按 ID、时间、厂商、状态码进行排序。
  * **Burp 工作流**：所有请求均通过 Burp 网络层发送，这意味着你可以在 Logger 中看到它们，或将其发送到 Repeater 进行进一步测试。

## 🛠️ 安装 (Installation)

1.  **下载/构建**：
      * 从 [Releases](https://www.google.com/search?q=https://github.com/yourusername/CloudStorageExplorer/releases) 页面下载最新的 `CloudStorageExplorer.jar`。
      * 或者手动构建：
        ```bash
        mvn clean package
        ```
2.  **加载插件**：
      * 打开 **Burp Suite** -\> **Extensions** -\> **Installed**。
      * 点击 **Add**。
      * 选择 **Extension type**: `Java`。
      * 选择下载或构建好的 `.jar` 文件。
3.  **验证**：
      * 加载成功后，你应该能看到名为 `CloudStorageExplorer` 的新标签页。
      * 控制台日志将输出作者信息 Banner。

## 📖 使用指南 (Usage)

### 1\. 配置凭证 (Credentials)

在左侧配置面板中填入获取到的云服务凭证：

  * **AccessKey ID**: 必填。
  * **AccessKey Secret**: 必填。
  * **Session Token**: 选填。如果是 STS 临时凭证，请务必填入此项。

### 2\. 设置目标 (Target)

  * **Region**: 输入区域代码（例如 `oss-cn-hangzhou`）。插件会自动处理 `oss-` 前缀。
  * **Bucket Name**: 输入存储桶名称。
  * **Host**: 输入 Region 后，Host 会自动生成。你也可以手动修改它以支持自定义域名。

### 3\. 选择操作 (Operation)

  * **Storage Vendor**: 选择对应的云厂商（目前 OSS 支持最完善）。
  * **Operation**: 选择要执行的操作，例如 `List Objects`。

### 4\. 执行与查看 (Execute & Inspect)

  * 点击 **Execute Request** 按钮。
  * 请求和响应将显示在右侧的选项卡中。
  * 底部的历史记录表会新增一条记录，点击表格行可以回溯历史请求。

## 📝 开发与构建 (Build from Source)

本项目依赖 Maven 进行依赖管理。

**依赖项 (pom.xml):**

```xml
<dependencies>
    <dependency>
        <groupId>net.portswigger.burp.extensions</groupId>
        <artifactId>montoya-api</artifactId>
        <version>2023.10.4</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

**编译命令:**

```bash
git clone https://github.com/yourusername/CloudStorageExplorer.git
cd CloudStorageExplorer
mvn clean package
```

## 🗺️ 路线图 (Roadmap)

  * [x] 阿里云 OSS 基础操作 (List Objects)
  * [x] STS Token 完整支持
  * [x] 历史记录排序功能
  * [ ] 腾讯云 COS 签名算法实现
  * [ ] 百度云 BOS 签名算法实现
  * [ ] 支持文件上传 (Put Object)
  * [ ] 支持列出 Bucket (List Buckets/GetService)

## ⚠️ 免责声明 (Disclaimer)

本工具仅用于**授权的渗透测试**和**安全研究**。严禁将本工具用于未授权的攻击行为。使用者需自行承担因使用本工具而产生的所有法律及连带责任。
