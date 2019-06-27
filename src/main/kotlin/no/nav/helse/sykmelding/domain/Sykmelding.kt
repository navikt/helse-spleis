package no.nav.helse.sykmelding.domain

import com.fasterxml.jackson.databind.JsonNode

data class Sykmelding(private val jsonNode: JsonNode) {

    val id = jsonNode["id"].asText()!!
}
