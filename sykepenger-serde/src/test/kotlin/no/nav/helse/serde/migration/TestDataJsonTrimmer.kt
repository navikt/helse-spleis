package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File

fun main() {
    /**
     * 1. Skriv din setup for migrering som en test i en 'AbstractDslTest'
     * 2. Skriv på slutten av testen `val json = dto().tilPersonData().tilSerialisertPerson().json`
     * 3. Debug testen og kopier innholdet av `json`
     * 4. CMD + Shift + N (Åpne JSON scratch fil)
     * 5. Paste innholdet av `json`
     * 6. Høyreklikk på scratch-fil -> Copy Path -> Absolute Path
     * 7. Paste pathen i `path`-varabelen under
     * 8. Tilpass felter som beholdes etter DITT behov. Defaulten er tilpasset V344
     * 9. Kjør og du har et bra utgangspunkt for din `original.json`
     */
    val path = "<Du må lese det som står over!>"
    val storPerson = File(path).readText()
    val person = jacksonObjectMapper().readTree(storPerson)
    trimPerson(person)
    println(person)
}

private fun JsonNode.fjernAltUntatt(felter: List<String>) {
    this as ObjectNode
    fieldNames().asSequence().toSet().forEach { key ->
        if (key !in felter) {
            remove(key)
        }
    }
}

private fun trimPerson(person: JsonNode) {
    person.fjernAltUntatt(listOf("fødselsnummer", "arbeidsgivere"))
    person.path("arbeidsgivere").forEach { arbeidsgiver ->
        arbeidsgiver.fjernAltUntatt(listOf("organisasjonsnummer", "yrkesaktivitetstype", "vedtaksperioder", "forkastede"))
        arbeidsgiver.path("vedtaksperioder").forEach { periode ->
            trimVedtaksperiode(periode)
        }
        arbeidsgiver.path("forkastede").forEach { forkastet ->
            trimVedtaksperiode(forkastet.path("vedtaksperiode"))
        }
    }
}

private fun trimVedtaksperiode(vedtaksperiode: JsonNode) {
    vedtaksperiode.fjernAltUntatt(listOf("id", "tilstand", "behandlinger"))
    vedtaksperiode.path("behandlinger").forEach { behandling ->
        behandling.fjernAltUntatt(listOf("id", "tilstand", "endringer"))
        behandling.path("endringer").forEach { endring ->
            endring.fjernAltUntatt(listOf("id", "utbetalingId", "tidsstempel"))
        }
    }
}
