package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.serde.migration.MigrationTest.Companion.readResource

fun main() {
    val big = "<FyllMegInnMedFilenDuVilTrimme>.json".readResource()
    val person = jacksonObjectMapper().readTree(big)
    trimPerson(person)
    println(person)
}

private fun JsonNode.fjernAltUntatt(felter: List<String>) {
    this as ObjectNode
    val feltNavn = mutableListOf<String>().apply {
        fieldNames().forEach { fieldName ->
            add(fieldName)
        }
    }
    feltNavn.forEach { k ->
        if (!felter.contains(k)) {
            remove(k)
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
            endring.fjernAltUntatt(listOf("id", "utbetalingId"))
        }
    }
}
