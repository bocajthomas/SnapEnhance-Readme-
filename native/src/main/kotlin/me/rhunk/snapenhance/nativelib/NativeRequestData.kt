package me.rhunk.snapenhance.nativelib

class NativeRequestData(
    val uri: String,
    var buffer: ByteArray,
    var canceled: Boolean = false,
)