package com.cn.ipc.compiler

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier

/**
 * IPC 符号处理器，主要负责解析标注了 `@IpcFacade` 的接口，
 * 并生成对应的客户端适配器 (Client Adapter) 和服务端存根 (Server Stub)。
 *
 * @param codeGenerator KSP 代码生成器。
 * @param logger KSP 日志记录器，用于输出编译期信息和错误。
 */
class IpcSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    /**
     * 处理符号的核心逻辑。
     *
     * @param resolver 符号解析器。
     * @return 无法处理或延迟处理的符号列表。
     */
    override fun process(resolver: Resolver): List<KSAnnotated> {
        // 查找所有标注了 @IpcFacade 的符号
        val symbols = resolver.getSymbolsWithAnnotation("com.cn.ipc.annotations.IpcFacade")
        
        // 过滤出无法处理的非类声明符号
        val unableToProcess = symbols.filterNot { it is KSClassDeclaration }.toList()
        
        val clientAdapterGenerator = ClientAdapterGenerator(codeGenerator)
        val serverStubGenerator = ServerStubGenerator(codeGenerator)

        // 仅处理类的声明（实际上是接口，在 validateFacade 中验证）
        symbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
            val isValid = validateFacade(classDeclaration)
            if (isValid) {
                // 生成对应的 Client Adapter 和 Server Stub 代码
                clientAdapterGenerator.generate(classDeclaration)
                serverStubGenerator.generate(classDeclaration)
                logger.info("ModernIPC: Successfully generated Client & Server for -> ${classDeclaration.simpleName.asString()}")
            }
        }

        return unableToProcess
    }

    /**
     * 校验 @IpcFacade 标注的类是否合法。
     * - 必须是 interface。
     * - 挂起函数必须有 @IpcAsync 注解。
     * - 返回 Flow 的函数必须有 @IpcStream 注解。
     *
     * @param classDeclaration 待校验的类声明。
     * @return 如果类结构符合要求则返回 true，否则返回 false。
     */
    private fun validateFacade(classDeclaration: KSClassDeclaration): Boolean {
        var isValid = true
        // 必须是接口
        if (classDeclaration.classKind != com.google.devtools.ksp.symbol.ClassKind.INTERFACE) {
            logger.error("@IpcFacade 只能用于 interface，但找到了 ${classDeclaration.classKind}", classDeclaration)
            return false
        }

        val functions = classDeclaration.getAllFunctions()
        functions.forEach { function ->
            // 忽略通用方法
            if (function.simpleName.asString() in listOf("equals", "hashCode", "toString")) return@forEach
            
            val isSuspend = function.modifiers.contains(Modifier.SUSPEND)
            
            // 检查挂起函数是否打上了 @IpcAsync 注解
            if (isSuspend) {
                val hasIpcAsync = function.annotations.any { 
                    it.shortName.asString() == "IpcAsync" 
                }
                if (!hasIpcAsync) {
                    logger.error(
                        "挂起函数 ${function.simpleName.asString()} 必须打上 @IpcAsync 注解以声明事务码。", 
                        function
                    )
                    isValid = false
                }
            } else {
                // 非挂起函数，检查是否有 @IpcOneway 或 @IpcStream
                val isFlow = function.returnType?.resolve()?.declaration?.qualifiedName?.asString() == "kotlinx.coroutines.flow.Flow"
                if (isFlow) {
                    val hasIpcStream = function.annotations.any { it.shortName.asString() == "IpcStream" }
                    if (!hasIpcStream) {
                        logger.error(
                            "返回 Flow 的流式函数 ${function.simpleName.asString()} 必须打上 @IpcStream 注解。", 
                            function
                        )
                        isValid = false
                    }
                }
            }
        }
        return isValid
    }
}
