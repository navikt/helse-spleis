package no.nav.helse.behov

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

internal class Pakke(map: Map<String, Any> = emptyMap()) {

    companion object {
        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun fromJson(json: String): Pakke {
            val params = objectMapper.readValue<Map<String, Any>>(json, object : TypeReference<Map<String, Any>>() {})
            return Pakke(params)
        }
    }


    private val params: MutableMap<String, Any> = map.toMutableMap()

    fun requireKey(key: String) {
        require(params.containsKey(key)) { "$key must be present" }
    }

    internal operator fun set(key: String, value: Any) {
        params[key] = value
    }

    internal operator fun get(key: String): Any? =
            params[key]

    fun toJson(): String {
        return objectMapper.writeValueAsString(params)
    }
}
