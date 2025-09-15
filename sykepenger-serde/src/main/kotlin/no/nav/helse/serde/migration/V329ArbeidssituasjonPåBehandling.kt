package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V329ArbeidssituasjonPåBehandling : JsonMigration(329) {
    override val description = "Legger til arbeidssituasjon på alle behandlinger"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val arbeidssituasjon = when (arbeidsgiver.path("yrkesaktivitetstype").asText()) {
                "ARBEIDSTAKER" -> "ARBEIDSTAKER"
                "ARBEIDSLEDIG" -> "ARBEIDSLEDIG"
                "FRILANS" -> "FRILANSER"
                "SELVSTENDIG" -> "SELVSTENDIG_NÆRINGSDRIVENDE"
                "SELVSTENDIG_JORDBRUKER" -> "SELVSTENDIG_NÆRINGSDRIVENDE"
                "SELVSTENDIG_FISKER" -> "SELVSTENDIG_NÆRINGSDRIVENDE"
                "SELVSTENDIG_BARNEPASSER" -> "BARNEPASSER"
                else -> "ARBEIDSTAKER"
            }
            arbeidsgiver.path("vedtaksperioder").forEach { vedtaksperiode ->
                migrerVedtaksperiode(vedtaksperiode, arbeidssituasjon)
            }
            arbeidsgiver.path("forkastede").forEach { forkastet ->
                migrerVedtaksperiode(forkastet.path("vedtaksperiode"), arbeidssituasjon)
            }
        }
    }

    private fun migrerVedtaksperiode(vedtaksperiode: JsonNode, arbeidssituasjon: String) {
        vedtaksperiode.path("behandlinger").forEach { behandling ->
            behandling.path("endringer").forEach { endring ->
                (endring as ObjectNode).put("arbeidssituasjon", arbeidssituasjon)
            }
        }
    }
}
