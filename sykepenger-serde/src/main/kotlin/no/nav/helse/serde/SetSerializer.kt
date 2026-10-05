package no.nav.helse.serde

import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ser.std.StdSerializer

internal class SetSerializer(t: Class<Set<*>>) : StdSerializer<Set<*>>(t) {

    override fun serialize(value: Set<*>, jgen: JsonGenerator, provider: SerializationContext) {
        jgen.writeStartArray()
        value.forEach {
            jgen.writeString(it.toString())
        }
        jgen.writeEndArray()
    }


}
