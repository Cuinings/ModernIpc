# Modern IPC 🚀

![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)
![Kotlin](https://img.shields.io/badge/kotlin-1.9.22-orange.svg)
![Coroutines](https://img.shields.io/badge/coroutines-1.7.3-success.svg)

**Modern IPC** 是一个专为 Android 现代架构设计的**纯协程、全类型安全、零 AIDL** 的跨进程通信（IPC）框架。
抛弃传统的 `.aidl` 文件与恶心的回调地狱，直接使用 Kotlin 接口 + 注解，通过 KSP (Kotlin Symbol Processing) 在编译期自动生成所有底层 Binder 桥接代码。

完全拥抱 **Kotlin Coroutines** 与 **Kotlin Flow**，让跨进程调用像调用本地挂起函数一样简单、安全且高效。

---

## ✨ 核心特性

- **🛑 告别 AIDL**：只需定义普通的 Kotlin `interface`，打上 `@IpcFacade` 注解即可。
- **⚡ 纯协程驱动 (Suspend)**：完美支持 `suspend fun`。底层采用非阻塞的 `PendingCallRegistry` 机制，千万级并发下也绝不发生 ANR 或死锁。
- **🌊 跨进程 Flow 状态流订阅**：原生支持 `Flow<T>`，热流 (SharedFlow) 跨进程多点广播，冷流按需拉取，数据变化实时推送到 UI。
- **🛡️ 极致的容灾与防内存泄漏**：
  - **进程死亡护盾**：自动监听底层 `DeathRecipient`，Server 端崩溃时客户端自动触发 **Exponential Backoff (指数退避)** 机制进行随机抖动重连，杜绝重连风暴。
  - **协程泄漏保护**：客户端页面销毁时只需 `job.cancel()`，底层 Continuation 会被自动安全移除，杜绝了传统 IPC 极其容易产生的回调泄露。
- **🔐 强校验鉴权网关**：内置 `CallerAuthenticator`，从 UID、PID 到包名签名进行全链路安全拦截。
- **🚀 Oneway 极速投递**：支持 `@IpcOneway`，纯 Fire-and-Forget 单向穿透，适合高频日志与埋点同步。

---

## 🏗️ 模块架构设计

- `:ipc-annotations`: 定义 `@IpcFacade`, `@IpcAsync`, `@IpcStream` 等核心元数据注解。
- `:ipc-compiler`: KSP 符号处理器。负责在编译期解析接口，自动生成 `XxxClientAdapter` 和 `XxxServerStub`。
- `:ipc-runtime-client`: 客户端引擎。管理 IPC 状态机 (`IpcConnectionController`)、挂起请求调度池及重连机制。
- `:ipc-runtime-server`: 服务端引擎。包含多线程限流器 (`BoundedDispatcher`)、安全鉴权网关及 Service 容器。
- `:ipc-api`: 存放您的业务通信接口定义 (`IUserService` 等)，KSP 在此模块自动生成代理代码。
- `:demo-app`: 包含全场景测试用例的宿主 App，可直接运行验证所有高并发及异常场景。

---

## 💻 快速开始

### 1. 定义通信接口 (Contract)
在 `:ipc-api` 模块中使用普通 Kotlin 语法定义接口：

```kotlin
@IpcFacade(serviceId = 1001, minApiVersion = 1)
interface IUserService {

    // 1. 简单的单向通信（不需要返回值，极速非阻塞）
    @IpcOneway(transaction = 1)
    fun ping()

    // 2. 异步挂起请求（像调用本地函数一样调用远程方法）
    @IpcAsync(requestTransaction = 10, cancelTransaction = 11, idempotent = true)
    suspend fun getUserInfo(userId: String): String

    // 3. 流式订阅（跨进程状态同步与广播）
    @IpcStream(subscribeTransaction = 20, unsubscribeTransaction = 21)
    fun observeGlobalBroadcast(): Flow<String>
}
```

### 2. 服务端实现 & 部署
实现接口并在 `IpcBrokerService` 中注册：

```kotlin
class UserServiceImpl : IUserService {
    override suspend fun getUserInfo(userId: String): String {
        delay(1000) // 模拟高耗时数据库查询
        return "User-$userId"
    }
    // ... 其他实现
}

class MyBrokerService : IpcBrokerService() {
    override fun onCreateRegistry(): DefaultIpcServiceRegistry {
        val registry = DefaultIpcServiceRegistry()
        
        // 注册实现并挂载生成的 ServerStub
        val stub = object : IUserServiceServerStub() {
            override val coroutineScope = CoroutineScope(Dispatchers.IO)
            /* 委托给 UserServiceImpl */
        }
        
        registry.register(RegisteredService(serviceId = 1001, binder = stub, ...))
        return registry
    }
}
```

### 3. 客户端调用
在 Activity 或 ViewModel 中极简调用：

```kotlin
// 1. 建立连接
val controller = IpcConnectionController(context, targetIntent, scope)
controller.connect()

// 2. 获取生成的代理类
val userService = IUserServiceClientAdapter(controller)

// 3. 发起挂起调用
scope.launch {
    try {
        val user = userService.getUserInfo("10086")
        println("收到数据: $user")
    } catch(e: Exception) {
        println("IPC 失败或超时: $e")
    }
}

// 4. 监听跨进程 Flow
userService.observeGlobalBroadcast()
    .onEach { msg -> println("收到广播: $msg") }
    .launchIn(scope)
```

---

## 🎮 Demo App 全场景测试用例

强烈建议您直接运行本项目自带的 `:demo-app`。它包含 8 大极度硬核的边缘场景测试验证，点击按钮即可体验：

1. **建立 IPC 连接**：观察连接状态机流转。
2. **极限压测**：瞬间发起 **1000 个挂起请求**，测试 BoundedDispatcher 吞吐量。
3. **长连接多流订阅**：同时订阅 3 个独立状态源。
4. **容灾测试 (强制杀掉 Server 进程)**：发送死亡指令，观察客户端 UI 不崩并执行 Exponential Backoff 重连。
5. **协程取消测试 (防内存泄漏)**：发起耗时请求并瞬间 `cancel()`，证明挂起句柄被安全回收。
6. **Oneway 单向消息投递**：Fire-and-Forget 高频埋点测试。
7. **大数据量跨进程传输**：单次传递 100KB 数据测试 Binder 序列化承载力。
8. **终极考验 (SharedFlow 广播)**：动态克隆 3 个独立的 Client Controller 同时连接 Server，验证同一条热流 (SharedFlow) 对多端的毫秒级精准广播。

---

## 🛠️ 构建与编译

本项目完全使用 Gradle Kotlin DSL 构建。
```bash
# 编译整个工程并生成 KSP 代码
./gradlew build

# 运行 Demo 验证测试
./gradlew :demo-app:installDebug
```

## 📝 设计蓝图与文档
- **开发接入指南 (必读)** 请参阅：[`docs/ModernIPC_Business_Interaction_Guide.md`](docs/ModernIPC_Business_Interaction_Guide.md)
- **架构设计全景文档** 请参阅：[`docs/ModernIPC_Architecture.md`](docs/ModernIPC_Architecture.md)
- 组件状态机演进模型位于 `IpcClientState` 与 `IpcConnectionController` 源码注解中。
