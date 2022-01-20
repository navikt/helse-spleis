package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion.VersionFlag.V7
import com.networknt.schema.ValidationMessage
import org.junit.jupiter.api.Assertions.assertEquals
import java.net.URI

internal object SubsumsjonAssertions {
    private val objectMapper = jacksonObjectMapper()
    private val schema by lazy {
        JsonSchemaFactory
            .getInstance(V7)
            .getSchema(URI("https://raw.githubusercontent.com/navikt/helse/main/subsumsjon/json-schema-1.0.0.json"))
    }

    internal fun assertSubsumsjonsmelding(melding: JsonNode) {
        assertEquals(emptySet<ValidationMessage>(), schema.validate(melding))
    }

    internal fun assertSubsumsjonsmelding(melding: String) = assertSubsumsjonsmelding(objectMapper.readTree(melding))
}
