# JG-RPC 快速开始指南

## 第一次运行

### 步骤1：构建项目

在项目根目录执行：

```bash
mvn clean install
```

### 步骤2：启动服务端

打开第一个终端，执行：

```bash
cd rpc-server
mvn exec:java -Dexec.mainClass="com.jinguan.rpc.server.ServerBootstrap"
```

或者如果你使用IDE（IntelliJ IDEA / Eclipse）：
1. 导入Maven项目
2. 找到 `ServerBootstrap.java`
3. 右键 → Run 'ServerBootstrap.main()'

### 步骤3：运行客户端

打开第二个终端，执行：

```bash
cd rpc-client
mvn exec:java -Dexec.mainClass="com.jinguan.rpc.client.ClientBootstrap"
```

或使用IDE：
1. 找到 `ClientBootstrap.java`
2. 右键 → Run 'ClientBootstrap.main()'

### 步骤4：运行示例程序

运行更多示例：

```bash
cd rpc-client
mvn exec:java -Dexec.mainClass="com.jinguan.rpc.client.RpcClientExample"
```

## 常见问题

### Q1: 端口8888已被占用怎么办？

修改端口号：
- 在 `ServerBootstrap.java` 中修改：`new RpcServer(8888)` → `new RpcServer(9999)`
- 在 `ClientBootstrap.java` 中修改：`new RpcClient("localhost", 8888)` → `new RpcClient("localhost", 9999)`

### Q2: 如何添加自己的服务？

**步骤1：在rpc-api模块定义接口**
```java
package com.jinguan.rpc.api;

public interface CalculatorService {
    int add(int a, int b);
    int subtract(int a, int b);
}
```

**步骤2：在rpc-server模块实现接口**
```java
package com.jinguan.rpc.server.impl;

import com.jinguan.rpc.api.CalculatorService;

public class CalculatorServiceImpl implements CalculatorService {
    @Override
    public int add(int a, int b) {
        return a + b;
    }
    
    @Override
    public int subtract(int a, int b) {
        return a - b;
    }
}
```

**步骤3：在ServerBootstrap中注册服务**
```java
RpcServer rpcServer = new RpcServer(8888);
rpcServer.register(new HelloServiceImpl());
rpcServer.register(new CalculatorServiceImpl()); // 新增
rpcServer.start();
```

**步骤4：在客户端调用**
```java
RpcRequest request = new RpcRequest(
    CalculatorService.class.getName(),
    "add",
    new Object[]{10, 20},
    new Class<?>[]{int.class, int.class}
);
RpcResponse response = rpcClient.sendRequest(request);
System.out.println("10 + 20 = " + response.getData());
```

### Q3: 客户端无法连接到服务器？

检查清单：
1. ✅ 服务器是否已启动
2. ✅ 端口号是否一致
3. ✅ 防火墙是否阻止了端口
4. ✅ 如果是远程连接，IP地址是否正确

## 下一步

- 阅读 [README.md](README.md) 了解完整功能
- 查看源码理解RPC实现原理
- 等待第二阶段更新（Netty + 动态代理）
- 等待第三阶段更新（服务发现 + 负载均衡）

## 需要帮助？

如有问题，欢迎提Issue或联系作者。

