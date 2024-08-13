package me.rhunk.snapenhance.core.wrapper.impl.composer

import me.rhunk.snapenhance.core.wrapper.AbstractWrapper
import java.lang.ref.WeakReference

class ComposerContext(obj: Any): AbstractWrapper(obj) {
    val componentPath by field<String>("componentPath")
    val viewModel by field<Any?>("innerViewModel")
    val moduleName by field<String>("moduleName")
    val componentContext by field<WeakReference<Any?>>("componentContext")
}