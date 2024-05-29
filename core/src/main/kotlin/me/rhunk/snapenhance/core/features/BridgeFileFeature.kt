package me.rhunk.snapenhance.core.features

import me.rhunk.snapenhance.common.bridge.FileHandleScope
import me.rhunk.snapenhance.common.bridge.InternalFileHandleType
import me.rhunk.snapenhance.common.bridge.toWrapper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

abstract class BridgeFileFeature(name: String, private val bridgeFileType: InternalFileHandleType, loadParams: Int) : Feature(name, loadParams) {
    private val fileLines = mutableListOf<String>()
    private val fileWrapper by lazy { context.bridgeClient.getFileHandlerManager().getFileHandle(FileHandleScope.INTERNAL.key, bridgeFileType.key)!!.toWrapper() }

    protected fun readFile() {
        val temporaryLines = mutableListOf<String>()
        fileWrapper.inputStream { stream ->
            with(BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8))) {
                var line = ""
                while (readLine()?.also { line = it } != null) temporaryLines.add(line)
                close()
            }
        }

        fileLines.clear()
        fileLines.addAll(temporaryLines)
    }

    private fun updateFile() {
        fileWrapper.outputStream { stream ->
            fileLines.forEach {
                stream.write(it.toByteArray())
                stream.write("\n".toByteArray())
                stream.flush()
            }
        }
    }

    protected fun exists(line: String) = fileLines.contains(line)

    protected fun toggle(line: String) {
        if (exists(line)) fileLines.remove(line) else fileLines.add(line)
        updateFile()
    }

    protected fun setState(line: String, state: Boolean) {
        if (state) {
            if (!exists(line)) fileLines.add(line)
        } else {
            if (exists(line)) fileLines.remove(line)
        }
        updateFile()
    }

    protected fun reload() = readFile()

    protected fun put(line: String) {
        fileLines.add(line)
        updateFile()
    }

    protected fun clear() {
        fileLines.clear()
        updateFile()
    }

    protected fun lines() = fileLines.toList()
}