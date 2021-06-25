package no.nav.helse.serde

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

class SetSerializer(t: Class<Set<*>>) : StdSerializer<Set<*>>(t) {

    override fun serialize(value: Set<*>, jgen: JsonGenerator, provider: SerializerProvider) {
        jgen.writeStartArray();
        value.forEach {
            jgen.writeString(it.toString())
        }
        jgen.writeEndArray();
    }


}
