package me.rhunk.snapenhance.core.features.impl.experiments

import android.location.Location
import android.location.LocationManager
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.rhunk.snapenhance.common.ui.OverlayType
import me.rhunk.snapenhance.common.ui.createComposeView
import me.rhunk.snapenhance.common.util.protobuf.EditorContext
import me.rhunk.snapenhance.common.util.protobuf.ProtoEditor
import me.rhunk.snapenhance.common.util.protobuf.ProtoReader
import me.rhunk.snapenhance.core.event.events.impl.AddViewEvent
import me.rhunk.snapenhance.core.event.events.impl.UnaryCallEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.ui.children
import me.rhunk.snapenhance.core.util.RandomWalking
import me.rhunk.snapenhance.core.util.dataBuilder
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import me.rhunk.snapenhance.core.util.hook.hookConstructor
import me.rhunk.snapenhance.core.util.ktx.getId
import me.rhunk.snapenhance.core.util.ktx.getObjectField
import me.rhunk.snapenhance.core.util.ktx.isDarkTheme
import me.rhunk.snapenhance.mapper.impl.CallbackMapper
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class FriendLocation(
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val lastUpdated: Long,
    val locality: String?,
    val localityPieces: List<String>,
    val batteryLevel: Float,
) {
    fun distanceTo(other: FriendLocation): Double {
        val deltaLat = Math.toRadians(other.latitude - this.latitude)
        val deltaLong = Math.toRadians(other.longitude - this.longitude)

        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(Math.toRadians(this.latitude)) * cos(Math.toRadians(other.latitude)) *
                sin(deltaLong / 2) * sin(deltaLong / 2)

        return 6371 * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}

class BetterLocation : Feature("Better Location") {
    val locationHistory = mutableMapOf<String, FriendLocation>()

    private val walkRadius by lazy {
        context.config.global.betterLocation.walkRadius.getNullable()
    }

    private val randomWalking by lazy {
        RandomWalking(walkRadius?.toDoubleOrNull())
    }

    private fun getLat() : Double {
        var spoofedLatitude = context.config.global.betterLocation.coordinates.get().first
        walkRadius?.let {
            spoofedLatitude += randomWalking.current_x
        }
        return spoofedLatitude
    }

    private fun getLong() : Double {
        var spoofedLongitude = context.config.global.betterLocation.coordinates.get().second
        walkRadius?.let {
            spoofedLongitude += randomWalking.current_y
        }
        return spoofedLongitude
    }

    private fun editClientUpdate(editor: EditorContext) {
        val config = context.config.global.betterLocation

        editor.apply {
            // SCVSLocationUpdate
            edit(1) {
                if (config.spoofLocation.get()) {
                    randomWalking.updatePosition()
                    remove(1)
                    remove(2)
                    addFixed32(1, getLat().toFloat()) // lat
                    addFixed32(2, getLong().toFloat()) // lng
                }

                if (config.alwaysUpdateLocation.get()) {
                    remove(7)
                    addVarInt(7, System.currentTimeMillis()) // timestamp
                }
            }

            if (context.config.global.betterLocation.suspendLocationUpdates.get()) {
                remove(1)
            }

            // SCVSDeviceData
            edit(3) {
                config.spoofBatteryLevel.getNullable()?.takeIf { it.isNotEmpty() }?.let {
                    val value = it.toIntOrNull()?.toFloat()?.div(100) ?: return@edit
                    remove(2)
                    addFixed32(2, value)
                    if (value == 100F) {
                        remove(3)
                        addVarInt(3, 1) // devicePluggedIn
                    }
                }

                if (config.spoofHeadphones.get()) {
                    remove(4)
                    addVarInt(4, 1) // headphoneOutput
                    remove(6)
                    addVarInt(6, 1) // isOtherAudioPlaying
                }

                edit(10) {
                    remove(1)
                    addVarInt(1, 4) // type = ALWAYS
                    remove(2)
                    addVarInt(2, 1) // precise = true
                }
            }
        }
    }

    private fun onLocationEvent(protoReader: ProtoReader) {
        protoReader.eachBuffer(3, 1) {
            val clusterId = UUID(getFixed64(1, 1) ?: return@eachBuffer, getFixed64(1, 2) ?: return@eachBuffer).toString()

            val latitude = getFixed32(4)?.let { Float.fromBits(it) }?.toDouble() ?: return@eachBuffer
            val longitude = getFixed32(5)?.let { Float.fromBits(it) }?.toDouble() ?: return@eachBuffer

            val locality = getString(10)
            val localityPieces = mutableListOf<String>().also {
                forEach { index, wire ->
                    if (index != 11) return@forEach
                    it.add((wire.value as ByteArray).toString(Charsets.UTF_8) )
                }
            }

            eachBuffer(7) friend@{
                val userId = if (contains(1)) UUID(getFixed64(1, 1) ?: return@friend, getFixed64(1, 2) ?: return@friend).toString() else clusterId
                val friendLocation = FriendLocation(
                    userId = userId,
                    latitude = latitude,
                    longitude = longitude,
                    lastUpdated = getVarInt(2) ?: -1L,
                    locality = locality,
                    localityPieces = localityPieces,
                    batteryLevel = getFixed32(13)?.let { Float.fromBits(it) } ?: -1F,
                )

                locationHistory[userId] = friendLocation
            }
        }
    }

    private fun openManagementOverlay() {
        context.bridgeClient.getLocationManager().provideFriendsLocation(
            locationHistory.values.toList().mapNotNull { locationHistory ->
                val friendInfo = context.database.getFriendInfo(locationHistory.userId) ?: return@mapNotNull null

                me.rhunk.snapenhance.bridge.location.FriendLocation().also {
                    it.username = friendInfo.mutableUsername ?: return@mapNotNull null
                    it.displayName = friendInfo.displayName
                    it.bitmojiId = friendInfo.bitmojiAvatarId
                    it.bitmojiSelfieId = friendInfo.bitmojiSelfieId
                    it.latitude = locationHistory.latitude
                    it.longitude = locationHistory.longitude
                    it.lastUpdated = locationHistory.lastUpdated
                    it.locality = locationHistory.locality
                    it.localityPieces = locationHistory.localityPieces
                }
            }
        )
        context.bridgeClient.openOverlay(OverlayType.BETTER_LOCATION)
    }

    override fun init() {
        if (context.config.global.betterLocation.globalState != true) return

        val canSpoofLocation = { context.config.global.betterLocation.spoofLocation.get() }

        LocationManager::class.java.apply {
            hook("isProviderEnabled", HookStage.BEFORE, { canSpoofLocation() }) { it.setResult(true) }
            hook("isProviderEnabledForUser", HookStage.BEFORE, { canSpoofLocation() }) { it.setResult(true) }
        }
        Location::class.java.apply {
            hook("getLatitude", HookStage.BEFORE, { canSpoofLocation() }) { it.setResult(getLat()) }
            hook("getLongitude", HookStage.BEFORE, { canSpoofLocation() }) { it.setResult(getLong()) }
        }

        val mapViewId = context.resources.getId("mapview")

        if (context.config.global.betterLocation.showBatteryLevel.get()) {
            findClass("snap.snap_maps_sdk.nano.SnapMapsSdk\$PublicUserInfo").hook("setDisplayName", HookStage.BEFORE) { param ->
                val instance = param.thisObject<Any>()
                val userId = instance.getObjectField("userId_") as? String ?: return@hook
                val batteryLevel = locationHistory[userId]?.batteryLevel?.takeIf { it > -1F } ?: return@hook
                param.setArg(0, param.arg<String>(0) + " (${(batteryLevel * 100).toInt()}%)")
            }

            findClass("com.snap.map_friend_focus_view.MapFocusViewFriendSectionDataModel").hookConstructor(HookStage.AFTER) { param ->
                val instance = param.thisObject<Any>()
                val userId = instance.getObjectField("_userId") as? String ?: return@hookConstructor
                val batteryLevel = locationHistory[userId]?.batteryLevel?.takeIf { it > -1F } ?: return@hookConstructor

                param.thisObject<Any>().dataBuilder {
                    val prevText = get<String?>("_lastSeen")?.let { " - $it" } ?: ""
                    set("_lastSeen", "(${(batteryLevel * 100).toInt()}%)$prevText")
                }
            }
        }

        context.event.subscribe(AddViewEvent::class) { event ->
            if (!event.viewClassName.endsWith("MapScreenRoot")) return@subscribe

            event.view.addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    val mapView = event.view.findViewById<View>(mapViewId) ?: throw IllegalStateException("Map view not found")
                    val view = (mapView.parent as ViewGroup).children().firstOrNull { it is RelativeLayout } as? RelativeLayout ?: throw IllegalStateException("Map view parent not found")

                    view.addView(createComposeView(view.context) {
                        val darkTheme = remember { context.androidContext.isDarkTheme() }
                        Box(
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            FilledIconButton(
                                modifier = Modifier.size(40.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = if (darkTheme) Color(0xFF1D1D1D) else Color.White,
                                    contentColor = if (darkTheme) Color.White else Color(0xFF151A1A),
                                ),
                                onClick = { openManagementOverlay() }
                            ) {
                                Icon(Icons.Default.EditLocation, contentDescription = null)
                            }
                        }
                    }.apply {
                        layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                            addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                            setMargins(0, (60 * context.resources.displayMetrics.density).toInt(), 0, 0)
                        }
                    })
                }

                override fun onViewDetachedFromWindow(v: View) {}
            })
        }

        context.event.subscribe(UnaryCallEvent::class) { event ->
            if (event.uri == "/snapchat.valis.Valis/SendClientUpdate") {
                event.buffer = ProtoEditor(event.buffer).apply {
                    edit {
                        editEach(1) {
                            editClientUpdate(this)
                        }
                    }
                }.toByteArray()
            }
        }

        context.mappings.useMapper(CallbackMapper::class) {
            callbacks.getClass("ServerStreamingEventHandler")?.hook("onEvent", HookStage.BEFORE) { param ->
                val buffer = param.argNullable<ByteBuffer>(1)?.let {
                    it.position(0)
                    ByteArray(it.capacity()).also { buffer -> it.get(buffer); it.position(0) }
                } ?: return@hook
                onLocationEvent(ProtoReader(buffer))
            }
        }

        findClass("com.snapchat.client.grpc.ClientStreamSendHandler\$CppProxy").hook("send", HookStage.BEFORE) { param ->
            val array = param.arg<ByteBuffer>(0).let {
                it.position(0)
                ByteArray(it.capacity()).also { buffer -> it.get(buffer); it.position(0) }
            }

            param.setArg(0, ProtoEditor(array).apply {
                edit {
                    editClientUpdate(this)
                }
            }.toByteArray().let {
                ByteBuffer.allocateDirect(it.size).put(it).rewind()
            })
        }
    }
}