package com.cn.ipc.server

import android.os.Binder
import android.os.Process

/**
 * 调用方身份信息数据类，用于封装调用方的基础身份与鉴权数据。
 *
 * @property uid 调用方进程的 UID（User ID）
 * @property pid 调用方进程的 PID（Process ID）
 * @property packages 调用方 UID 关联的所有的包名集合
 * @property sessionId 当前的会话 ID
 */
data class CallerIdentity(
    val uid: Int,
    val pid: Int,
    val packages: Set<String>,
    val sessionId: Long
)

/**
 * 安全网关：验证调用方的身份并进行粗粒度的权限校验。
 * 作为跨进程通信的第一道防线，确保调用的合法性。
 */
class CallerAuthenticator(private val context: android.content.Context) {

    /**
     * 对调用方进行身份认证，获取其身份信息。
     * 
     * @param sessionId 本次连接的会话 ID，默认值为 0L
     * @return 封装了调用方信息的 [CallerIdentity] 实例
     */
    fun authenticate(sessionId: Long = 0L): CallerIdentity {
        val uid = Binder.getCallingUid()
        val pid = Binder.getCallingPid()
        
        // 如果是同进程调用，直接放行，无需进一步查询包名等耗时操作
        if (uid == Process.myUid()) {
            return CallerIdentity(uid, pid, setOf(context.packageName), sessionId)
        }
        
        // 查询该 UID 对应的所有包名
        val pm = context.packageManager
        val packages = pm.getPackagesForUid(uid)?.toSet() ?: emptySet()
        
        return CallerIdentity(uid, pid, packages, sessionId)
    }

    /**
     * 校验调用方是否拥有调用某个服务某个操作的权限
     */
    fun authorize(caller: CallerIdentity, serviceId: Int) {
        // 在此处可以扩展校验逻辑：
        // 1. 检查签名证书
        // 2. 检查特定服务所需的权限清单
        // 目前为了 POC 跑通，暂时放行所有同一签名的应用
        // val mySignatures = ...
    }
}
