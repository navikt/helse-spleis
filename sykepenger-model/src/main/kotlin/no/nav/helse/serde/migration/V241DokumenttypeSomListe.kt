package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory

internal class V241DokumenttypeSomListe: JsonMigration(241) {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override val description = "gjør hendelserIder til en liste av dokumenter fordi inntektsmelding håndteres to ganger (inntekt- og dager-delen)"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { vedtaksperiode ->
                migrer(vedtaksperiode)
            }

            arbeidsgiver.path("forkastede").forEach { forkasting ->
                val vedtaksperiode = forkasting.path("vedtaksperiode")
                migrer(vedtaksperiode)
            }
        }
    }

    private fun migrer(vedtaksperiode: JsonNode) {
        val hendelser = vedtaksperiode.path("hendelseIder")
            .fields()
            .asSequence()
            .flatMap { (dokumentId, dokumenttype) ->
                when (val dokumenttypetekst = dokumenttype.asText()) {
                    "Inntektsmelding" -> listOf(
                        dokumentobjekt(dokumentId, "InntektsmeldingDager"),
                        dokumentobjekt(dokumentId, "InntektsmeldingInntekt")
                    )
                    else -> listOf(dokumentobjekt(dokumentId, dokumenttypetekst))
                }
            }.toList()

        (vedtaksperiode as ObjectNode).replace("hendelseIder", serdeObjectMapper.createArrayNode().addAll(hendelser))
    }

    private fun dokumentobjekt(dokumentId: String, dokumenttype: String): JsonNode {
        return serdeObjectMapper.createObjectNode().apply {
            put("dokumentId", dokumentId)
            put("dokumenttype", dokumenttype)
        }
    }
}