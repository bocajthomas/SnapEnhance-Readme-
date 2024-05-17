package me.rhunk.snapenhance.mapper.impl

import me.rhunk.snapenhance.mapper.AbstractClassMapper
import me.rhunk.snapenhance.mapper.ext.findConstString
import me.rhunk.snapenhance.mapper.ext.getClassName
import me.rhunk.snapenhance.mapper.ext.isAbstract
import me.rhunk.snapenhance.mapper.ext.isEnum
import java.lang.reflect.Modifier

class FriendRelationshipChangerMapper : AbstractClassMapper("FriendRelationshipChanger") {
    val classReference = classReference("class")

    val friendshipRelationshipChangerKtx = classReference("removeFriendClass")
    val addFriendMethod = string("addFriendMethod")
    val runFriendDurableJob = string("runFriendDurableJob")

    init {
        mapper {
            for (classDef in classes) {
                classDef.methods.firstOrNull { it.name == "<init>" }?.implementation?.findConstString("FriendRelationshipChangerImpl")?.takeIf { it } ?: continue
                classReference.set(classDef.getClassName())

                runFriendDurableJob.set(classDef.methods.firstOrNull {
                    Modifier.isStatic(it.accessFlags) &&
                            it.returnType.contains("CompletableAndThenCompletable") &&
                            it.parameterTypes.size == 5 &&
                            it.parameterTypes[0] == classDef.type &&
                            it.parameterTypes[1] == "Ljava/lang/String;" &&
                            it.parameterTypes[3] == "I" &&
                            it.parameterTypes[4] == "Ljava/lang/String;"
                }?.name ?: continue)
            }
        }
        mapper {
            for (classDef in classes) {
                if (!classDef.isAbstract()) continue
                val addFriendDexMethod = classDef.methods.firstOrNull {
                    Modifier.isStatic(it.accessFlags) &&
                    it.parameterTypes.size == 6 &&
                    it.parameterTypes[1] == "Ljava/lang/String;" &&
                    getClass(it.parameterTypes[2])?.isEnum() == true &&
                    getClass(it.parameterTypes[4])?.isEnum() == true &&
                    it.parameterTypes[5] == "I"
                } ?: continue

                friendshipRelationshipChangerKtx.set(classDef.getClassName())
                addFriendMethod.set(addFriendDexMethod.name)
                return@mapper
            }
        }
    }
}