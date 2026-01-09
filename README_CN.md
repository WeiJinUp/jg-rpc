# JG-RPC 框架

[![Java](https://img.shields.io/badge/Java-8+-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com)

> 一个为现代分布式系统（尤其是DDD架构）设计的轻量级、高可扩展的Java RPC框架

[English](README.md) | 简体中文

## 📋 目录

- [项目概述](#项目概述)
- [核心特性](#核心特性)
- [技术架构](#技术架构)
- [快速开始](#快速开始)
- [项目结构](#项目结构)
- [使用示例](#使用示例)
- [开发路线](#开发路线)
- [文档导航](#文档导航)
- [技术亮点](#技术亮点)
- [许可证](#许可证)

## 🎯 项目概述

**JG-RPC** 是一个从零开始构建的远程过程调用（RPC）框架，旨在深入理解分布式通信的核心原理。这个项目展示了对以下技术的深刻理解：

- 基于Java Socket的网络编程
- 对象序列化与反序列化
- 多线程请求处理
- 服务注册模式
- 基于反射的方法调用

这是**三阶段项目** - **全部完成！**
- ✅ **第一阶段**：使用原生Java API构建最简RPC骨架（已完成）
- ✅ **第二阶段**：引入工业级组件（Netty、动态代理、可插拔序列化）（已完成）
- ✅ **第三阶段**：生产级特性（服务发现、负载均衡、异步调用、优雅停机）（已完成）

## ✨ 核心特性

### 第一阶段：基础框架（已完成）

- 🔌 **纯Java实现** - 无外部依赖，仅使用Java SE
- 🌐 **基于Socket通信** - 使用`java.net.Socket`实现可靠的TCP/IP通信
- 🔄 **对象序列化** - 使用Java原生序列化处理请求/响应
- 🔍 **基于反射的调用** - 使用Java反射API实现动态方法调用
- 🧵 **多线程处理** - 线程池处理并发客户端请求
- 📝 **服务注册** - 简单但有效的服务注册机制
- 💬 **请求/响应模型** - 清晰的DTO设计用于RPC通信

### 第二阶段：工业级组件（已完成）

- ⚡ **Netty集成** - 高性能异步网络通信
- 🎭 **动态代理** - 像调用本地方法一样透明的RPC调用
- 🔧 **可插拔序列化** - 支持JSON、Java序列化和自定义序列化器
- 📦 **自定义协议** - 包含魔数、版本号、消息类型的二进制协议
- 🔄 **自定义编解码器** - 高效的消息序列化编解码器
- 🏗️ **连接复用** - 长连接提升性能

### 第三阶段：生产级特性（已完成）

- 🏗️ **服务发现** - 基于Zookeeper的服务注册与发现
- ⚖️ **负载均衡** - 多种策略（随机、轮询、一致性哈希）
- ⚡ **异步调用** - 基于CompletableFuture的异步调用
- 🛡️ **优雅停机** - 正确的资源清理和连接排空
- 🔄 **客户端缓存** - 连接池和连接复用
- 📊 **临时节点** - 服务故障时自动注销

## 🏗️ 技术架构

### 第一阶段架构图

```
┌─────────────────┐                    ┌─────────────────┐
│   RPC客户端     │                    │   RPC服务端     │
│                 │                    │                 │
│  ┌───────────┐  │                    │  ┌───────────┐  │
│  │ 创建请求  │  │                    │  │  服务注册 │  │
│  └─────┬─────┘  │   1. TCP连接       │  └─────┬─────┘  │
│        │        │ ─────────────────> │        │        │
│  ┌─────▼─────┐  │                    │  ┌─────▼─────┐  │
│  │  序列化   │  │   2. 发送请求      │  │ 反序列化  │  │
│  │  请求对象 │  │ ─────────────────> │  │  请求对象 │  │
│  └───────────┘  │                    │  └─────┬─────┘  │
│                 │                    │        │        │
│                 │                    │  ┌─────▼─────┐  │
│                 │                    │  │ 反射调用  │  │
│  ┌───────────┐  │   3. 获取响应      │  │  目标方法 │  │
│  │ 反序列化  │  │ <───────────────── │  └─────┬─────┘  │
│  │  响应对象 │  │                    │        │        │
│  └─────┬─────┘  │                    │  ┌─────▼─────┐  │
│        │        │                    │  │  序列化   │  │
│  ┌─────▼─────┐  │                    │  │  响应对象 │  │
│  │ 返回结果  │  │                    │  └───────────┘  │
│  └───────────┘  │                    │                 │
└─────────────────┘                    └─────────────────┘
```

### 模块设计

```
jg-rpc/
├── rpc-api/           # 公共接口和DTO
│   ├── HelloService   # 示例服务接口
│   ├── RpcRequest     # 请求封装
│   └── RpcResponse    # 响应封装
│
├── rpc-server/        # 服务端实现
│   ├── RpcServer      # 核心服务端逻辑
│   ├── HelloServiceImpl
│   └── ServerBootstrap
│
└── rpc-client/        # 客户端实现
    ├── RpcClient      # 核心客户端逻辑
    ├── ClientBootstrap
    └── RpcClientExample
```

## 🚀 快速开始

### 环境要求

- Java 8 或更高版本
- Maven 3.6+

### 构建项目

```bash
# 克隆仓库
git clone https://github.com/WeiJinUp/jg-rpc.git
cd jg-rpc

# 使用Maven构建
mvn clean install
```

### 启动服务端

```bash
# 进入服务端模块
cd rpc-server

# 运行服务端
mvn exec:java -Dexec.mainClass="com.jinguan.rpc.server.ServerBootstrap"
```

你将看到：
```
========================================
   JG-RPC Server Starting...           
========================================
[RpcServer] Registered service: com.jinguan.rpc.api.HelloService
[RpcServer] Starting RPC server on port 8888...
[RpcServer] RPC server started successfully, listening on port 8888
```

### 启动客户端

在新的终端中：

```bash
# 进入客户端模块
cd rpc-client

# 运行客户端
mvn exec:java -Dexec.mainClass="com.jinguan.rpc.client.ClientBootstrap"
```

你将看到：
```
========================================
   JG-RPC Client Starting...           
========================================
[RpcClient] Connecting to server at localhost:8888
[RpcClient] Sent request: RpcRequest{...}
[RpcClient] Received response: RpcResponse{data=Hello, JinGuan! Welcome to JG-RPC Framework., success=true}

========================================
   RPC Call Successful!                
========================================
Result: Hello, JinGuan! Welcome to JG-RPC Framework.
```

## 📁 项目结构

```
jg-rpc/
├── pom.xml                                    # 父POM
├── README.md                                  # 英文说明文档
├── README_CN.md                               # 中文说明文档
├── LICENSE                                    # MIT许可证
├── .gitignore
│
├── rpc-api/                                   # API模块
│   ├── pom.xml
│   └── src/main/java/com/jinguan/rpc/api/
│       ├── HelloService.java                  # 服务接口
│       ├── async/
│       │   └── AsyncHelloService.java         # 异步服务接口
│       ├── codec/
│       │   ├── RpcEncoder.java                # 消息编码器
│       │   └── RpcDecoder.java                # 消息解码器
│       ├── dto/
│       │   ├── RpcRequest.java                # 请求DTO
│       │   └── RpcResponse.java               # 响应DTO
│       ├── loadbalance/
│       │   └── LoadBalancer.java              # 负载均衡器接口
│       ├── protocol/
│       │   ├── RpcProtocol.java               # 协议常量
│       │   └── RpcMessage.java                # 协议消息
│       ├── registry/
│       │   ├── ServiceDiscovery.java          # 服务发现接口
│       │   └── ServiceRegistry.java           # 服务注册接口
│       └── serializer/
│           ├── Serializer.java                # 序列化器接口
│           ├── JavaSerializer.java            # Java序列化
│           ├── JsonSerializer.java            # JSON序列化
│           └── SerializerFactory.java         # 序列化器工厂
│
├── rpc-server/                                # 服务端模块
│   ├── pom.xml
│   └── src/main/java/com/jinguan/rpc/server/
│       ├── RpcServer.java                     # 第一阶段：核心服务端
│       ├── ServerBootstrap.java              # 第一阶段：服务端入口
│       ├── RpcServerWithRegistry.java        # 第三阶段：带注册的服务端
│       ├── Phase3ServerBootstrap.java         # 第三阶段：服务端入口
│       ├── impl/
│       │   ├── HelloServiceImpl.java          # 服务实现
│       │   └── AsyncHelloServiceImpl.java     # 异步服务实现
│       ├── netty/
│       │   ├── NettyRpcServer.java            # 第二阶段：Netty服务端
│       │   ├── NettyRpcServerHandler.java     # 第二阶段：服务端处理器
│       │   └── NettyServerBootstrap.java      # 第二阶段：服务端入口
│       └── registry/
│           └── ZookeeperServiceRegistry.java  # 第三阶段：ZK注册中心
│
└── rpc-client/                                # 客户端模块
    ├── pom.xml
    └── src/main/java/com/jinguan/rpc/client/
        ├── RpcClient.java                     # 第一阶段：核心客户端
        ├── ClientBootstrap.java               # 第一阶段：客户端入口
        ├── RpcClientWithDiscovery.java        # 第三阶段：带发现的客户端
        ├── discovery/
        │   └── ZookeeperServiceDiscovery.java # 第三阶段：ZK服务发现
        ├── loadbalance/
        │   ├── RoundRobinLoadBalancer.java     # 第三阶段：轮询负载均衡
        │   ├── RandomLoadBalancer.java         # 第三阶段：随机负载均衡
        │   └── ConsistentHashLoadBalancer.java # 第三阶段：一致性哈希
        ├── netty/
        │   ├── NettyRpcClient.java            # 第二阶段：Netty客户端
        │   └── NettyClientBootstrap.java       # 第二阶段：客户端入口
        └── proxy/
            ├── RpcClientProxy.java             # 第二阶段：动态代理
            └── RpcClientProxyWithDiscovery.java # 第三阶段：带发现的代理
```

## 💡 使用示例

### 示例1：定义服务接口

```java
// 在rpc-api模块中
public interface UserService {
    User getUserById(Long id);
    boolean createUser(User user);
}
```

### 示例2：实现服务

```java
// 在rpc-server模块中
public class UserServiceImpl implements UserService {
    @Override
    public User getUserById(Long id) {
        // 你的业务逻辑
        return new User(id, "张三");
    }
    
    @Override
    public boolean createUser(User user) {
        // 你的业务逻辑
        return true;
    }
}
```

### 示例3：注册并启动服务端

```java
public class ServerBootstrap {
    public static void main(String[] args) {
        RpcServer server = new RpcServer(8888);
        
        // 注册服务
        server.register(new UserServiceImpl());
        
        // 启动服务器
        server.start();
    }
}
```

### 示例4：从客户端发起RPC调用（第一阶段）

```java
public class ClientApp {
    public static void main(String[] args) throws Exception {
        RpcClient client = new RpcClient("localhost", 8888);
        
        // 创建请求
        RpcRequest request = new RpcRequest(
            UserService.class.getName(),
            "getUserById",
            new Object[]{1L},
            new Class<?>[]{Long.class}
        );
        
        // 发送请求
        RpcResponse response = client.sendRequest(request);
        
        if (response.isSuccess()) {
            User user = (User) response.getData();
            System.out.println("获取到用户: " + user);
        }
    }
}
```

### 示例5：动态代理调用（第二阶段）

```java
public class ClientApp {
    public static void main(String[] args) {
        // 创建Netty客户端
        NettyRpcClient client = new NettyRpcClient("localhost", 9000);
        
        // 创建代理工厂
        RpcClientProxy proxyFactory = new RpcClientProxy(client);
        
        // 获取代理实例 - 看起来像本地对象！
        UserService userService = proxyFactory.getProxy(UserService.class);
        
        // 像调用本地方法一样调用远程方法
        User user = userService.getUserById(1L);
        System.out.println("获取到用户: " + user);
    }
}
```

### 示例6：服务发现和负载均衡（第三阶段）

```java
public class ClientApp {
    public static void main(String[] args) {
        // 创建服务发现
        ServiceDiscovery discovery = new ZookeeperServiceDiscovery("localhost:2181");
        
        // 创建负载均衡器
        LoadBalancer loadBalancer = new RoundRobinLoadBalancer();
        
        // 创建客户端管理器
        RpcClientWithDiscovery clientManager = 
            new RpcClientWithDiscovery(discovery, loadBalancer);
        
        // 创建代理工厂
        RpcClientProxyWithDiscovery proxyFactory = 
            new RpcClientProxyWithDiscovery(clientManager);
        
        // 获取代理 - 自动发现服务并负载均衡
        UserService userService = proxyFactory.getProxy(UserService.class);
        
        // 调用远程方法
        User user = userService.getUserById(1L);
        System.out.println("获取到用户: " + user);
    }
}
```

## 📚 文档导航

### 📖 可用文档

- **[快速开始指南](QUICK_START.md)** - 5分钟快速上手
- **[API文档](rpc-api/)** - 服务接口和DTO
- **[贡献指南](CONTRIBUTING.md)** - 如何参与贡献
- **[更新日志](CHANGELOG.md)** - 版本历史和更新记录

### 🔍 完整文档导航

查看 [DOCS_GUIDE.md](DOCS_GUIDE.md) 获取所有文档的完整指南。

### 📝 代码文档

所有核心代码文件都包含**详细的中英文双语注释**，说明：
- 每个方法的详细执行步骤
- 设计原理和设计决策
- 技术实现细节
- 各阶段之间的区别
- 最佳实践和设计模式

这些注释旨在帮助开发者理解：
- RPC框架的内部工作原理
- 网络编程和协议设计
- 服务发现和负载均衡机制
- 异步编程模式
- 分布式系统概念

## 🗺️ 开发路线

### ✅ 第一阶段：基础框架（已完成）
- [x] 多模块Maven项目搭建
- [x] 基于Socket的客户端-服务端通信
- [x] Java原生序列化
- [x] 基于反射的服务调用
- [x] 线程池处理并发请求
- [x] 服务注册模式

### ✅ 第二阶段：工业级组件（已完成）
- [x] 使用Netty替换Socket实现高性能I/O
- [x] 自定义协议设计（魔数、版本号、序列化类型等）
- [x] 自定义编解码器（LengthFieldBasedFrameDecoder）
- [x] 动态代理实现透明RPC调用
- [x] 可插拔序列化（JSON、Java序列化）
- [x] 工厂模式管理序列化器
- [x] 连接复用和长连接

### ✅ 第三阶段：生产级特性（已完成）
- [x] Zookeeper集成实现服务发现
- [x] 多种负载均衡策略（随机、轮询、一致性哈希）
- [x] 基于CompletableFuture的异步RPC调用
- [x] JVM关闭钩子实现优雅停机
- [x] 服务注册和自动注销
- [x] 客户端连接缓存和管理
- [x] 临时节点实现自动故障检测

## 🎓 技术亮点

通过构建这个项目，我深入理解了：

1. **网络编程**：如何使用Java Socket和Netty进行TCP通信
2. **序列化技术**：对象如何转换为字节流进行网络传输
3. **多线程编程**：如何使用线程池处理并发请求
4. **反射机制**：如何在运行时动态调用方法
5. **设计模式**：服务注册、DTO、工厂、代理、策略模式等
6. **分布式系统**：RPC、服务发现、负载均衡的核心概念
7. **协议设计**：自定义二进制协议的设计和实现
8. **事件驱动架构**：Netty的Reactor模式和事件循环模型
9. **服务发现**：基于Zookeeper的注册和发现机制
10. **负载均衡**：不同算法及其权衡

### 核心技术栈

**第一阶段**
- Java SE 8+
- Socket编程
- Java序列化
- 反射API
- 线程池（ExecutorService）
- Maven多模块管理

**第二阶段（已完成）**
- Netty 4.x
- JDK动态代理
- JSON序列化（Gson）
- 自定义协议设计
- 工厂模式
- LengthFieldBasedFrameDecoder

**第三阶段（已完成）**
- Apache Curator（Zookeeper客户端）
- CompletableFuture
- JVM Shutdown Hook
- 负载均衡算法（轮询、随机、一致性哈希）
- 服务注册发现
- 连接池管理

## 🤝 贡献

这是一个个人学习项目，但欢迎提出建议和反馈！

- 提交Issue反馈bug或建议
- Fork项目并尝试自己的功能
- 分享你的学习经验

## 📄 许可证

本项目采用 [MIT License](LICENSE) 开源。

## 👤 作者

**JinGuan**

- GitHub: [@WeiJinUp](https://github.com/WeiJinUp)
- Email: 18340309128@163.com

## 🙏 致谢

- 灵感来源于Dubbo、gRPC、Thrift等知名RPC框架
- 作为学习分布式系统的实践项目
- 感谢开源社区的知识分享

---

⭐ 如果这个项目对你有帮助，请给个Star支持一下！

**注意**：这是一个教育性质的项目，用于演示RPC框架的基本原理。生产环境请使用成熟的框架如Apache Dubbo或gRPC。

