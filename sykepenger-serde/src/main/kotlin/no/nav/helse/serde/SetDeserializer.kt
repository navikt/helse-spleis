package no.nav.helse.serde

import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.deser.std.StdDeserializer
import java.util.UUID.fromString

internal class SetDeserializer(t: Class<Set<*>>) : StdDeserializer<Set<*>>(t) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Set<*> {

        return p
            .readValueAs(LinkedHashSet::class.java)
            .map {
                try {
                    fromString(it as String)
                } catch (e: Exception) {
                    it
                }
            }
            .toMutableSet()
    }
}
