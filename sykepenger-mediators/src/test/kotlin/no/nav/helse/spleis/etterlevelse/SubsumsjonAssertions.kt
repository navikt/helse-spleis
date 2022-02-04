package no.nav.helse.spleis.etterlevelse

import com.fasterxml.jackson.databind.JsonNode
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion.VersionFlag.V7
import com.networknt.schema.ValidationMessage
import org.junit.jupiter.api.Assertions.assertEquals
import java.net.URI

private val schema by lazy {
    JsonSchemaFactory
        .getInstance(V7)
        .getSchema(URI("https://raw.githubusercontent.com/navikt/helse/c53bc453251b7878135f31d5d1070e5406ae4af1/subsumsjon/json-schema-1.0.0.json"))
}

internal fun assertSubsumsjonsmelding(melding: JsonNode) {
    assertEquals(emptySet<ValidationMessage>(), schema.validate(melding))
}
