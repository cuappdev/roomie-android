package com.example.roomie.data.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * The API sends dates as `YYYY-MM-DD` ([LocalDate]) and timestamps as ISO 8601 ([Instant]).
 * Nulls are left out of request bodies, so an unset PATCH field isn't changed.
 */
object RoomieGson {
    val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter.nullSafe())
        .registerTypeAdapter(Instant::class.java, InstantAdapter.nullSafe())
        .create()
}

private object LocalDateAdapter : TypeAdapter<LocalDate>() {
    override fun write(out: JsonWriter, value: LocalDate) {
        out.value(value.toString())
    }

    override fun read(reader: JsonReader): LocalDate = LocalDate.parse(reader.nextString())
}

private object InstantAdapter : TypeAdapter<Instant>() {
    override fun write(out: JsonWriter, value: Instant) {
        out.value(value.toString())
    }

    // OffsetDateTime accepts both `Z` and `+00:00` offsets.
    override fun read(reader: JsonReader): Instant = OffsetDateTime.parse(reader.nextString()).toInstant()
}
