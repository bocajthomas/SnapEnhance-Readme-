package me.rhunk.snapenhance.core.features.impl.tweaks

import android.view.View
import me.rhunk.snapenhance.core.event.events.impl.BindViewEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.util.dataBuilder
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.ktx.getObjectField
import me.rhunk.snapenhance.mapper.impl.PlaybackViewContextMapper
import java.lang.reflect.Proxy
import java.util.WeakHashMap

class VoiceNoteAutoPlay: Feature("Voice Note Auto Play") {
    override fun init() {
        if (!context.config.experimental.voiceNoteAutoPlay.get()) return

        val views = WeakHashMap<View, Any>() // component context -> view

        fun setPlaybackState(componentContext: Any, state: String): Boolean {
            val onPlayButtonTapped = componentContext.getObjectField("_onPlayButtonTapped") ?: return false
            onPlayButtonTapped.javaClass.getMethod("invoke", Any::class.java).invoke(
                onPlayButtonTapped,
                findClass("com.snap.voicenotes.PlaybackState").enumConstants.first {
                    it.toString() == state
                }
            )
            return true
        }

        fun playNextVoiceNote(currentContext: Any): Boolean {
            val currentContextView = views.entries.filter { it.key.isAttachedToWindow }.firstOrNull { it.value.hashCode() == currentContext.hashCode() }?.key ?: return false

            val currentViewLocation = IntArray(2)
            currentContextView.getLocationOnScreen(currentViewLocation)

            context.log.verbose("currentView -> $currentContextView")

            // find a view under the current view
            val nextView = views.entries.filter { it.key.isAttachedToWindow }.map { entry ->
                entry to IntArray(2).let {
                    entry.key.getLocationOnScreen(it)
                    it[1] - currentViewLocation[1]
                }
            }.filter { it.second > 1 }.minByOrNull { it.second }?.first

            if (nextView == null) {
                context.log.verbose("No more voice notes to play")
                return false
            }

            context.log.verbose("nextView -> ${nextView.key}")

            return setPlaybackState(nextView.value, "PLAYING")
        }

        context.mappings.useMapper(PlaybackViewContextMapper::class) {
            componentContextClass.getAsClass()?.hook(setOnPlayButtonTapedMethod.get() ?: return@useMapper, HookStage.AFTER) { param ->
                val instance = param.thisObject<Any>()
                var lastPlayerState: String? = null

                instance.dataBuilder {
                    val onPlayButtonTapped = get("_onPlayButtonTapped") as? Any ?: return@dataBuilder

                    set("_onPlayButtonTapped", Proxy.newProxyInstance(
                        context.androidContext.classLoader,
                        arrayOf(findClass("kotlin.jvm.functions.Function1"))
                    ) { _, _, args ->
                        lastPlayerState = null
                        context.log.verbose("onPlayButtonTapped ${args.contentToString()}")
                        onPlayButtonTapped.javaClass.getMethod("invoke", Any::class.java).invoke(onPlayButtonTapped, args[0])
                    })

                    from("_playbackStateObservable") {
                        val oldSubscribe = get("_subscribe") as? Any ?: return@from

                        fun subscribe(listener: Any): Any? {
                            return oldSubscribe.javaClass.getMethod("invoke", Any::class.java).invoke(oldSubscribe, listener)
                        }

                        set("_subscribe", Proxy.newProxyInstance(
                            context.androidContext.classLoader,
                            arrayOf(findClass("kotlin.jvm.functions.Function1"))
                        ) proxy@{ _, _, args ->
                            val function4 = args[0]

                            subscribe(
                                Proxy.newProxyInstance(
                                    context.androidContext.classLoader,
                                    arrayOf(findClass("kotlin.jvm.functions.Function4"))
                                ) { _, _, listenerArgs ->
                                    val state = listenerArgs[2]?.toString()

                                    if (state == "PAUSED" && lastPlayerState == "PLAYING") {
                                        context.log.verbose("playback finished. playing next voice note")
                                        runCatching {
                                            if (!playNextVoiceNote(instance)) {
                                                lastPlayerState = null
                                            }
                                        }.onFailure {
                                            context.log.error("Failed to play next voice note", it)
                                        }
                                    }

                                    lastPlayerState = state
                                    function4.javaClass.methods.first { it.parameterCount == 4 }.invoke(function4, *listenerArgs)
                                }
                            )

                        })
                    }
                }
            }
        }

        onNextActivityCreate {
            context.event.subscribe(BindViewEvent::class) { event ->
                if (!event.prevModel.toString().contains("audio_note")) return@subscribe

                // find view model of the audio note
                val viewModelField = event.prevModel.javaClass.fields.firstOrNull { field ->
                    field.type.constructors.firstOrNull()?.parameterTypes?.takeIf { it.size == 3 }?.let { args ->
                        args[1].interfaces.any { it.name == "com.snap.composer.utils.ComposerMarshallable" }
                    } == true
                } ?: return@subscribe

                val viewModel = viewModelField.get(event.prevModel)
                var playbackViewComponentContext: Any? = null

                for (field in viewModel.javaClass.fields) {
                    val fieldContent = runCatching { field.get(viewModel) }.getOrNull() ?: continue
                    if (fieldContent.javaClass.declaredFields.any { it.name == "_onPlayButtonTapped" }) {
                        playbackViewComponentContext = fieldContent
                        break;
                    }
                }

                views[event.view] = playbackViewComponentContext ?: return@subscribe
            }
        }
    }
}