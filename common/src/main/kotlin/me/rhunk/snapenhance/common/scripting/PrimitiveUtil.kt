package me.rhunk.snapenhance.common.scripting

fun Any?.toPrimitiveValue(type: Lazy<String>) = when (this) {
    is Number -> when (type.value) {
        "byte" -> this.toByte()
        "short" -> this.toShort()
        "int" -> this.toInt()
        "long" -> this.toLong()
        "float" -> this.toFloat()
        "double" -> this.toDouble()
        "boolean" -> this.toByte() != 0.toByte()
        "char" -> this.toInt().toChar()
        else -> this
    }
    is Boolean -> if (type.value == "boolean") this.toString().toBoolean() else this
    else -> this
}

fun Array<out Any?>.isSameParameters(parameters: Array<Class<*>>): Boolean {
    if (this.size != parameters.size) return false
    for (i in this.indices) {
        val type = parameters[i]
        val value = this[i]?.toPrimitiveValue(lazy { type.name }) ?: continue
        if (type.isPrimitive) {
            when (type.name) {
                "byte" -> if (value !is Byte) return false
                "short" -> if (value !is Short) return false
                "int" -> if (value !is Int) return false
                "long" -> if (value !is Long) return false
                "float" -> if (value !is Float) return false
                "double" -> if (value !is Double) return false
                "boolean" -> if (value !is Boolean) return false
                "char" -> if (value !is Char) return false
                else -> return false
            }
        } else if (!type.isAssignableFrom(value.javaClass)) return false
    }
    return true
}