package no.nav.helse.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse

enum class SykdomshendelseType {
    SendtSøknadMottatt,
    NySøknadMottatt,
    InntektsmeldingMottatt
}

internal class SykdomshendelseDeserializer : SykdomstidslinjeHendelse.Deserializer {

    override fun deserialize(jsonNode: JsonNode): SykdomstidslinjeHendelse {
         val type = jsonNode["type"].asText()
         return when (type) {
             SykdomshendelseType.InntektsmeldingMottatt.name -> Inntektsmelding.fromJson(jsonNode)
             SykdomshendelseType.NySøknadMottatt.name -> NySøknad.fromJson(jsonNode)
             SykdomshendelseType.SendtSøknadMottatt.name -> SendtSøknad.fromJson(jsonNode)
             else -> throw RuntimeException("ukjent type: $type for melding ${jsonNode["hendelseId"]?.asText()}")
         }
     }
}
