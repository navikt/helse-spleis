package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.LoggerFactory

private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
internal class V317SykNavIEgenListe : JsonMigration(version = 317) {
    override val description = "legger SykNav-dager i egen liste"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { vedtaksperiode ->
                migrerVedtaksperiode(vedtaksperiode)
            }
            arbeidsgiver.path("forkastede").forEach { forkastet ->
                migrerVedtaksperiode(forkastet.path("vedtaksperiode"))
            }
        }
    }

    private fun migrerVedtaksperiode(vedtaksperiode: JsonNode) {
        val id = vedtaksperiode.path("id").asText()
        vedtaksperiode.path("behandlinger").forEach {
            migrerBehandling(id, it)
        }
    }

    private fun migrerBehandling(vedtaksperiodeId: String, behandling: JsonNode) {
        behandling.path("endringer").forEach { migrerEndring(vedtaksperiodeId, it) }
    }

    private fun migrerEndring(vedtaksperiodeId: String, endring: JsonNode) {
        val sykNavDager = endring.path("sykdomstidslinje").path("dager")
            .filter { it.path("type").asText() == "SYKEDAG_NAV" }
            .flatMap { dag ->
                val fom = (dag.path("dato").takeIf(JsonNode::isTextual) ?: dag.path("fom")).asText().dato
                val tom = (dag.path("dato").takeIf(JsonNode::isTextual) ?: dag.path("tom")).asText().dato
                fom.rangeTo(tom).dager
            }
        val dagerFraFør = endring.path("dagerNavOvertarAnsvar").flatMap { periode ->
            periode.path("fom").asText().dato.rangeTo(periode.path("tom").asText().dato).dager
        }

        if (endring.hasNonNull("dagerNavOvertarAnsvar")) {
            sykNavDager
                .filter { dag -> dag !in dagerFraFør }
                .takeIf(List<*>::isNotEmpty)
                ?.also {
                    sikkerlogg.info("{} har forskjell i syknav-dager: $it", kv("vedtaksperiodeId", vedtaksperiodeId))
                }
        }

        (endring as ObjectNode).putArray("dagerNavOvertarAnsvar").apply {
            removeAll()
            sykNavDager.grupperDatoer().forEach { range ->
                addObject().apply {
                    put("fom", range.start.toString())
                    put("tom", range.endInclusive.toString())
                }
            }
        }
    }

    private val ClosedRange<LocalDate>.dager get() =
        start.datesUntil(endInclusive.plusDays(1)).toList()

    private fun List<LocalDate>.grupperDatoer() = this
        .fold(emptyList<ClosedRange<LocalDate>>()) { resultat, dag ->
        val last = resultat.lastOrNull()
        when {
            last == null -> listOf(dag.rangeTo(dag))
            last.endInclusive.plusDays(1) == dag -> resultat.dropLast(1) + (last.start.rangeTo(dag))
            else -> resultat.plusElement(dag.rangeTo(dag))
        }
    }
}
