package no.nav.helse.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse

// old enums, replaced by Hendelsetype.
// kept around to patch json when deserializing
internal enum class SykdomshendelseType {
    SendtSøknadMottatt,
    NySøknadMottatt,
    InntektsmeldingMottatt
}

internal class SykdomshendelseDeserializer : SykdomstidslinjeHendelse.Deserializer {

    private companion object {
        private val inntektsmeldingtyper = listOf(
            Hendelsetype.Inntektsmelding.name,
            SykdomshendelseType.InntektsmeldingMottatt.name
        )
        private val søknadtyper = listOf(
            Hendelsetype.NySøknad.name,
            SykdomshendelseType.NySøknadMottatt.name,
            Hendelsetype.SendtSøknad.name,
            SykdomshendelseType.SendtSøknadMottatt.name
        )
    }

    override fun deserialize(jsonNode: JsonNode): SykdomstidslinjeHendelse {
        return when (val type = jsonNode["type"].asText()) {
            in inntektsmeldingtyper -> Inntektsmelding.fromJson(jsonNode.toString())
            in søknadtyper -> SøknadHendelse.fromJson(jsonNode.toString())
            else -> throw RuntimeException("ukjent type: $type for melding ${jsonNode["hendelseId"]?.asText()}")
        }
    }
}
