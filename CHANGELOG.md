# 更新日志

本文档记录了项目的所有重要更改。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [未发布]

### 计划中 - 第三阶段
- 服务注册与发现（Zookeeper）
- 负载均衡策略（随机、轮询、加权）
- 异步RPC调用（CompletableFuture）
- 优雅停机（ShutdownHook）
- 健康检查和心跳机制
- 监控和度量

### 计划中 - 第二阶段
- Netty网络通信
- 自定义通信协议
- 编解码器实现
- JDK动态代理
- 可插拔序列化（JSON、Protobuf）
- SPI扩展机制

## [1.0.0-SNAPSHOT] - 2025-12-23

### 新增 - 第一阶段（基础RPC框架）

#### 项目结构
- Maven多模块项目架构
- rpc-api、rpc-server、rpc-client三个子模块
- 完整的.gitignore配置

#### 核心功能
- **rpc-api模块**
  - `HelloService` 示例服务接口
  - `RpcRequest` 请求数据传输对象
  - `RpcResponse` 响应数据传输对象
  
- **rpc-server模块**
  - `RpcServer` 核心服务器实现
    - 基于ServerSocket的TCP监听
    - ExecutorService线程池处理并发请求
    - 服务注册表（Map存储）
    - 基于反射的方法调用
  - `HelloServiceImpl` 服务实现示例
  - `ServerBootstrap` 服务器启动类
  
- **rpc-client模块**
  - `RpcClient` 核心客户端实现
    - Socket连接管理
    - 请求发送和响应接收
    - 超时控制
  - `ClientBootstrap` 简单测试客户端
  - `RpcClientExample` 示例集合

#### 技术实现
- Java原生Socket实现TCP通信
- Java原生ObjectInputStream/ObjectOutputStream实现序列化
- 反射机制实现动态方法调用
- 线程池处理并发请求
- 异常处理和资源管理

#### 文档
- `README.md` - 英文版完整文档
- `README_CN.md` - 中文版完整文档
- `QUICK_START.md` - 快速开始指南
- `PROJECT_SUMMARY.md` - 项目总结（面试准备）
- `CONTRIBUTING.md` - 贡献指南
- `CHANGELOG.md` - 更新日志（本文件）
- `LICENSE` - MIT开源许可证

#### 工具脚本
- `git-init.sh` - Git初始化脚本
- `verify-build.sh` - 项目验证脚本

### 技术栈
- Java 8+
- Maven 3.6+
- Java Socket API
- Java Reflection API
- Java Serialization
- ExecutorService

### 代码质量
- 完善的JavaDoc注释
- 清晰的日志输出
- 异常处理机制
- 资源自动释放
- 编码规范

---

## 版本说明

### 版本格式
- **主版本号**：不兼容的API修改
- **次版本号**：向下兼容的功能性新增
- **修订号**：向下兼容的问题修正

### 开发阶段
- `1.0.0-SNAPSHOT`: 第一阶段（当前）- 基础RPC框架
- `2.0.0-SNAPSHOT`: 第二阶段（计划）- 工业级组件
- `3.0.0-SNAPSHOT`: 第三阶段（计划）- 生产级特性

### 里程碑
- ✅ 2025-12-23: 第一阶段完成
- 🚧 待定: 第二阶段开始
- 🚧 待定: 第三阶段开始

---

**注意**: 这是一个学习项目，版本号主要用于标记开发阶段。

