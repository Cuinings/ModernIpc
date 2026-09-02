package com.cn.ipc.client

import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * 将 AIDL 的 Subscribe / Unsubscribe 模式桥接为 Kotlin Flow 的辅助工具类。
 * 由 KSP 生成的 Client Adapter 代码将调用此类方法。
 */
object FlowAdapterHelper {

    /**
     * 桥接为 callbackFlow。
     * 确保在 Flow 被收集 (collect) 时发起远程订阅，在协程取消时发送解除订阅。
     *
     * @param registry          客户端的订阅注册表
     * @param generation        当前连接的生命周期代次
     * @param serviceId         所属服务 ID
     * @param operationId       具体方法/流的标识 ID
     * @param observerBinder    将要跨进程传递的 AIDL Observer Binder
     * @param subscribeAction   发起远程订阅的 lambda，必须返回远程生成的 subscriptionId。
     * @param unsubscribeAction 发起远程取消订阅的 lambda。
     * @param block             Flow 内部的逻辑，允许提前配置或发出初始状态。
     */
    fun <T> createIpcFlow(
        registry: SubscriptionRegistry,
        generation: Long,
        serviceId: Int,
        operationId: Int,
        observerBinder: android.os.IBinder,
        subscribeAction: () -> Long,
        unsubscribeAction: (Long) -> Unit,
        block: suspend ProducerScope<T>.() -> Unit = {}
    ): Flow<T> = callbackFlow {
        block()

        val remoteSubscriptionId = try {
            subscribeAction()
        } catch (e: Exception) {
            // 如果订阅失败（例如 RemoteException），立即关闭 Flow
            close(e)
            return@callbackFlow
        }

        // 注册到本地，以便支持断线重连恢复
        registry.register(
            remoteSubscriptionId = remoteSubscriptionId,
            generation = generation,
            serviceId = serviceId,
            operationId = operationId,
            observerBinder = observerBinder
        )

        // 挂起直到 Flow collection 被取消
        awaitClose {
            // 从本地注册表移除
            registry.unregister(remoteSubscriptionId)
            // 发起远程取消调用
            try {
                unsubscribeAction(remoteSubscriptionId)
            } catch (ignored: Exception) {
                // 取消时抛出异常通常意味着 Binder 已经断开，可以安全忽略
            }
        }
    }
}
