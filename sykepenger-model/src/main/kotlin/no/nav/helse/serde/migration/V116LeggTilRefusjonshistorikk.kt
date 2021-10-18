package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.serde.serdeObjectMapper

internal class V116LeggTilRefusjonshistorikk : JsonMigration(version = 116) {
    override val description = "Legger til refusjonshistorikk"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val inntektsmeldinger = meldingerSupplier.hentMeldinger()
            .mapValues { (_, melding) -> serdeObjectMapper.readTree(melding) }
            .filterValues { melding -> "inntektsmelding" == melding["@event_name"].asText() }
            .filterValues { melding -> melding.hasNonNull("virksomhetsnummer") }
            .values
            .groupBy { melding -> melding["virksomhetsnummer"].asText() }

        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            val arbeidsgiverInntektsmeldinger = inntektsmeldinger[arbeidsgiver["organisasjonsnummer"].asText()] ?: emptyList()
            val refusjonshistorikk = arbeidsgiver.withArray<ArrayNode>("refusjonshistorikk")
            refusjonshistorikk.removeAll()
            arbeidsgiverInntektsmeldinger.forEach { arbeidsgiverInntektsmelding ->
                refusjonshistorikk.addObject()
                    .put("meldingsreferanseId", arbeidsgiverInntektsmelding["@id"].asText())
                    .put("førsteFraværsdag", arbeidsgiverInntektsmelding["foersteFravaersdag"].asText())
                    .set<ObjectNode>("arbeidsgiverperioder", arbeidsgiverInntektsmelding["arbeidsgiverperioder"].deepCopy<ObjectNode>())
                    .put("beløp", arbeidsgiverInntektsmelding["refusjon"]["beloepPrMnd"].asDouble())
                    .put("sisteRefusjonsdag", arbeidsgiverInntektsmelding["refusjon"]["opphoersdato"].asText())
                    .set<ObjectNode>("endringerIRefusjon", arbeidsgiverInntektsmelding["endringIRefusjoner"].deepCopy<ArrayNode>().onEach { endring ->
                        endring as ObjectNode
                        endring.put("beløp", endring.remove("beloep").asDouble())
                    })
                    .put("tidsstempel", arbeidsgiverInntektsmelding["@opprettet"].asText())
            }
        }
    }
}
