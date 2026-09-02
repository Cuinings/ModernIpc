package com.cn.ipc.client

import kotlin.reflect.KClass

/**
 * M4: AndLinker 双栈兼容路由层
 *
 * 针对已经在重度使用 AndLinker 的存量业务，我们不能“一刀切”地强行迁移。
 * 该 Router 提供了一个统一的入口，通过 A/B 实验、按 UID 灰度或服务端配置，
 * 动态决定某次调用是走老旧的 AndLinker 反射通道，还是走全新的 Modern IPC 强类型通道。
 */
class ModernIpcRouter {

    /**
     * 判断当前服务或用户是否应该命中新架构灰度。
     * 实际生产中应接入 AB 测试 SDK 或配置中心，根据 UID、设备等信息决定是否放量。
     *
     * @param serviceClass 需要获取的业务服务接口
     * @return 如果应该使用 Modern IPC（新通道），则返回 true；否则返回 false（使用老旧通道）。
     */
    private fun isHitModernIpc(serviceClass: KClass<*>): Boolean {
        // 示例：始终命中新版
        return true
    }

    /**
     * 获取业务接口。根据灰度策略返回不同的代理对象。
     *
     * @param serviceClass 业务接口，如 IUserService::class
     * @param modernAdapter 现代 IPC 架构自动生成的适配器，如 IUserServiceClientAdapter
     * @param fallbackAndLinker 老旧架构生成器
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getService(
        serviceClass: KClass<T>,
        modernAdapter: () -> T,
        fallbackAndLinker: () -> T
    ): T {
        return if (isHitModernIpc(serviceClass)) {
            modernAdapter()
        } else {
            fallbackAndLinker()
        }
    }
}
