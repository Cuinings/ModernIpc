package com.cn.ipc.server

import android.os.IInterface
import android.os.RemoteCallbackList
import android.os.RemoteException
import java.util.concurrent.atomic.AtomicLong

/**
 * 服务端管理 IPC 订阅与广播的管理器。
 * 对 Android 原生的 [RemoteCallbackList] 进行了封装，支持基于 SubscriptionId 的精确取消订阅。
 * 此类能够自动处理跨进程通信中客户端死亡等异常情况。
 *
 * @param T 具体的业务 Observer 接口（继承自 [IInterface]）
 */
class IpcSubscriptionManager<T : IInterface> {

    // 颁发给客户端的订阅 ID
    private val subscriptionIdCounter = AtomicLong(1)

    // RemoteCallbackList 自带线程安全和 Binder.DeathRecipient 能力
    // cookie 参数我们用来保存 subscriptionId，方便反向查找。
    private val callbackList = object : RemoteCallbackList<T>() {
        override fun onCallbackDied(callback: T, cookie: Any?) {
            super.onCallbackDied(callback, cookie)
            // 客户端意外死亡，自动清理，可在此处加入监控打点
        }
    }

    /**
     * 注册一个新的订阅。
     *
     * @param observer 客户端传来的 AIDL Observer
     * @return 分配给该订阅的全局唯一 ID
     */
    fun register(observer: T): Long {
        val subscriptionId = subscriptionIdCounter.getAndIncrement()
        // 将 subscriptionId 作为 cookie 存入，用于后续的精确取消
        callbackList.register(observer, subscriptionId)
        return subscriptionId
    }

    /**
     * 客户端主动取消订阅。
     *
     * @param subscriptionId 注册时返回的 ID
     */
    fun unregister(subscriptionId: Long) {
        // RemoteCallbackList 原生仅支持按 IInterface 对象注销，
        // 为了支持通过 ID 注销，我们需要遍历。虽然 O(N)，但在特定业务域内订阅数通常较小。
        // 或者客户端在调用时直接传 Binder 对象过来。
        // 为了兼容 AIDL 蓝图中 oneway unsubscribe(long id) 的设计，采用遍历。
        
        var targetObserver: T? = null
        val n = callbackList.beginBroadcast()
        try {
            for (i in 0 until n) {
                val cookie = callbackList.getBroadcastCookie(i)
                if (cookie == subscriptionId) {
                    targetObserver = callbackList.getBroadcastItem(i)
                    break
                }
            }
        } finally {
            callbackList.finishBroadcast()
        }

        if (targetObserver != null) {
            callbackList.unregister(targetObserver)
        }
    }

    /**
     * 安全地向所有存活的客户端广播事件。
     * 会自动处理 beginBroadcast / finishBroadcast 对，并吞没 DeadObjectException。
     *
     * @param action 广播动作，参数为单个 observer
     */
    fun broadcast(action: (T) -> Unit) {
        val n = callbackList.beginBroadcast()
        try {
            for (i in 0 until n) {
                val observer = callbackList.getBroadcastItem(i)
                try {
                    action(observer)
                } catch (e: RemoteException) {
                    // 该异常可能是因为客户端在刚刚那一瞬间死掉，或者调用了 oneway 但缓冲区满
                    // RemoteCallbackList 会在后续自动清理它，这里只需要忽略，不影响其他客户端。
                }
            }
        } finally {
            callbackList.finishBroadcast()
        }
    }

    /**
     * 清理所有订阅，通常在服务端宿主 Service 销毁时调用。
     */
    fun kill() {
        callbackList.kill()
    }
}
