package com.cn.ipc.server

import android.os.RemoteException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher

/**
 * 有界执行器：避免服务端被过多并发请求拖垮。
 * 如果队列满了，直接拒绝请求（Fail-Fast）。
 */
class BoundedDispatcher(
    corePoolSize: Int = 4,
    maxPoolSize: Int = 16,
    queueCapacity: Int = 128
) {
    private val threadPool = ThreadPoolExecutor(
        corePoolSize,
        maxPoolSize,
        60L,
        TimeUnit.SECONDS,
        ArrayBlockingQueue(queueCapacity),
        { r -> Thread(r, "IpcServer-Worker") },
        ThreadPoolExecutor.AbortPolicy() // 队列满了抛出 RejectedExecutionException
    )
    
    /**
     * 将有界线程池转换为协程调度器，用于在协程上下文中执行任务。
     */
    val coroutineDispatcher: CoroutineDispatcher = threadPool.asCoroutineDispatcher()
    
    /**
     * 提交一个任务到线程池中执行。
     * 如果当前任务队列已满，会触发 Fail-Fast（快速失败）机制，直接抛出 [RemoteException] 拒绝该请求。
     * 
     * @param block 需要执行的具体任务逻辑
     * @throws RemoteException 当服务器负载过高、任务队列已满时抛出此异常
     */
    fun execute(block: Runnable) {
        try {
            // 尝试将任务提交给线程池处理
            threadPool.execute(block)
        } catch (e: java.util.concurrent.RejectedExecutionException) {
            // 捕获拒绝执行异常并转换为远端异常返回给调用方
            throw RemoteException("SERVER_BUSY: Server is overloaded, queue is full.")
        }
    }
}
