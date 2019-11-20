package no.nav.helse.spleis.søknad

import com.fasterxml.jackson.databind.JsonNode

internal fun skalTaInnSøknad(søknad: JsonNode): Boolean {
    val type = søknad["soknadstype"]?.textValue()
            ?: søknad["type"]?.textValue()
            ?: throw RuntimeException("Fant ikke type på søknad")
    val status = søknad["status"].textValue()

    return when {
        riktigSøknad(type, status) -> true
        else -> {
            SøknadProbe.søknadIgnorert()
            false
        }
    }
}

private fun riktigSøknad(type: String, status: String) = type in listOf("ARBEIDSTAKERE", "SELVSTENDIGE_OG_FRILANSERE")
        && (status in listOf("SENDT", "NY", "FREMTIDIG"))
