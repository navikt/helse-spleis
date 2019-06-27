package no.nav.helse.søknad.domain

import com.fasterxml.jackson.databind.JsonNode

data class Sykepengesøknad(private val jsonNode: JsonNode) {

    val id = jsonNode["id"].asText()!!

    val sykmeldingId = jsonNode["sykmeldingId"].asText()!!
}
