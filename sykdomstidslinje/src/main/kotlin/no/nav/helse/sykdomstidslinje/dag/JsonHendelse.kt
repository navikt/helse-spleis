package no.nav.helse.sykdomstidslinje.dag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelse.*

internal data class JsonHendelse(
    val type: String,
    val json: JsonNode
) {
    fun hendelseId() = json["hendelseId"].asText()
    fun toHendelse(): Sykdomshendelse = when (enumValueOf<Sykdomshendelse.Type>(type)) {
        Sykdomshendelse.Type.Inntektsmelding -> Inntektsmelding(json)
        Sykdomshendelse.Type.NySykepengesøknad -> NySykepengesøknad(json)
        Sykdomshendelse.Type.SendtSykepengesøknad -> SendtSykepengesøknad(
            json
        )
        Sykdomshendelse.Type.Sykepengehistorikk -> Sykepengehistorikk(json)
    }
}