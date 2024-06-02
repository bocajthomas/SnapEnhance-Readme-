package me.rhunk.snapenhance.storage

import android.content.ContentValues
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import me.rhunk.snapenhance.bridge.location.LocationCoordinates
import me.rhunk.snapenhance.common.util.ktx.getDoubleOrNull
import me.rhunk.snapenhance.common.util.ktx.getInteger
import me.rhunk.snapenhance.common.util.ktx.getStringOrNull


fun AppDatabase.getLocationCoordinates(): List<LocationCoordinates> {
    return runBlocking(executor.asCoroutineDispatcher()) {
        database.rawQuery("SELECT * FROM location_coordinates ORDER BY id DESC", null).use { cursor ->
            val locationCoordinates = mutableListOf<LocationCoordinates>()
            while (cursor.moveToNext()) {
                locationCoordinates.add(
                    LocationCoordinates().run {
                        id = cursor.getInteger("id")
                        name = cursor.getStringOrNull("name") ?: return@run null
                        latitude = cursor.getDoubleOrNull("latitude") ?: return@run null
                        longitude = cursor.getDoubleOrNull("longitude") ?: return@run null
                        radius = cursor.getDoubleOrNull("radius") ?: return@run null
                        this
                    } ?: continue
                )
            }
            locationCoordinates
        }
    }
}

fun AppDatabase.addOrUpdateLocationCoordinate(id: Int?, locationCoordinates: LocationCoordinates): Int {
    return runBlocking(executor.asCoroutineDispatcher()) {
        if (id == null) {
            val resultId = database.insert("location_coordinates", null, ContentValues().apply {
                put("name", locationCoordinates.name)
                put("latitude", locationCoordinates.latitude)
                put("longitude", locationCoordinates.longitude)
                put("radius", locationCoordinates.radius)
            })
            resultId.toInt()
        } else {
            database.update("location_coordinates", ContentValues().apply {
                put("name", locationCoordinates.name)
                put("latitude", locationCoordinates.latitude)
                put("longitude", locationCoordinates.longitude)
                put("radius", locationCoordinates.radius)
            }, "id = ?", arrayOf(id.toString()))
            id
        }
    }
}

fun AppDatabase.removeLocationCoordinate(id: Int) {
    runBlocking(executor.asCoroutineDispatcher()) {
        database.delete("location_coordinates", "id = ?", arrayOf(id.toString()))
    }
}


