package com.cn.ipc.demo

import com.cn.ipc.server.DefaultIpcServiceRegistry
import com.cn.ipc.server.IpcBrokerService
import com.cn.ipc.server.RegisteredService

/**
 * IPC 服务端点服务 (Broker Service)。
 * 运行在服务端进程中，负责提供底层的 Binder 通信支持，并注册和管理所有跨进程服务。
 */
class MyBrokerService : IpcBrokerService() {
    
    /**
     * 创建并初始化 IPC 服务注册表。
     * 在此方法中，我们将真实的业务服务与其对应的 Binder Stub 进行绑定和注册。
     *
     * @return 配置完毕的服务注册表实例
     */
    override fun onCreateRegistry(): DefaultIpcServiceRegistry {
        val registry = DefaultIpcServiceRegistry()
        
        // 实例化真实的业务处理逻辑
        val serviceImpl = UserServiceImpl()
        
        // 创建对应的 Stub，它是跨进程通信中服务端的 Binder 存根。
        // Stub 将接收到的 IPC 调用转发给实际的业务实现 (serviceImpl)。
        val stub = object : com.cn.ipc.api.test.IUserServiceServerStub() {
            // 指定协程作用域，用于执行服务端的挂起函数和流操作
            override val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
            
            override fun ping() = serviceImpl.ping()
            override fun logMessage(msg: String) = serviceImpl.logMessage(msg)
            override suspend fun getLargeData(sizeInBytes: Int) = serviceImpl.getLargeData(sizeInBytes)
            override suspend fun getUserInfo(userId: String) = serviceImpl.getUserInfo(userId)
            override fun observeUserStatus(userId: String) = serviceImpl.observeUserStatus(userId)
            override fun observeGlobalBroadcast() = serviceImpl.observeGlobalBroadcast()
        }

        // 将该服务注册到注册表中，公开给客户端调用
        registry.register(
            RegisteredService(
                serviceId = 1001,           // 服务的唯一标识符
                apiVersion = 1,             // 接口的 API 版本，用于兼容性检查
                apiHash = "hash123",        // 接口定义的哈希值，确保客户端与服务端接口定义一致
                requiredCapability = 0L,    // 调用此服务所需的权限能力标识
                permission = null,          // 可选的 Android 权限字符串要求
                binder = stub               // 注册刚刚构建的 Binder Stub
            )
        )
        return registry
    }
}
