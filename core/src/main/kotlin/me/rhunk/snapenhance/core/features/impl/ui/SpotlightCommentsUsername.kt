package me.rhunk.snapenhance.core.features.impl.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rhunk.snapenhance.core.event.events.impl.BindViewEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.impl.messaging.Messaging
import me.rhunk.snapenhance.core.ui.children
import me.rhunk.snapenhance.core.util.EvictingMap

class SpotlightCommentsUsername : Feature("SpotlightCommentsUsername") {
    private val usernameCache = EvictingMap<String, String>(150)

    @SuppressLint("SetTextI18n")
    override fun init() {
        if (!context.config.global.spotlightCommentsUsername.get()) return

        onNextActivityCreate(defer = true) {
            val messaging = context.feature(Messaging::class)
            context.event.subscribe(BindViewEvent::class) { event ->
                val posterUserId = event.prevModel.toString().takeIf { it.startsWith("Comment") }
                    ?.substringAfter("posterUserId=")?.substringBefore(",")?.substringBefore(")") ?: return@subscribe

                if (posterUserId == "null") return@subscribe

                fun setUsername(username: String) {
                    usernameCache[posterUserId] = username
                    val commentsCreatorBadgeTimestamp = (event.view as ViewGroup).children().filterIsInstance<TextView>()
                        .getOrNull(1) ?: return
                    if (commentsCreatorBadgeTimestamp.text.contains(username)) return
                    commentsCreatorBadgeTimestamp.text = " (${username})" + commentsCreatorBadgeTimestamp?.text.toString()
                }

                event.view.post {
                    usernameCache[posterUserId]?.let {
                        setUsername(it)
                        return@post
                    }

                    context.coroutineScope.launch {
                        val username = runCatching {
                            messaging.fetchSnapchatterInfos(listOf(posterUserId)).firstOrNull()
                        }.onFailure {
                            context.log.error("Failed to fetch snapchatter info for user $posterUserId", it)
                        }.getOrNull()?.username ?: return@launch

                        withContext(Dispatchers.Main) {
                            setUsername(username)
                        }
                    }
                }
            }
        }
    }
}