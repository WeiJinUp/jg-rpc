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

## [2.0.0-SNAPSHOT] - 2025-12-25

### 新增 - 第二阶段（工业级组件）✅

#### 网络层升级
- **Netty集成** - 替换原生Socket，使用高性能NIO框架
  - Boss/Worker线程组架构
  - 事件驱动模型
  - 连接复用
  
#### 自定义通信协议
- **协议设计** - 标准化的二进制协议
  - 魔数识别（0xCAFEBABE）
  - 版本控制
  - 序列化算法标识
  - 消息类型区分
  - 长度字段防粘包
  
#### 编解码器
- **RpcEncoder** - 消息编码器
  - 按协议格式编码
  - 支持多种序列化
- **RpcDecoder** - 消息解码器
  - LengthFieldBasedFrameDecoder
  - 自动拆包
  
#### 可插拔序列化
- **Serializer接口** - 序列化抽象
- **JavaSerializer** - Java原生序列化
- **JsonSerializer** - Gson JSON序列化
- **SerializerFactory** - 工厂管理

#### 动态代理
- **RpcClientProxy** - JDK动态代理
  - 透明RPC调用
  - 自动请求构建
  - 像调用本地方法
  
#### 异步支持
- **CompletableFuture** - 异步请求处理
- **RequestId匹配** - 请求响应关联

#### 基础设施
- **日志系统** - SLF4J + Logback
- **优雅关闭** - ShutdownHook

### 技术栈更新
- Netty 4.1.100.Final
- Gson 2.10.1
- SLF4J 2.0.9 + Logback 1.4.11

### 性能提升
- QPS: ~1K → ~10K (10倍提升)
- 连接管理: 每次新建 → 连接复用
- I/O模型: 阻塞 → 非阻塞

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

