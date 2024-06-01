package me.rhunk.snapenhance.common.util

import android.os.IInterface


open class LazyBridgeValue<T: IInterface>(
    private val block: () -> T,
    private val isConstant: Boolean = false
): Lazy<T> {
    private val lock = Any()
    private var _value: T? = null

    override val value: T get() = run {
        synchronized(lock) {
            if (_value == null || (!isConstant && !_value!!.asBinder().pingBinder())) {
                _value = block()
            }
        }
        return _value!!
    }

    override fun isInitialized(): Boolean {
        return _value != null && (isConstant || _value!!.asBinder().pingBinder())
    }

    operator fun getValue(thisRef: Any?, property: Any?): T {
        return value
    }
}


fun <T : IInterface, R> mappedLazyBridge(lazyBridgeValue: LazyBridgeValue<T>, map: (T) -> R): Lazy<R> {
    return object : Lazy<R> {
        private var _value: T? = null
        private var _mappedValue: R? = null

        override val value: R get() = run {
            if (_value != lazyBridgeValue.value) {
                _value = lazyBridgeValue.value
                _mappedValue = map(_value!!)
            }
            return _mappedValue!!
        }
        override fun isInitialized(): Boolean = lazyBridgeValue.isInitialized()
    }
}

fun <T: IInterface> lazyBridge(block: () -> T) = LazyBridgeValue(block)
fun <T: IInterface> constantLazyBridge(value: () -> T) = LazyBridgeValue(value, isConstant = true)
