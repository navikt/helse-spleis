package no.nav.helse.behov

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class Pakke(private val behov: List<String>, map: Map<String, Any> = emptyMap()) {

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }


    private val params: MutableMap<String, Any> = map.toMutableMap()

    internal operator fun set(key: String, value: Any) {
        params[key] = value
    }

    internal operator fun get(key: String): Any? = params[key]

    internal operator fun plus(other: Pakke) = Pakke(this.behov + other.behov, this.params + other.params)

    fun toJson(): String {
        val paramsMedBehov = params + ("behov" to behov)
        return objectMapper.writeValueAsString(paramsMedBehov)
    }

    internal interface Transportpakke {
        operator fun plus(other: Pakke): Pakke
    }
}
