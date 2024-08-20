package me.rhunk.snapenhance.common.scripting.impl

import me.rhunk.snapenhance.common.scripting.bindings.AbstractBinding
import me.rhunk.snapenhance.common.scripting.bindings.BindingSide
import org.mozilla.javascript.annotations.JSFunction

typealias Listener = (List<String?>) -> Unit

abstract class IPCInterface : AbstractBinding("ipc", BindingSide.COMMON) {
    abstract fun on(eventName: String, listener: Listener)

    abstract fun onBroadcast(channel: String, eventName: String, listener: Listener)

    abstract fun emit(eventName: String, vararg args: String?): Int
    abstract fun broadcast(channel: String, eventName: String, vararg args: String?): Int

    @Suppress("unused")
    fun emit(eventName: String) = emit(eventName, *emptyArray())
    @Suppress("unused")
    fun broadcast(channel: String, eventName: String) =
        broadcast(channel, eventName, *emptyArray())

    override fun getObject() = this

    @JSFunction("isBridgeAlive")
    fun isBridgeAlive() = context.runtime.scripting.asBinder().pingBinder()
}