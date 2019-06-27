package no.nav.helse.sykmelding.domain

import com.fasterxml.jackson.databind.JsonNode

data class SykmeldingMessage(private val jsonNode: JsonNode) {

    val sykmelding get() = Sykmelding(jsonNode["sykmelding"])
}
