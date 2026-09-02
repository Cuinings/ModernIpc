package com.cn.ipc.annotations

/**
 * 标记一个 Kotlin [suspend] 函数为跨进程异步请求。
 * 
 * 对应的底层 AIDL 将通过 request 和 cancel 两个 transaction 来实现挂起函数的桥接。
 * 这是一个核心注解，用于处理跨进程的协程挂起和恢复。
 *
 * @property requestTransaction 发起请求的 AIDL 事务码（在对应的 Service 接口内必须唯一）。
 * @property cancelTransaction  取消请求的 AIDL 事务码。用于在协程取消时中断跨进程请求。
 * @property idempotent         该方法是否为幂等操作。如果为 true，底层 Runtime 在断线重连后可能会自动重试，以提高系统的健壮性。
 */
@Target(AnnotationTarget.FUNCTION) // 该注解仅可应用于函数
@Retention(AnnotationRetention.SOURCE) // 该注解仅在源码级别保留，编译后将被丢弃，交由KSP处理
annotation class IpcAsync(
    val requestTransaction: Int,
    val cancelTransaction: Int,
    val idempotent: Boolean = false
)

/**
 * 标记一个返回 Kotlin `Flow` 的函数为跨进程流式订阅。
 *
 * 底层会桥接为一个包含订阅与取消订阅的方法对，从而实现响应式的数据流传输。
 * 适用于需要持续接收状态更新的场景。
 *
 * @property subscribeTransaction   发起订阅的 AIDL 事务码。用于通知服务端开始发送数据。
 * @property unsubscribeTransaction 取消订阅的 AIDL 事务码。用于通知服务端停止发送数据。
 */
@Target(AnnotationTarget.FUNCTION) // 同样仅应用于函数
@Retention(AnnotationRetention.SOURCE) // 源码保留
annotation class IpcStream(
    val subscribeTransaction: Int,
    val unsubscribeTransaction: Int
)

/**
 * 标记一个普通的单向、无返回值（返回 Unit）的方法。
 * 对应于 AIDL 中的 `oneway void` 方法。
 * 这种方法调用后会立即返回，不阻塞当前线程，也不等待服务端的执行结果。
 *
 * @property transaction 方法对应的 AIDL 事务码。
 */
@Target(AnnotationTarget.FUNCTION) // 作用于函数
@Retention(AnnotationRetention.SOURCE) // 源码保留
annotation class IpcOneway(
    val transaction: Int
)
