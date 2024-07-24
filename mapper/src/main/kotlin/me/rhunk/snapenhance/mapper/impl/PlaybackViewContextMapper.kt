package me.rhunk.snapenhance.mapper.impl

import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22c
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import me.rhunk.snapenhance.mapper.AbstractClassMapper
import me.rhunk.snapenhance.mapper.ext.getClassName

class PlaybackViewContextMapper: AbstractClassMapper("Playback View Context Mapper") {
    val componentContextClass = classReference("componentContextClass")
    val setOnPlayButtonTapedMethod = string("setOnPlayButtonTapedMethod")

    init {
        mapper {
            val playbackViewClass = getClass("Lcom/snap/voicenotes/PlaybackView;") ?: return@mapper

            val componentContextDexClass = getClass(playbackViewClass.methods.firstOrNull {
                it.name == "create" && it.parameters.size > 3
            }?.parameterTypes?.get(2)) ?: return@mapper

            componentContextClass.set(componentContextDexClass.getClassName())

            val setOnPlayButtonTapedDexMethod = componentContextDexClass.methods.firstOrNull { method ->
                method.name != "<init>" && method.implementation?.instructions?.any {
                    if (it is Instruction22c && it.reference is FieldReference) {
                        (it.reference as FieldReference).name == "_onPlayButtonTapped"
                    } else false
                } == true
            } ?: return@mapper

            setOnPlayButtonTapedMethod.set(setOnPlayButtonTapedDexMethod.name)
        }
    }
}