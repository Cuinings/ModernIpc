package com.cn.ipc.compiler

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * 客户端适配器生成器。
 * 负责根据标有 `@IpcFacade` 的接口生成对应的客户端代理类，
 * 该代理类将本地的方法调用转换为跨进程的 Binder 通信。
 *
 * @param codeGenerator KSP 代码生成器，用于将生成的代码写入文件。
 */
class ClientAdapterGenerator(
    private val codeGenerator: CodeGenerator
) {
    /**
     * 为指定的接口生成客户端适配器类。
     *
     * @param classDeclaration 接口的类声明。
     */
    fun generate(classDeclaration: KSClassDeclaration) {
        val packageName = classDeclaration.packageName.asString()
        val interfaceName = classDeclaration.simpleName.asString()
        val adapterClassName = "${interfaceName}ClientAdapter"

        // 1. 创建类: class IUserServiceClientAdapter(val controller: IpcConnectionController) : IUserService
        val typeBuilder = TypeSpec.classBuilder(adapterClassName)
            .addSuperinterface(classDeclaration.toClassName())
            .addModifiers(KModifier.PUBLIC)
            
        // 添加主构造函数，接收 IPC 连接控制器
        val constructorBuilder = FunSpec.constructorBuilder()
            .addParameter("controller", com.squareup.kotlinpoet.ClassName("com.cn.ipc.client", "IpcConnectionController"))
        
        typeBuilder.primaryConstructor(constructorBuilder.build())
            .addProperty(
                com.squareup.kotlinpoet.PropertySpec.builder("controller", com.squareup.kotlinpoet.ClassName("com.cn.ipc.client", "IpcConnectionController"))
                    .initializer("controller")
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )

        // 2. 为每个接口方法生成对应的 override 实现
        val functions = classDeclaration.getAllFunctions()
        functions.forEach { function ->
            // 忽略 Object 的基础方法
            if (function.simpleName.asString() in listOf("equals", "hashCode", "toString")) return@forEach

            val funName = function.simpleName.asString()
            val isSuspend = function.modifiers.contains(Modifier.SUSPEND)

            val funBuilder = FunSpec.builder(funName)
                .addModifiers(KModifier.OVERRIDE)

            // 如果是挂起函数，需添加 suspend 修饰符
            if (isSuspend) {
                funBuilder.addModifiers(KModifier.SUSPEND)
            }

            // 添加方法参数
            function.parameters.forEach { param ->
                val paramName = param.name?.asString() ?: "arg"
                funBuilder.addParameter(paramName, param.type.toTypeName())
            }

            // 设置返回类型（Unit 忽略）
            val returnType = function.returnType?.resolve()
            if (returnType != null && returnType.declaration.qualifiedName?.asString() != "kotlin.Unit") {
                funBuilder.returns(function.returnType!!.toTypeName())
            }

            // 提取注解中的事务码 (这里简化处理，假设一定是规范注解)
            var transactionCode = -1
            if (isSuspend) {
                val asyncAnnotation = function.annotations.find { it.shortName.asString() == "IpcAsync" }
                transactionCode = asyncAnnotation?.arguments?.firstOrNull { it.name?.asString() == "requestTransaction" }?.value as? Int ?: -1
            } else if (returnType?.declaration?.qualifiedName?.asString() == "kotlinx.coroutines.flow.Flow") {
                val streamAnnotation = function.annotations.find { it.shortName.asString() == "IpcStream" }
                transactionCode = streamAnnotation?.arguments?.firstOrNull { it.name?.asString() == "subscribeTransaction" }?.value as? Int ?: -1
            } else {
                val onewayAnnotation = function.annotations.find { it.shortName.asString() == "IpcOneway" }
                transactionCode = onewayAnnotation?.arguments?.firstOrNull { it.name?.asString() == "transaction" }?.value as? Int ?: -1
            }

            // 根据方法类型生成真实调用逻辑
            if (isSuspend) {
                // 生成异步挂起调用的 Binder 通信代码
                funBuilder.beginControlFlow(
                    "return controller.pendingCallRegistry.callSuspend(serviceId = %L, operationId = %L) { requestId ->", 
                    1001, transactionCode
                )
                funBuilder.addStatement("val _data = android.os.Parcel.obtain()")
                funBuilder.addStatement("_data.writeInterfaceToken(%S)", "$packageName.$interfaceName")
                funBuilder.addStatement("_data.writeLong(requestId)")
                function.parameters.forEach { param ->
                    val paramName = param.name!!.asString()
                    val paramType = param.type.resolve().declaration.qualifiedName?.asString()
                    when (paramType) {
                        "kotlin.String" -> funBuilder.addStatement("_data.writeString(%L)", paramName)
                        "kotlin.Int" -> funBuilder.addStatement("_data.writeInt(%L)", paramName)
                        "kotlin.Long" -> funBuilder.addStatement("_data.writeLong(%L)", paramName)
                        "kotlin.Boolean" -> funBuilder.addStatement("_data.writeInt(if (%L) 1 else 0)", paramName)
                        else -> funBuilder.addStatement("_data.writeParcelable(%L, 0)", paramName)
                    }
                }
                // 我们使用全局 ResponseChannel，不需要在这里单独传 binder，或者在此处传一个
                funBuilder.addStatement("_data.writeStrongBinder(controller.globalResponseBinder)")
                funBuilder.addStatement("val _binder = controller.getServiceBinder(%L)", 1001)
                funBuilder.addStatement("_binder.transact(%L, _data, null, android.os.IBinder.FLAG_ONEWAY)", transactionCode)
                funBuilder.addStatement("_data.recycle()")
                funBuilder.endControlFlow()
            } else if (returnType?.declaration?.qualifiedName?.asString() == "kotlinx.coroutines.flow.Flow") {
                // 生成 Flow 流式订阅的 Binder 通信代码
                funBuilder.addCode(
                    """
                    |val _observer = android.os.Binder()
                    |return com.cn.ipc.client.FlowAdapterHelper.createIpcFlow(
                    |    registry = controller.subscriptionRegistry,
                    |    generation = controller.currentGeneration,
                    |    serviceId = 1001,
                    |    operationId = %L,
                    |    observerBinder = _observer,
                    |    subscribeAction = { 
                    |        val _data = android.os.Parcel.obtain()
                    |        _data.writeInterfaceToken(%S)
                    |        _data.writeStrongBinder(_observer) // 传递 observerBinder
                    |        val _reply = android.os.Parcel.obtain()
                    |        val _binder = controller.getServiceBinder(1001)
                    |        _binder.transact(%L, _data, _reply, 0) // Blocking call to get subscriptionId
                    |        val subId = _reply.readLong()
                    |        _reply.recycle()
                    |        _data.recycle()
                    |        subId
                    |    },
                    |    unsubscribeAction = { remoteId -> 
                    |        val _data = android.os.Parcel.obtain()
                    |        _data.writeInterfaceToken(%S)
                    |        _data.writeLong(remoteId)
                    |        val _binder = controller.getServiceBinder(1001)
                    |        _binder.transact(%L, _data, null, android.os.IBinder.FLAG_ONEWAY)
                    |        _data.recycle()
                    |    }
                    |)
                    |""".trimMargin(), transactionCode, packageName + "." + interfaceName, transactionCode, packageName + "." + interfaceName, transactionCode + 1
                )
            } else {
                // 生成单向通信的 Binder 通信代码
                funBuilder.addStatement("val _data = android.os.Parcel.obtain()")
                funBuilder.addStatement("_data.writeInterfaceToken(%S)", "$packageName.$interfaceName")
                function.parameters.forEach { param ->
                    val paramName = param.name!!.asString()
                    val paramType = param.type.resolve().declaration.qualifiedName?.asString()
                    when (paramType) {
                        "kotlin.String" -> funBuilder.addStatement("_data.writeString(%L)", paramName)
                        "kotlin.Int" -> funBuilder.addStatement("_data.writeInt(%L)", paramName)
                        "kotlin.Long" -> funBuilder.addStatement("_data.writeLong(%L)", paramName)
                        "kotlin.Boolean" -> funBuilder.addStatement("_data.writeInt(if (%L) 1 else 0)", paramName)
                        else -> funBuilder.addStatement("_data.writeParcelable(%L, 0)", paramName)
                    }
                }
                funBuilder.addStatement("val _binder = controller.getServiceBinder(%L)", 1001)
                funBuilder.addStatement("_binder.transact(%L, _data, null, android.os.IBinder.FLAG_ONEWAY)", transactionCode)
                funBuilder.addStatement("_data.recycle()")
            }

            typeBuilder.addFunction(funBuilder.build())
        }

        // 3. 将生成的代码写入文件
        val fileSpec = FileSpec.builder(packageName, adapterClassName)
            .addType(typeBuilder.build())
            .build()

        fileSpec.writeTo(codeGenerator, Dependencies(aggregating = false, classDeclaration.containingFile!!))
    }
}
