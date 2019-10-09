package no.nav.helse.hendelse

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

internal class SykdomsheldelseSerializer : StdSerializer<Sykdomshendelse>(Sykdomshendelse::class.java) {
    override fun serialize(hendelse: Sykdomshendelse?, gen: JsonGenerator?, provider: SerializerProvider?) {
        gen?.writeObject(hendelse?.toJson())
    }
}
