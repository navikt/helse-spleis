package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.til
import org.slf4j.LoggerFactory

internal class V240KopiereSykdomstidslinjeTilVedtaksperiodeutbetalinger: JsonMigration(240) {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override val description = "kopierer historisk sykdomstidslinje til vedtaksperiodeberegninger slik at utbetalingstidslinjeberegning kan deprecates"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()

        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val beregninger = arbeidsgiver.path("beregnetUtbetalingstidslinjer").mapNotNull { beregning ->
                val beregningId = beregning.path("id").id()
                val sykdomshistorikkelementId = beregning.path("sykdomshistorikkElementId").id()

                val sykdomstidslinje = arbeidsgiver.path("sykdomshistorikk").firstOrNull { element ->
                    element.path("id").id() == sykdomshistorikkelementId
                }
                if (sykdomstidslinje == null) logg(aktørId, "beregning $beregningId kan ikke mappes til sykdomstidslinje $sykdomshistorikkelementId fordi elementet finnes ikke")
                sykdomstidslinje?.let {
                    beregningId to it.path("beregnetSykdomstidslinje")
                }
            }.toMap()
            val utbetalinger = arbeidsgiver.path("utbetalinger").associate { utbetaling ->
                utbetaling.path("id").id() to utbetaling.path("beregningId").id()
            }

            arbeidsgiver.path("vedtaksperioder").forEach { vedtaksperiode ->
                migrer(aktørId, beregninger, utbetalinger, vedtaksperiode)
            }

            arbeidsgiver.path("forkastede").forEach { forkasting ->
                val vedtaksperiode = forkasting.path("vedtaksperiode")
                migrer(aktørId, beregninger, utbetalinger, vedtaksperiode)
            }
        }
    }

    private fun migrer(
        aktørId: String,
        beregninger: Map<UUID, JsonNode>,
        utbetalinger: Map<UUID, UUID>,
        vedtaksperiode: JsonNode
    ) {
        val fom = vedtaksperiode.path("fom").dato()
        val tom = vedtaksperiode.path("tom").dato()
        val periodeVedtaksperiode = fom..tom
        vedtaksperiode.path("utbetalinger").forEach { utbetaling ->
            val utbetalingId = utbetaling.path("utbetalingId").id()
            val beregningId = utbetalinger.getValue(utbetalingId)
            val sykdomstidslinje = beregninger[beregningId]?.deepCopy<ObjectNode>() ?: return logg(aktørId, "Utbetaling $utbetalingId peker på en beregning $beregningId som ikke kan mappes til sykdomstidslinje")

            val låstePerioder = sykdomstidslinje.path("låstePerioder") as ArrayNode
            låstePerioder.removeAll()

            sykdomstidslinje.path("periode").takeIf { it.isObject }?.also {
                it as ObjectNode
                it.put("fom", fom.toString())
                it.put("tom", tom.toString())
            }

            val dager = sykdomstidslinje.path("dager") as ArrayNode
            val beholde = dager
                .map { it.deepCopy<JsonNode>() }
                .filter { dag ->
                    if (dag.hasNonNull("dato")) dag.path("dato").dato() in periodeVedtaksperiode
                    else {
                        val dagFom = dag.path("fom").dato()
                        val dagTom = dag.path("tom").dato()
                        (dagFom..dagTom).overlapperMed(periodeVedtaksperiode)
                    }
                }
            beholde.firstOrNull()?.also { dag ->
                if (dag.hasNonNull("fom")) {
                    dag as ObjectNode
                    dag.put("fom", maxOf(fom, dag.path("fom").dato()).toString()) // i tilfelle den første overlappende dagen starter før
                }
            }
            beholde.lastOrNull()?.also { dag ->
                if (dag.hasNonNull("fom")) {
                    dag as ObjectNode
                    dag.put("tom", minOf(dag.path("tom").dato(), tom).toString()) // i tilfelle den siste overlappende dagen slutter etter
                }
            }
            dager.removeAll()
            dager.addAll(beholde)

            (utbetaling as ObjectNode).set<ObjectNode>("sykdomstidslinje", sykdomstidslinje)
        }
    }

    private fun ClosedRange<LocalDate>.overlapperMed(other: ClosedRange<LocalDate>): Boolean {
        val start = maxOf(this.start, other.start)
        val slutt = minOf(this.endInclusive, other.endInclusive)
        return start <= slutt
    }

    private fun logg(aktørId: String, melding: String) {
        sikkerlogg.info("[240] {} $melding", keyValue("aktørId", aktørId))
    }
    private fun JsonNode.id(): UUID = UUID.fromString(asText())
    private fun JsonNode.dato() = LocalDate.parse(asText())
}