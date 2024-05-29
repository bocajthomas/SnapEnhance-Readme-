package me.rhunk.snapenhance.core.util

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class RandomWalking(
    private var walkRadius: Double?,
) {
    var current_x = 0.0
        private set
    var current_y = 0.0
        private set
    private var last_update_time = 0L
    private var pause_expire = 0L
    private var target_x = 0.0
    private var target_y = 0.0

    fun updatePosition() {
        //Latitude ft / deg /Longitude ft /deg = 1.26301179736
        // 4ft/s * 1 degree/364000ft (latitude) * 1s/1000ms = .000000010989011 degrees/ms
        val max_speed = 4.0 / 364000.0 / 1000.0
        val pause_chance = .0023 // .23% chance to pause every second = after 5 minutes 50% chance of pause
        val pause_duration = 60000L //ms
        val pause_random = 30000L //ms

        val now = System.currentTimeMillis()

        if(current_x == target_x && current_y == target_y) {
            val walk_rad = if (walkRadius == null
            ) 0.0 else (walkRadius!! / 364000.0) //Lat deg

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
                    current_x += (target_x - current_x) / dist * max_dist
                    current_y += (target_y - current_y) / dist * max_dist
                }
            }
        }
        last_update_time = now
    }
}