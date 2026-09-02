package com.cn.ipc.server

/**
 * 异常与监控大盘 (IpcTelemetry)
 *
 * 负责收集跨进程调用的 P50/P99 耗时、限流拦截率、崩溃率，并提供插槽导出到外部 APM 监控系统。
 */
object IpcTelemetry {
    
    /**
     * 记录一次跨进程 IPC 调用的详细指标数据。
     *
     * @param traceId 用于分布式链路追踪的唯一追踪 ID
     * @param serviceId 目标服务的标识 ID
     * @param operationId 具体被调用操作的标识 ID
     * @param durationMs 此次调用耗时（单位：毫秒）
     * @param isSuccess 本次调用是否成功
     */
    fun recordCall(
        traceId: String,
        serviceId: Int,
        operationId: Int,
        durationMs: Long,
        isSuccess: Boolean
    ) {
        // TODO: 接入真实的 APM (Application Performance Management) SDK，比如 Firebase 或自研监控体系
    }
    
    /**
     * 记录服务端繁忙（因限流或队列满导致拒绝请求）的事件。
     *
     * @param serviceId 触发限流的目标服务 ID
     */
    fun recordServerBusy(serviceId: Int) {
        // TODO: 记录限流事件，用于统计限流拦截率和报警
    }
}
