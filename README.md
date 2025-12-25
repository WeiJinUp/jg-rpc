# JG-RPC Framework

[![Java](https://img.shields.io/badge/Java-8+-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com)

> A lightweight, high-scalable Java RPC framework designed for modern distributed systems (especially DDD architecture)

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Usage Examples](#usage-examples)
- [Roadmap](#roadmap)
- [Documentation](#documentation)
- [Contributing](#contributing)
- [License](#license)

## 🎯 Overview

**JG-RPC** is a Remote Procedure Call (RPC) framework built from scratch to understand the core principles of distributed communication. This project demonstrates a deep understanding of:

- Network programming with Java Sockets
- Object serialization and deserialization
- Multi-threaded request handling
- Service registry pattern
- Reflection-based method invocation

This is a three-phase project:
- ✅ **Phase 1**: Barebones RPC using native Java APIs (Completed)
- ✅ **Phase 2**: Industrial-grade components (Netty, Dynamic Proxy, Pluggable Serialization) (Completed)
- 🚧 **Phase 3**: Production features (Service Discovery, Load Balancing, Async Calls, Graceful Shutdown)

## ✨ Features

### Current Features (Phase 1)

- 🔌 **Pure Java Implementation** - No external dependencies, built with Java SE
- 🌐 **Socket-based Communication** - Reliable TCP/IP communication using `java.net.Socket`
- 🔄 **Object Serialization** - Native Java serialization for request/response
- 🔍 **Reflection-based Invocation** - Dynamic method invocation using Java Reflection API
- 🧵 **Multi-threaded Processing** - Thread pool for handling concurrent client requests
- 📝 **Service Registry** - Simple but effective service registration mechanism
- 💬 **Request/Response Model** - Clean DTO design for RPC communication

### Planned Features (Phase 2 & 3)

- ⚡ **Netty Integration** - High-performance asynchronous network communication
- 🎭 **Dynamic Proxy** - Transparent RPC calls that look like local method calls
- 🔧 **Pluggable Serialization** - Support for JSON, Protobuf, and custom serializers
- 🏗️ **Service Discovery** - Zookeeper-based service registration and discovery
- ⚖️ **Load Balancing** - Multiple strategies (Random, Round-Robin, Weighted)
- ⚡ **Async Calls** - CompletableFuture-based asynchronous invocation
- 🛡️ **Graceful Shutdown** - Proper resource cleanup and connection draining

## 🏗️ Architecture

### Phase 1 Architecture

```
┌─────────────────┐                    ┌─────────────────┐
│   RPC Client    │                    │   RPC Server    │
│                 │                    │                 │
│  ┌───────────┐  │                    │  ┌───────────┐  │
│  │ Create    │  │                    │  │  Service  │  │
│  │ Request   │  │   1. TCP Connect   │  │  Registry │  │
│  └─────┬─────┘  │ ─────────────────> │  └─────┬─────┘  │
│        │        │                    │        │        │
│  ┌─────▼─────┐  │   2. Send Request  │  ┌─────▼─────┐  │
│  │ Serialize │  │ ─────────────────> │  │Deserialize│  │
│  │  Request  │  │                    │  │  Request  │  │
│  └───────────┘  │                    │  └─────┬─────┘  │
│                 │                    │        │        │
│                 │                    │  ┌─────▼─────┐  │
│                 │                    │  │ Reflection│  │
│  ┌───────────┐  │  3. Get Response   │  │  Invoke   │  │
│  │Deserialize│  │ <───────────────── │  └─────┬─────┘  │
│  │ Response  │  │                    │        │        │
│  └─────┬─────┘  │                    │  ┌─────▼─────┐  │
│        │        │                    │  │ Serialize │  │
│  ┌─────▼─────┐  │                    │  │ Response  │  │
│  │  Return   │  │                    │  └───────────┘  │
│  │  Result   │  │                    │                 │
│  └───────────┘  │                    │                 │
└─────────────────┘                    └─────────────────┘
```

### Module Design

```
jg-rpc/
├── rpc-api/           # Common interfaces and DTOs
│   ├── HelloService   # Example service interface
│   ├── RpcRequest     # Request encapsulation
│   └── RpcResponse    # Response encapsulation
│
├── rpc-server/        # Server implementation
│   ├── RpcServer      # Core server logic
│   ├── HelloServiceImpl
│   └── ServerBootstrap
│
└── rpc-client/        # Client implementation
    ├── RpcClient      # Core client logic
    ├── ClientBootstrap
    └── RpcClientExample
```

## 🚀 Quick Start

### Prerequisites

- Java 8 or higher
- Maven 3.6+

### Build the Project

```bash
# Clone the repository
git clone https://github.com/yourusername/jg-rpc.git
cd jg-rpc

# Build with Maven
mvn clean install
```

### Run the Server

```bash
# Navigate to server module
cd rpc-server

# Run server
mvn exec:java -Dexec.mainClass="com.jinguan.rpc.server.ServerBootstrap"
```

You should see:
```
========================================
   JG-RPC Server Starting...           
========================================
[RpcServer] Registered service: com.jinguan.rpc.api.HelloService
[RpcServer] Starting RPC server on port 8888...
[RpcServer] RPC server started successfully, listening on port 8888
```

### Run the Client

In a new terminal:

```bash
# Navigate to client module
cd rpc-client

# Run client
mvn exec:java -Dexec.mainClass="com.jinguan.rpc.client.ClientBootstrap"
```

You should see:
```
========================================
   JG-RPC Client Starting...           
========================================
[RpcClient] Connecting to server at localhost:8888
[RpcClient] Sent request: RpcRequest{interfaceName='com.jinguan.rpc.api.HelloService', methodName='hello', ...}
[RpcClient] Received response: RpcResponse{data=Hello, JinGuan! Welcome to JG-RPC Framework., success=true}

========================================
   RPC Call Successful!                
========================================
Result: Hello, JinGuan! Welcome to JG-RPC Framework.
```

## 📁 Project Structure

```
jg-rpc/
├── pom.xml                                    # Parent POM
├── README.md                                  # This file
├── .gitignore
│
├── rpc-api/                                   # API Module
│   ├── pom.xml
│   └── src/main/java/com/jinguan/rpc/api/
│       ├── HelloService.java                  # Service interface
│       └── dto/
│           ├── RpcRequest.java                # Request DTO
│           └── RpcResponse.java               # Response DTO
│
├── rpc-server/                                # Server Module
│   ├── pom.xml
│   └── src/main/java/com/jinguan/rpc/server/
│       ├── RpcServer.java                     # Core server
│       ├── ServerBootstrap.java               # Server entry point
│       └── impl/
│           └── HelloServiceImpl.java          # Service implementation
│
└── rpc-client/                                # Client Module
    ├── pom.xml
    └── src/main/java/com/jinguan/rpc/client/
        ├── RpcClient.java                     # Core client
        ├── ClientBootstrap.java               # Simple client test
        └── RpcClientExample.java              # Advanced examples
```

## 💡 Usage Examples

### Example 1: Define a Service Interface

```java
// In rpc-api module
public interface UserService {
    User getUserById(Long id);
    boolean createUser(User user);
}
```

### Example 2: Implement the Service

```java
// In rpc-server module
public class UserServiceImpl implements UserService {
    @Override
    public User getUserById(Long id) {
        // Your business logic here
        return new User(id, "John Doe");
    }
    
    @Override
    public boolean createUser(User user) {
        // Your business logic here
        return true;
    }
}
```

### Example 3: Register and Start Server

```java
public class ServerBootstrap {
    public static void main(String[] args) {
        RpcServer server = new RpcServer(8888);
        
        // Register service
        server.register(new UserServiceImpl());
        
        // Start server
        server.start();
    }
}
```

### Example 4: Make RPC Call from Client

```java
public class ClientApp {
    public static void main(String[] args) throws Exception {
        RpcClient client = new RpcClient("localhost", 8888);
        
        // Create request
        RpcRequest request = new RpcRequest(
            UserService.class.getName(),
            "getUserById",
            new Object[]{1L},
            new Class<?>[]{Long.class}
        );
        
        // Send request
        RpcResponse response = client.sendRequest(request);
        
        if (response.isSuccess()) {
            User user = (User) response.getData();
            System.out.println("Got user: " + user);
        }
    }
}
```

## 📚 Documentation

### 📖 Available Documents

- **[Quick Start Guide](QUICK_START.md)** - Get started in 5 minutes
- **[API Documentation](rpc-api/)** - Service interfaces and DTOs
- **[Contributing Guide](CONTRIBUTING.md)** - How to contribute
- **[Changelog](CHANGELOG.md)** - Version history and updates

### 🔍 Document Navigation

For a complete guide to all documentation, see [DOCS_GUIDE.md](DOCS_GUIDE.md).

## 🗺️ Roadmap

### ✅ Phase 1: Foundation (Completed)
- [x] Multi-module Maven project setup
- [x] Basic Socket-based client-server communication
- [x] Java native serialization
- [x] Reflection-based service invocation
- [x] Thread pool for concurrent request handling
- [x] Service registry pattern

### 🚧 Phase 2: Industrial Components (Planned)
- [ ] Replace Socket with Netty for high-performance I/O
- [ ] Custom protocol design (magic number, version, serialization type, etc.)
- [ ] Custom encoder/decoder (ByteToMessageCodec)
- [ ] Dynamic proxy for transparent RPC calls
- [ ] Pluggable serialization (JSON, Protobuf, Kryo)
- [ ] SPI (Service Provider Interface) for extensibility

### 🚧 Phase 3: Production Features (Planned)
- [ ] Zookeeper integration for service discovery
- [ ] Multiple load balancing strategies
- [ ] Asynchronous RPC calls with CompletableFuture
- [ ] Graceful shutdown with JVM shutdown hooks
- [ ] Health check and heartbeat mechanism
- [ ] Metrics and monitoring
- [ ] Circuit breaker pattern
- [ ] Rate limiting

## 🎓 Learning Outcomes

Building this project helped me understand:

1. **Network Programming**: How to use Java Sockets for TCP communication
2. **Serialization**: How objects are converted to bytes for network transmission
3. **Multi-threading**: How to handle concurrent requests using thread pools
4. **Reflection**: How to dynamically invoke methods at runtime
5. **Design Patterns**: Service Registry, DTO, Factory patterns
6. **Distributed Systems**: Core concepts of RPC and service-oriented architecture

## 🤝 Contributing

This is a personal learning project, but suggestions and feedback are welcome! Feel free to:

- Open an issue for bugs or suggestions
- Fork the project and experiment with your own features
- Share your learning experience

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

## 👤 Author

**JinGuan**

- GitHub: [@yourusername](https://github.com/yourusername)
- Email: your.email@example.com

## 🙏 Acknowledgments

- Inspired by popular RPC frameworks like Dubbo, gRPC, and Thrift
- Built as part of a learning journey to understand distributed systems
- Special thanks to the open-source community for knowledge sharing

---

⭐ If you find this project helpful, please consider giving it a star!

**Note**: This is an educational project built to demonstrate understanding of RPC fundamentals. For production use, consider established frameworks like Apache Dubbo or gRPC.

