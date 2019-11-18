package no.nav.helse.hendelser.søknad

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

internal class SykepengesøknadSerializer : StdSerializer<Sykepengesøknad>(Sykepengesøknad::class.java) {
    override fun serialize(hendelse: Sykepengesøknad?, gen: JsonGenerator?, provider: SerializerProvider?) {
        gen?.writeObject(hendelse?.toJson())
    }
}
