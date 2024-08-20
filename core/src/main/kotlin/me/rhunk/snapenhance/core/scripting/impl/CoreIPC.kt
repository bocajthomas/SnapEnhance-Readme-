package me.rhunk.snapenhance.core.scripting.impl

import me.rhunk.snapenhance.bridge.scripting.IPCListener
import me.rhunk.snapenhance.common.scripting.impl.IPCInterface
import me.rhunk.snapenhance.common.scripting.impl.Listener

class CoreIPC : IPCInterface() {
    override fun onBroadcast(channel: String, eventName: String, listener: Listener) {
        bridgeAutoReload {
            context.runtime.scripting.registerIPCListener(channel, eventName, object: IPCListener.Stub() {
                override fun onMessage(args: Array<out String?>) {
                    listener(args.toList())
                }
            })
        }
    }

    override fun on(eventName: String, listener: Listener) {
        onBroadcast(context.moduleInfo.name, eventName, listener)
    }

    override fun emit(eventName: String, vararg args: String?): Int {
        return broadcast(context.moduleInfo.name, eventName, *args)
    }

    override fun broadcast(channel: String, eventName: String, vararg args: String?): Int {
        return runCatching { context.runtime.scripting.sendIPCMessage(channel, eventName, args) }.getOrNull() ?: 0
    }
}