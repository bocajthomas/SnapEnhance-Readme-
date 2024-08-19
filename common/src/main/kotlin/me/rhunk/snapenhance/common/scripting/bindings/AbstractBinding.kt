package me.rhunk.snapenhance.common.scripting.bindings

abstract class AbstractBinding(
    val name: String,
    val side: BindingSide
) {
    lateinit var context: BindingsContext

    private val bridgeReloadList = mutableListOf<() -> Unit>()

    fun bridgeAutoReload(block: () -> Unit) {
        bridgeReloadList += block
        block()
    }

    open fun onInit() {}

    open fun onBridgeReloaded() {
        bridgeReloadList.forEach { it() }
    }

    open fun onDispose() {}

    abstract fun getObject(): Any
}