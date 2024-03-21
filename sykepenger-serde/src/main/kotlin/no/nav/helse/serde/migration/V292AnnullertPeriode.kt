package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory
import org.slf4j.MDC

internal class V292AnnullertPeriode: JsonMigration(version = 292) {
    override val description = "setter tilstand=ANNULLERT_PERIODE for annullerte behandlinger"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()
        val fnr = jsonNode.path("fødselsnummer").asText()
        MDC.putCloseable("aktørId", aktørId).use {
            jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
                val orgnr = arbeidsgiver.path("organisasjonsnummer").asText()
                MDC.putCloseable("orgnr", orgnr).use {
                    val opphørtAvAndreUtbetalinger = arbeidsgiver.path("utbetalinger").flatMap { utbetaling ->
                        utbetaling.path("annulleringer").map { it.asText() }
                    }.toSet()
                    val utbetalinger = arbeidsgiver.path("utbetalinger")
                        .associateBy { it.path("id").asText() }

                    val annulleringer = arbeidsgiver.path("utbetalinger")
                        .filter { utbetaling -> utbetaling.path("type").asText() == "ANNULLERING" && utbetaling.path("status").asText() == "ANNULLERT" }
                        .filterNot { annullering -> annullering.path("id").asText() in opphørtAvAndreUtbetalinger }

                    arbeidsgiver.path("forkastede")
                        .forEach { forkastet ->
                            val vedtaksperiode = forkastet.path("vedtaksperiode")
                            MDC.putCloseable("vedtaksperiodeId", vedtaksperiode.path("id").asText()).use {
                                migrerVedtaksperiode(utbetalinger, annulleringer, vedtaksperiode)
                            }
                        }
                }
            }
        }
    }

    private fun migrerVedtaksperiode(utbetalinger: Map<String, JsonNode>, annulleringer: List<JsonNode>, vedtaksperiode: JsonNode) {
        val generasjoner = vedtaksperiode.path("generasjoner") as ArrayNode
        val siste = generasjoner.last()

        if (siste.path("tilstand").asText() != "TIL_INFOTRYGD") return sikkerLogg.info("[V292] siste generasjon ${siste.path("id").asText()} som står i ${siste.path("tilstand").asText()} og det er forventet at den skal være TIL_INFOTRYGD")
        if (generasjoner.size() == 1) return

        val nestSist = generasjoner[generasjoner.size() - 2]
        if (nestSist.path("tilstand").asText() != "VEDTAK_IVERKSATT") return

        val sisteEndring = nestSist.path("endringer").lastOrNull() ?: return
        val utbetalingId = sisteEndring.path("utbetalingId")?.takeIf(JsonNode::isTextual)?.asText()
            ?: return sikkerLogg.info("[V292] nest siste generasjon ${siste.path("id").asText()} har ingen utbetaling ?!")
        val utbetalingen = utbetalinger.getValue(utbetalingId)

        val tilhørendeAnnullering = annulleringer.firstOrNull { annullering ->
            annullering.path("korrelasjonsId").asText() == utbetalingen.path("korrelasjonsId").asText()
        } ?: return sikkerLogg.info("[V292] fant ikke en tilhørende annullering for utbetaling $utbetalingId ?!")

        sikkerLogg.info("[V292] endrer tilstand til ANNULLERT_PERIODE for generasjon ${siste.path("id").asText()}")
        siste as ObjectNode
        siste.put("tilstand", "ANNULLERT_PERIODE")
        val sisteEndringerSisteGenerasjon = siste.path("endringer").last() as ObjectNode

        val kopi = sisteEndring.deepCopy<ObjectNode>()
        sisteEndringerSisteGenerasjon.put("utbetalingId", tilhørendeAnnullering.path("id").asText())
        kopi.path("vilkårsgrunnlagId").takeIf(JsonNode::isTextual)?.asText()?.also { vilkårsgrunnlagId ->
            sisteEndringerSisteGenerasjon.put("vilkårsgrunnlagId", vilkårsgrunnlagId)
        }
    }

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    }
}