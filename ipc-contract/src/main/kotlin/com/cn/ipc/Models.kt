/**
 * IPC 协议中使用的数据模型定义。
 *
 * 包含握手请求/响应、RPC错误信息等实体类，这些类均实现了 [Parcelable] 接口
 * 以便在 Android 进程间进行序列化和反序列化传输。
 */
package com.cn.ipc

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 客户端握手请求数据包。
 *
 * 在连接建立初期，客户端通过 [IIpcBroker.handshake] 将此对象发送给服务端，
 * 服务端根据协议版本与能力位掩码决定是否接受连接。
 *
 * @property protocolMajor         客户端支持的协议主版本号，主版本不兼容时握手拒绝。
 * @property protocolMinor         客户端支持的协议次版本号，用于协商可选特性。
 * @property clientPackage         客户端应用包名，服务端可用于权限校验或日志追踪。
 * @property clientVersionCode     客户端应用版本号（versionCode），便于服务端判断兼容性。
 * @property requestedCapabilities 客户端期望启用的能力集合（位掩码），服务端取交集后返回。
 * @property nonce                 客户端生成的随机一次性数，用于防重放攻击及会话绑定。
 */
@Parcelize
data class ClientHello(
    val protocolMajor: Int,
    val protocolMinor: Int,
    val clientPackage: String,
    val clientVersionCode: Long,
    val requestedCapabilities: Long,
    val nonce: Long
) : Parcelable

/**
 * 服务端握手响应数据包。
 *
 * 由服务端在收到 [ClientHello] 后通过 [IIpcBroker.handshake] 返回，
 * 包含本次会话协商结果及服务端基础信息。
 *
 * @property protocolMajor         服务端实际使用的协议主版本号。
 * @property protocolMinor         服务端实际使用的协议次版本号。
 * @property serverVersionCode     服务端应用版本号（versionCode），供客户端兼容性判断。
 * @property supportedCapabilities 服务端与客户端协商后实际启用的能力集合（位掩码）。
 * @property maxInlinePayloadBytes 服务端允许的单次内联载荷最大字节数，超出时需走文件描述符传输。
 * @property serviceVersions       服务端已注册的服务版本映射表，key 为 serviceId，value 为当前版本号。
 * @property sessionId             本次会话唯一 ID，由服务端生成，可用于日志关联与会话追踪。
 */
@Parcelize
data class ProtocolInfo(
    val protocolMajor: Int,
    val protocolMinor: Int,
    val serverVersionCode: Long,
    val supportedCapabilities: Long,
    val maxInlinePayloadBytes: Int,
    val serviceVersions: Map<Int, Int>,
    val sessionId: Long
) : Parcelable

/**
 * RPC 调用错误描述对象。
 *
 * 当服务端处理 RPC 请求失败时，将此对象通过异常或回调返回给客户端。
 * 客户端可依据 [retryable] 和 [retryAfterMs] 决定是否进行重试。
 *
 * @property domain       错误域，用于区分错误来源（如网络层、业务层、框架层等）。
 * @property code         错误码，同一 [domain] 内唯一，客户端根据此值做精确错误处理。
 * @property safeMessage  可安全展示给用户的错误描述（不含敏感信息），可为 null。
 * @property retryable    是否可重试。为 true 时客户端可在 [retryAfterMs] 毫秒后重试。
 * @property retryAfterMs 建议重试的等待时间（毫秒），仅在 [retryable] 为 true 时有效。
 * @property traceId      分布式追踪 ID，用于日志关联与问题排查，可为 null。
 */
@Parcelize
data class RpcError(
    val domain: Int,
    val code: Int,
    val safeMessage: String?,
    val retryable: Boolean,
    val retryAfterMs: Long,
    val traceId: String?
) : Parcelable