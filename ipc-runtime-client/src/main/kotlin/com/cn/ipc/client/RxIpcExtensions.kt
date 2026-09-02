package com.cn.ipc.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
// import io.reactivex.rxjava3.core.Observable
// import io.reactivex.rxjava3.core.Single
// import kotlinx.coroutines.rx3.asObservable
// import kotlinx.coroutines.rx3.rxSingle

/**
 * M4: 存量 RxJava 兼容层
 *
 * 为那些还未能迁移到 Kotlin Coroutines/Flow，仍在使用 RxJava 的老旧模块，
 * 提供的无缝转换包装。
 *
 * 注：实际生产中需要依赖 kotlinx-coroutines-rx3 库，这里为了 POC 仅展示接口转换设计。
 */
object RxIpcExtensions {

    /**
     * 将挂起函数 (suspend) 包装为 RxJava 的 Single。
     */
    /*
    fun <T : Any> wrapAsSingle(
        scope: CoroutineScope,
        block: suspend () -> T
    ): Single<T> {
        return scope.rxSingle { block() }
    }
    */

    /**
     * 将跨进程订阅的 Flow 包装为 RxJava 的 Observable。
     */
    /*
    fun <T : Any> Flow<T>.asRxObservable(): Observable<T> {
        return this.asObservable()
    }
    */
}
