package me.rhunk.snapenhance.common.config

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

object DataProcessors {
    enum class Type {
        STRING,
        BOOLEAN,
        INTEGER,
        FLOAT,
        STRING_MULTIPLE_SELECTION,
        STRING_UNIQUE_SELECTION,
        MAP_COORDINATES,
        INT_COLOR,
        CONTAINER,
    }

    class PropertyDataProcessor<T>
    internal constructor(
        val type: Type,
        private val serialize: (T, exportSensitiveData: Boolean) -> JsonElement,
        private val deserialize: (JsonElement) -> T
    ) {
        @Suppress("UNCHECKED_CAST")
        fun serializeAny(value: Any, exportSensitiveData: Boolean) = serialize(value as T, exportSensitiveData)
        fun deserializeAny(value: JsonElement) = deserialize(value)
    }

    val STRING = PropertyDataProcessor(
        type = Type.STRING,
        serialize = { it, _ ->
            if (it != null) JsonPrimitive(it)
            else JsonNull.INSTANCE
        },
        deserialize = {
            if (it.isJsonNull) null
            else it.asString
        },
    )

    val BOOLEAN = PropertyDataProcessor(
        type = Type.BOOLEAN,
        serialize = { it, _ ->
            if (it) JsonPrimitive(true)
            else JsonPrimitive(false)
        },
        deserialize = { it.asBoolean },
    )

    val INTEGER = PropertyDataProcessor(
        type = Type.INTEGER,
        serialize = { it, _ -> JsonPrimitive(it) },
        deserialize = { it.asInt },
    )

    val FLOAT = PropertyDataProcessor(
        type = Type.FLOAT,
        serialize = { it, _ -> JsonPrimitive(it) },
        deserialize = { it.asFloat },
    )

    val STRING_MULTIPLE_SELECTION = PropertyDataProcessor(
        type = Type.STRING_MULTIPLE_SELECTION,
        serialize = { it, _ -> JsonArray().apply { it.forEach { add(it) } } },
        deserialize = { obj ->
            obj.asJsonArray.map { it.asString }.toMutableList()
        },
    )

    val STRING_UNIQUE_SELECTION = PropertyDataProcessor(
        type = Type.STRING_UNIQUE_SELECTION,
        serialize = { it, _ -> JsonPrimitive(it) },
        deserialize = { obj -> obj.takeIf { !it.isJsonNull }?.asString?.takeIf { it != "false" && it != "true" } }
    )

    val MAP_COORDINATES = PropertyDataProcessor(
        type = Type.MAP_COORDINATES,
        serialize = { it, _ ->
            JsonObject().apply {
                addProperty("lat", it.first.takeIf { it in -90.0..90.0 } ?: 0.0)
                addProperty("lng", it.second.takeIf { it in -180.0..180.0 } ?: 0.0)
            }
        },
        deserialize = { obj ->
            val jsonObject = obj.asJsonObject
            (jsonObject["lat"].asDouble.takeIf { it in -90.0..90.0 } ?: 0.0) to
                (jsonObject["lng"].asDouble.takeIf { it in -180.0..180.0 } ?: 0.0)
        },
    )

    val INT_COLOR = PropertyDataProcessor(
        type = Type.INT_COLOR,
        serialize = { it, _ ->
            it?.let { JsonPrimitive(it) } ?: JsonNull.INSTANCE
        },
        deserialize = { if (it.isJsonNull) null else it.asString.toIntOrNull() },
    )

    fun <T : ConfigContainer> container(container: T) = PropertyDataProcessor(
        type = Type.CONTAINER,
        serialize = { it, exportSensitiveData ->
            JsonObject().apply {
                addProperty("state", it.globalState)
                add("properties", it.toJson(exportSensitiveData))
            }
        },
        deserialize = { obj ->
            val jsonObject = obj.asJsonObject
            container.apply {
                globalState = jsonObject["state"]?.takeIf { !it.isJsonNull }?.asBoolean
                jsonObject["properties"]?.asJsonObject?.let { fromJson(it) }
            }
        },
    )
}