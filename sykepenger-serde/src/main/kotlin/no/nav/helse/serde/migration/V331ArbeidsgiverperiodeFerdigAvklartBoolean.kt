package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import no.nav.helse.serde.migration.JsonMigration.Companion.dato
import org.slf4j.LoggerFactory

internal class V331ArbeidsgiverperiodeFerdigAvklartBoolean : JsonMigration(331) {
    override val description = "Boolean for ferdig avklart AGP"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val erAGPRelevant = arbeidsgiver.path("yrkesaktivitetstype").asText() == "ARBEIDSTAKER"
            arbeidsgiver.path("vedtaksperioder").forEach { vedtaksperiode ->
                migrerVedtaksperiode(vedtaksperiode, erAGPRelevant)
            }
            arbeidsgiver.path("forkastede").forEach { forkastet ->
                migrerVedtaksperiode(forkastet.path("vedtaksperiode"), erAGPRelevant)
            }
        }
    }

    private fun migrerVedtaksperiode(vedtaksperiode: JsonNode, erAGPRelevant: Boolean) {
        vedtaksperiode.path("behandlinger").forEach { behandling ->
            val behandlingstilstand = behandling.path("tilstand").asText()
            behandling.path("endringer").forEach { endring ->
                endring as ObjectNode
                val agp = endring.path("arbeidsgiverperioder").deepCopy<ArrayNode>()
                val ferdigAvklart = !erAGPRelevant || erFerdigAvklart(behandlingstilstand, agp)
                endring.putObject("arbeidsgiverperiode").apply {
                    put("ferdigAvklart", ferdigAvklart)
                    set<ArrayNode>("dager", agp.utenInfotrygd)
                }
            }
        }
    }

    private fun erFerdigAvklart(tilstand: String, agp: ArrayNode): Boolean {
        return when (tilstand) {
            "UBEREGNET",
            "TIL_INFOTRYGD" -> agp.fraInfotrygd || agp.antallDager() == 16

            "AVSLUTTET_UTEN_VEDTAK" -> false

            "UBEREGNET_OMGJØRING",
            "UBEREGNET_REVURDERING",
            "BEREGNET",
            "BEREGNET_OMGJØRING",
            "BEREGNET_REVURDERING",
            "VEDTAK_FATTET",
            "REVURDERT_VEDTAK_AVVIST",
            "VEDTAK_IVERKSATT",
            "ANNULLERT_PERIODE",
            "UBEREGNET_ANNULLERING",
            "BEREGNET_ANNULLERING",
            "OVERFØRT_ANNULLERING" -> true

            else -> error("Ukjent tilstand: $tilstand")
        }
    }

    private fun ArrayNode.antallDager(): Int {
        return sumOf {
            val fom = it.path("fom").dato
            val tom = it.path("tom").dato
            ChronoUnit.DAYS.between(fom, tom).toInt() + 1
        }
    }
}

private val ArrayNode.utenInfotrygd get() = if (fraInfotrygd) this.arrayNode() else this

private val ArrayNode.fraInfotrygd get() = this.count() == 1
    && this[0].path("fom").dato == LocalDate.EPOCH
    && this[0].path("tom").dato == LocalDate.EPOCH

private val JsonNode.dato get() = when {
    isTextual -> asText().dato
    isArray -> LocalDate.of(this[0].asInt(), this[1].asInt(), this[2].asInt())
    else -> error("Ukjent datoformat: $this")
}

private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
