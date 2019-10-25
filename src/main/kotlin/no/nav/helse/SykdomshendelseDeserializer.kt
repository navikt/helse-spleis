package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.inntektsmelding.InntektsmeldingHendelse

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.søknad.NySøknadHendelse
import no.nav.helse.søknad.SendtSøknadHendelse

enum class SykdomshendelseType {
    SendtSøknadMottatt,
    NySøknadMottatt,
    InntektsmeldingMottatt
}

class SykdomshendelseDeserializer : SykdomstidslinjeHendelse.Deserializer {

    override fun deserialize(jsonNode: JsonNode): SykdomstidslinjeHendelse {
         val type = jsonNode["type"].asText()
         return when (type) {
             SykdomshendelseType.InntektsmeldingMottatt.name -> InntektsmeldingHendelse.fromJson(jsonNode)
             SykdomshendelseType.NySøknadMottatt.name -> NySøknadHendelse.fromJson(jsonNode)
             SykdomshendelseType.SendtSøknadMottatt.name -> SendtSøknadHendelse.fromJson(jsonNode)
             else -> throw RuntimeException("ukjent type: $type for melding ${jsonNode["hendelseId"]?.asText()}")
         }
     }
}
