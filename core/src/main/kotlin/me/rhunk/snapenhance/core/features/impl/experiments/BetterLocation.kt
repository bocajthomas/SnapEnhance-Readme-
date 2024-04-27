package me.rhunk.snapenhance.core.features.impl.experiments

import android.location.Location
import android.location.LocationManager
import me.rhunk.snapenhance.common.util.protobuf.EditorContext
import me.rhunk.snapenhance.common.util.protobuf.ProtoEditor
import me.rhunk.snapenhance.core.event.events.impl.UnaryCallEvent
import me.rhunk.snapenhance.core.features.Feature
import me.rhunk.snapenhance.core.features.FeatureLoadParams
import me.rhunk.snapenhance.core.features.impl.global.SuspendLocationUpdates
import me.rhunk.snapenhance.core.util.hook.HookStage
import me.rhunk.snapenhance.core.util.hook.hook
import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.days

class BetterLocation : Feature("Better Location", loadParams = FeatureLoadParams.INIT_SYNC) {
    //Latitude ft / deg /Longitude ft /deg = 1.26301179736
    // 4ft/s * 1 degree/364000ft (latitude) * 1s/1000ms = .000000010989011 degrees/ms
    val max_speed = 4.0 / 364000.0 / 1000.0
    val pause_chance = .0023 // .23% chance to pause every second = after 5 minutes 50% chance of pause
    val pause_duration = 60000L //ms
    val pause_random = 30000L //ms

    var pause_expire = 0L
    var current_x = 0.0
    var current_y = 0.0
    var target_x = 0.0
    var target_y = 0.0
    var last_update_time = 0L
    private fun updatePosition(){
        val now = System.currentTimeMillis()

        if(current_x == target_x && current_y == target_y) {
            val config = context.config.global.betterLocation
            val walk_rad = if (config.walkRadius.get()
                    .toDoubleOrNull() == null
            ) 0.0 else (config.walkRadius.get().toDouble() / 364000.0) //Lat deg

            if(last_update_time == 0L){ //Start at random position
                val radius1 = sqrt(Math.random()) * walk_rad
                val theta1 = Math.PI * 2.0 * Math.random()
                current_x = cos(theta1) * radius1 * 1.26301179736
                current_y = sin(theta1) * radius1
            }

            val radius2 = sqrt(Math.random()) * walk_rad
            val theta2 = Math.PI * 2.0 * Math.random()
            target_x = cos(theta2) * radius2 * 1.26301179736
            target_y = sin(theta2) * radius2
        } else if (pause_expire < now) {
            val deltat = now - last_update_time
            if(Math.random() > (1.0 - pause_chance).pow(deltat / 1000.0)){
                pause_expire = now + pause_duration + (pause_random * Math.random()).toLong()
            } else {
                val max_dist = max_speed * deltat
                val dist = hypot(target_x - current_x, target_y - current_y)

                if (dist <= max_dist) {
                    current_x = target_x
                    current_y = target_y
                } else {
                    val norm_x = (target_x - current_x) / dist * max_dist
                    val norm_y = (target_y - current_y) / dist * max_dist
                    current_x += norm_x
                    current_y += norm_y
                }
            }
        }
        last_update_time = now
    }

    private fun getLat() : Double {
        updatePosition()
        return (context.config.global.betterLocation.coordinates.get().first + current_x)
    }

    private fun getLong() : Double {
        updatePosition()
        return (context.config.global.betterLocation.coordinates.get().second + current_y)
    }
    private fun editClientUpdate(editor: EditorContext) {
        val config = context.config.global.betterLocation

        editor.apply {
            // SCVSLocationUpdate
            edit(1) {
                context.log.verbose("SCVSLocationUpdate ${this@apply}")
                if (config.spoofLocation.get()) {
                    remove(1)
                    remove(2)
                    addFixed32(1, getLat().toFloat()) // lat
                    addFixed32(2, getLong().toFloat()) // lng
                }

                if (config.alwaysUpdateLocation.get()) {
                    remove(7)
                    addVarInt(7, System.currentTimeMillis()) // timestamp
                }

                if (context.feature(SuspendLocationUpdates::class).isSuspended()) {
                    remove(7)
                    addVarInt(7, System.currentTimeMillis() - 15.days.inWholeMilliseconds)
                }
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

    override fun init() {
        if (context.config.global.betterLocation.globalState != true) return

        if (context.config.global.betterLocation.spoofLocation.get()) {
            LocationManager::class.java.apply {
                hook("isProviderEnabled", HookStage.BEFORE) { it.setResult(true) }
                hook("isProviderEnabledForUser", HookStage.BEFORE) { it.setResult(true) }
            }
            Location::class.java.apply {
                hook("getLatitude", HookStage.BEFORE) {
                    it.setResult(getLat()) }
                hook("getLongitude", HookStage.BEFORE) {
                    it.setResult(getLong())
                }
            }
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