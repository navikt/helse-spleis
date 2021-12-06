package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArgument
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.Periode
import no.nav.helse.sykdomstidslinje.erHelg
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal class V130TrimmeUtbetalingstidslinje : JsonMigration(version = 130) {
    override val description = "Fjerner innledende arbeidsdager og fridager på Utbetalingstidslinje forårsaket av historisk bug hvor vi ikke trimmet ordentlig"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val sizeBefore = jsonNode.toString().length

        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            migrer("utbetalingId", arbeidsgiver["utbetalinger"])
            migrer("beregnetUtbetalingstidslinjeId", arbeidsgiver["beregnetUtbetalingstidslinjer"])
            migrer("vedtaksperiodeId", arbeidsgiver["vedtaksperioder"])
            migrer("forkastetVedtaksperiodeId", arbeidsgiver["forkastede"].map { it.path("vedtaksperiode") })
        }

        val sizeAfter = jsonNode.toString().length
        logger.info("Trimmet bort ${sizeBefore - sizeAfter} bytes")
    }

    private fun migrer(ting: String, elementer: Iterable<JsonNode>) {
        elementer.forEach { element ->
            val id = element.path("id").asText()
            trimUtbetalingstidslinje(element.path("utbetalingstidslinje"), keyValue(ting, id))
        }
    }

    private fun trimUtbetalingstidslinje(utbetalingstidslinje: JsonNode, sporing: StructuredArgument) {
        val dager = utbetalingstidslinje.path("dager") as ArrayNode
        val indeksFørsteOrdentligeDag = finnFørsteDagSomIkkeErArbeidsdagEllerHelgFridag(dager) ?: return
        val førsteOrdentligeDag = dager[indeksFørsteOrdentligeDag]
        val førsteDag = dager.first()

        repeat(indeksFørsteOrdentligeDag) { dager.remove(0) }

        val førsteDagPeriode = periode(førsteDag)
        val førsteOrdentligeDagPeriode = periode(førsteOrdentligeDag)

        val avstand = ChronoUnit.DAYS.between(førsteDagPeriode.start, førsteOrdentligeDagPeriode.start.plusDays(1))

        logger.info("Trimmer bort $avstand dager i utbetalingstidslinjen for {}", sporing)
    }

    private fun finnFørsteDagSomIkkeErArbeidsdagEllerHelgFridag(dager: JsonNode) = dager.indexOfFirst { dag ->
        !erArbeidsdag(dag) && !erHelgFridag(dag) && !erUkjentDag(dag)
    }.takeIf { it > 0 }

    private fun erUkjentDag(dag: JsonNode) = dag.path("type").asText() == UkjentDag
    private fun erArbeidsdag(dag: JsonNode) = dag.path("type").asText() == Arbeidsdag
    private fun erHelgFridag(dag: JsonNode) = dag.path("type").asText() == Fridag && periode(dag).erKunHelg()
    private fun periode(dag: JsonNode) = if (dag.hasNonNull("dato"))
        dag.path("dato").asLocalDate().let { Periode(it, it) }
    else
        Periode(dag.path("fom").asLocalDate(), dag.path("tom").asLocalDate())
    private fun JsonNode.asLocalDate() = LocalDate.parse(asText())
    private fun Periode.erKunHelg() = count() <= 2 && start.erHelg() && endInclusive.erHelg()

    private companion object {
        private val logger = LoggerFactory.getLogger("tjenestekall")
        private const val Arbeidsdag = "Arbeidsdag"
        private const val Fridag = "Fridag"
        private const val UkjentDag = "UkjentDag"
    }
}
