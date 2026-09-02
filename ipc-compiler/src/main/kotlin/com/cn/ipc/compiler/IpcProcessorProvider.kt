package com.cn.ipc.compiler

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * IPC 符号处理器提供者，用于在 KSP 编译期间创建 [IpcSymbolProcessor] 实例。
 */
class IpcProcessorProvider : SymbolProcessorProvider {
    /**
     * 创建并返回一个 [IpcSymbolProcessor] 实例。
     * 
     * @param environment KSP 处理环境，提供代码生成器和日志记录器等上下文。
     * @return 新建的 IPC 符号处理器。
     */
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return IpcSymbolProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger
        )
    }
}
