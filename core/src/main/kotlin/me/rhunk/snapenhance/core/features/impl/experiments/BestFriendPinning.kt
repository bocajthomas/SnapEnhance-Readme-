package me.rhunk.snapenhance.core.features.impl.experiments

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.common.bridge.InternalFileHandleType
import me.rhunk.snapenhance.common.util.protobuf.ProtoReader
import me.rhunk.snapenhance.core.event.events.impl.NetworkApiRequestEvent
import me.rhunk.snapenhance.core.event.events.impl.UnaryCallEvent
import me.rhunk.snapenhance.core.features.BridgeFileFeature
import me.rhunk.snapenhance.core.features.FeatureLoadParams
import me.rhunk.snapenhance.core.ui.triggerRootCloseTouchEvent
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.util.UUID

class BestFriendPinning: BridgeFileFeature("Best Friend Pinning", InternalFileHandleType.PINNED_BEST_FRIEND, loadParams = FeatureLoadParams.INIT_SYNC) {
    private fun updatePinnedBestFriendStatus() {
        lines().firstOrNull()?.trim()?.let {
            context.database.updatePinnedBestFriendStatus(it.substring(0, 36), "number_one_bf_for_two_months")
        }
    }

    override fun init() {
        if (!context.config.experimental.bestFriendPinning.get()) return
        reload()

        context.event.subscribe(UnaryCallEvent::class) { event ->
            if (!event.uri.endsWith("/PinBestFriend") && !event.uri.endsWith("/UnpinBestFriend")) return@subscribe
            event.canceled = true
            val userId = ProtoReader(event.buffer).let {
                UUID(it.getFixed64(1, 1) ?: return@subscribe, it.getFixed64(1, 2)?: return@subscribe).toString()
            }

            clear()
            put(userId)

            updatePinnedBestFriendStatus()

            val username = context.database.getFriendInfo(userId)?.mutableUsername ?: "Unknown"

            context.inAppOverlay.showStatusToast(
                icon = Icons.Default.FavoriteBorder,
                "Pinned $username as best friend! Please restart the app to apply changes.",
                durationMs = 5000
            )

            context.coroutineScope.launch(Dispatchers.Main) {
                delay(500)
                @Suppress("DEPRECATION")
                context.mainActivity!!.onBackPressed()
                context.mainActivity!!.triggerRootCloseTouchEvent()
            }
        }

        context.event.subscribe(NetworkApiRequestEvent::class) { event ->
            if (!event.url.contains("ami/friends")) return@subscribe
            val pinnedBFF = lines().firstOrNull()?.trim() ?: return@subscribe

            event.onSuccess { buffer ->
                val jsonObject = context.gson.fromJson(
                    InputStreamReader(buffer?.inputStream() ?: return@onSuccess, Charsets.UTF_8),
                    JsonObject::class.java
                ).apply {
                    getAsJsonArray("friends").map { it.asJsonObject }.forEach { friend ->
                        if (friend.get("user_id").asString != pinnedBFF) return@forEach
                        friend.add("friendmojis", JsonArray().apply {
                            friend.getAsJsonArray("friendmojis").map { it.asJsonObject }.forEach { friendmoji ->
                                val category = friendmoji.get("category_name").asString
                                if (category == "on_fire" || category == "birthday") {
                                    add(friendmoji)
                                }
                            }
                            add(JsonObject().apply {
                                addProperty("category_name", "number_one_bf_for_two_months")
                            })
                        })
                    }
                }

                jsonObject.toString().toByteArray(Charsets.UTF_8).let {
                    setArg(2, ByteBuffer.allocateDirect(it.size).apply {
                        put(it)
                        flip()
                    })
                }
            }
        }
    }
}