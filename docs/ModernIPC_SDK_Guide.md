# Modern IPC SDK 接入与使用指南

**Modern IPC** 是一个纯协程、全类型安全且零 AIDL 的现代 Android 跨进程通信框架。业务线同学无需关心底层的 Binder 原理，像调用本地代码一样调用跨进程接口即可。

## 核心使用三步曲

### 1. 定义通信接口 (契约层)
所有的 IPC 接口都应该定义在公共模块中（如 `:ipc-api`）。通过给普通的 Kotlin 接口打上注解，KSP 会在编译期自动生成所需的底层桥接代码。

```kotlin
@IpcFacade(serviceId = 1001) // serviceId 必须全局唯一
interface IUserService {
    // 单向消息（Fire-and-Forget，不阻塞）
    @IpcOneway(transaction = 1)
    fun ping()

    // 异步挂起请求（像本地协程一样挂起，自带超时与异常传递）
    @IpcAsync(requestTransaction = 10, cancelTransaction = 11)
    suspend fun getUserInfo(userId: String): String

    // 流式订阅（跨进程持续广播）
    @IpcStream(subscribeTransaction = 20, unsubscribeTransaction = 21)
    fun observeGlobalBroadcast(): Flow<String>
}
```

### 2. 服务端实现 (Server)
在服务端进程中实现该接口，并将其注册到服务的网关中：

```kotlin
class UserServiceImpl : IUserService {
    override suspend fun getUserInfo(userId: String): String {
        delay(500) // 执行耗时网络/DB查询
        return "User-$userId"
    }
    // ... 其他实现略
}

class MyBrokerService : IpcBrokerService() {
    override fun onCreateRegistry(): DefaultIpcServiceRegistry {
        val registry = super.onCreateRegistry()
        // IUserServiceServerStub 是通过 KSP 自动生成的
        val stub = object : IUserServiceServerStub() {
            override val coroutineScope = CoroutineScope(Dispatchers.IO)
            val impl = UserServiceImpl()
            override suspend fun getUserInfo(userId: String) = impl.getUserInfo(userId)
            // ... 委托其他方法给 impl
        }
        registry.register(RegisteredService(serviceId = 1001, binder = stub))
        return registry
    }
}
```

### 3. 客户端调用 (Client)
在 UI 层（如 `ViewModel` 中）建立连接，并直接发起方法调用：

```kotlin
// 1. 建立 IPC 连接
val controller = IpcConnectionController(context, targetIntent, viewModelScope)
controller.connect()

// 2. 获取生成的代理类
val userService = IUserServiceClientAdapter(controller)

// 3. 极简调用 (无缝结合协程和 Flow)
viewModelScope.launch {
    try {
        val user = userService.getUserInfo("10086") // 自动挂起并等待跨进程结果
        // 成功，更新 UI
    } catch(e: Exception) {
        // 通信断开、服务端崩溃或服务端抛出的业务异常，统一在此捕获
    }
}
```
> **提示**：客户端由于和 `viewModelScope` 绑定，一旦 ViewModel 销毁，底层正在挂起的 IPC 请求会被自动拦截并取消，绝不会产生内存泄漏。
