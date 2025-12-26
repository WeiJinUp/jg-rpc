# JG-RPC 快速开始指南

## 🎯 选择你的起点

JG-RPC分为三个阶段，每个阶段都是独立可运行的：

- **Phase 1**: 基础RPC（Socket + 反射）- 理解原理
- **Phase 2**: 工业级组件（Netty + 动态代理）- 提升性能
- **Phase 3**: 生产级特性（Zookeeper + 负载均衡）- 服务治理

选择你想要的阶段开始！

---

## 📌 Phase 1: 基础RPC（最简单）

### 环境要求
- Java 8+
- Maven 3.6+

### 步骤1: 构建项目
```bash
cd /Users/jinguan/Desktop/jg-rpc
mvn clean install
```

### 步骤2: 启动服务端（终端1）
```bash
cd rpc-server
mvn exec:java -Dexec.mainClass="com.jinguan.rpc.server.ServerBootstrap"
```

### 步骤3: 运行客户端（终端2）
```bash
cd rpc-client
mvn exec:java -Dexec.mainClass="com.jinguan.rpc.client.ClientBootstrap"
```

**成功标志**: 看到 "RPC Call Successful!"

---

## 📌 Phase 2: 工业级组件（Netty + 动态代理）

### 环境要求
- Java 8+
- Maven 3.6+

### 步骤1: 启动Netty服务端（终端1）
```bash
cd rpc-server
mvn exec:java -Dexec.mainClass="com.jinguan.rpc.server.netty.NettyServerBootstrap"
```

### 步骤2: 运行客户端（终端2）
```bash
cd rpc-client
mvn exec:java -Dexec.mainClass="com.jinguan.rpc.client.netty.NettyClientBootstrap"
```

**亮点**: 
- ✨ 动态代理，像调用本地方法
- ✨ 高性能Netty通信
- ✨ 可插拔序列化

---

## 📌 Phase 3: 生产级特性（完整功能）⭐

### 环境要求
- Java 8+
- Maven 3.6+
- Docker Desktop（运行Zookeeper）

### 步骤1: 启动Zookeeper
```bash
cd /Users/jinguan/Desktop/jg-rpc
docker-compose up -d
```

验证Zookeeper运行：
```bash
docker ps | grep zookeeper
```

### 步骤2: 启动服务端（终端1）
```bash
cd rpc-server
mvn exec:java -Dexec.mainClass="com.jinguan.rpc.server.Phase3ServerBootstrap"
```

### 步骤3: 运行客户端（终端2）
```bash
cd rpc-client
mvn exec:java -Dexec.mainClass="com.jinguan.rpc.client.Phase3ClientBootstrap"
```

### 步骤4: 测试负载均衡（可选）

启动第二个服务端实例（终端3）：
```bash
cd rpc-server
# 修改端口为9002，然后运行
# 或者使用 -Dport=9002 参数
mvn exec:java -Dexec.mainClass="com.jinguan.rpc.server.Phase3ServerBootstrap"
```

客户端会自动在两个服务器之间负载均衡！

### 步骤5: 测试优雅停机

在服务端终端按 `Ctrl+C`，观察输出：
```
========================================
  Starting graceful shutdown...
========================================
Step 1: Unregistering services from Zookeeper...
✓ All services unregistered
Step 2: Waiting for ongoing requests to complete...
✓ Wait completed
Step 3: Shutting down Netty server...
✓ Netty server stopped
========================================
  Graceful shutdown completed
========================================
```

**完整特性**:
- ✨ 自动服务注册与发现
- ✨ 轮询负载均衡
- ✨ 异步调用支持
- ✨ 优雅停机

---

## 🐛 常见问题

### Q1: 端口被占用
修改端口号或杀掉占用进程：
```bash
lsof -i :9000
kill -9 <PID>
```

### Q2: Zookeeper连接失败
确保Zookeeper运行中：
```bash
docker ps | grep zookeeper
docker logs jg-rpc-zookeeper
```

重启Zookeeper：
```bash
docker-compose restart
```

### Q3: 如何停止Zookeeper
```bash
docker-compose down
```

### Q4: 没有Docker怎么办？

可以下载Zookeeper独立版本：
```bash
# 下载
wget https://dlcdn.apache.org/zookeeper/zookeeper-3.8.3/apache-zookeeper-3.8.3-bin.tar.gz
tar -xzf apache-zookeeper-3.8.3-bin.tar.gz
cd apache-zookeeper-3.8.3-bin

# 启动
bin/zkServer.sh start
```

---

## 📊 功能对比

| 功能 | Phase 1 | Phase 2 | Phase 3 |
|------|---------|---------|---------|
| 网络 | Socket | Netty | Netty |
| 调用方式 | 手动构建 | 动态代理 | 动态代理 |
| 序列化 | Java原生 | 可插拔 | 可插拔 |
| 服务发现 | ❌ | ❌ | ✅ Zookeeper |
| 负载均衡 | ❌ | ❌ | ✅ 3种策略 |
| 异步调用 | ❌ | 部分 | ✅ 完整支持 |
| 优雅停机 | ❌ | 基础 | ✅ 完整流程 |
| 端口 | 8888 | 9000 | 9001 |

---

## 🎓 学习路径建议

### 新手（第一次接触）
```
Phase 1 → 理解RPC原理 → Phase 2 → 理解工业级实践 → Phase 3 → 理解服务治理
```

### 时间有限
```
直接看 Phase 3 → 然后回看 Phase 1/2 理解演进过程
```

### 准备面试
```
Phase 1 理解原理 → Phase 3 展示完整功能 → 对比说明技术选型
```

---

## 🚀 下一步

- 查看 [README.md](README.md) 了解完整功能
- 查看源码理解实现原理
- 尝试添加自己的服务
- 思考如何应用到实际项目

---

**提示**: Phase 3 需要 Zookeeper，如果暂时没有环境，可以先从 Phase 1/2 开始！
