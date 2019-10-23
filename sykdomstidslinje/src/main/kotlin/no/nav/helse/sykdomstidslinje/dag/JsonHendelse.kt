package no.nav.helse.sykdomstidslinje.dag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelse.*

internal data class JsonHendelse(
    val type: String,
    val json: JsonNode
) {
    fun hendelseId() = json["hendelseId"].asText()

    fun toHendelse(): DokumentMottattHendelse = when (type) {
        DokumentMottattHendelse.Type.InntektsmeldingMottatt.name -> InntektsmeldingMottatt(json)
        DokumentMottattHendelse.Type.NySøknadOpprettet.name -> NySøknadOpprettet(json)
        DokumentMottattHendelse.Type.SendtSøknadMottatt.name -> SendtSøknadMottatt(json)
        Sykepengehistorikk::javaClass.name -> Sykepengehistorikk(json)
        else -> throw RuntimeException("ukjent type")
    }
}