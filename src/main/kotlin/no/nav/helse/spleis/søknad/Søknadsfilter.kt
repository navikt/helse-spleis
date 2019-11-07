package no.nav.helse.spleis.søknad

import com.fasterxml.jackson.databind.JsonNode

internal fun skalTaInnSøknad(søknad: JsonNode, søknadProbe: SøknadProbe): Boolean {
    val id = søknad["id"].textValue()
    val type = søknad["soknadstype"]?.textValue()
            ?: søknad["type"]?.textValue()
            ?: throw RuntimeException("Fant ikke type på søknad")
    val status = søknad["status"].textValue()

    return when {
        riktigSøknad(type, status) -> true
        else -> {
            SøknadProbe.søknadIgnorert(id, type, status)
            false
        }
    }
}

private fun riktigSøknad(type: String, status: String) = type in listOf("ARBEIDSTAKERE", "SELVSTENDIGE_OG_FRILANSERE")
        && (status in listOf("SENDT", "NY", "FREMTIDIG"))

internal fun erSendtSøknad(søknad: JsonNode) = søknad["status"].textValue() == "SENDT"
