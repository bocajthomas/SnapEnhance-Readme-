package me.rhunk.snapenhance.core.wrapper.impl.composer

import me.rhunk.snapenhance.core.wrapper.AbstractWrapper

class ComposerMarshaller(obj: Any): AbstractWrapper(obj) {
    private val getUntypedMethod by lazy { instanceNonNull().javaClass.methods.first { it.name == "getUntyped" } }
    private val getSizeMethod by lazy { instanceNonNull().javaClass.methods.first { it.name == "getSize" } }
    private val pushUntypedMethod by lazy { instanceNonNull().javaClass.methods.first { it.name == "pushUntyped" } }

    fun getUntyped(index: Int): Any? = getUntypedMethod.invoke(instanceNonNull(), index)
    fun getSize() = getSizeMethod.invoke(instanceNonNull()) as Int
    fun pushUntyped(value: Any?): Any? = pushUntypedMethod.invoke(instanceNonNull(), value)
}