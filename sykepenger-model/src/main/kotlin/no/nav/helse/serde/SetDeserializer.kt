package no.nav.helse.serde

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import java.util.UUID.fromString

class SetDeserializer(t: Class<Set<*>>) : StdDeserializer<Set<*>>(t) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Set<*> {

        return p
            .readValueAs(LinkedHashSet::class.java)
            .map {
                try {
                    fromString(it as String)
                } catch(e: Exception) {
                    it
                }
            }
            .toMutableSet()
    }
}
