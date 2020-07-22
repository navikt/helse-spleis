package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.person.Inntekthistorikk.Inntektsendring.Kilde.INNTEKTSMELDING
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_DATE
import java.util.*

internal class V18UtbetalingstidslinjeØkonomi : JsonMigration(version = 18) {
    override val description = "utvide økonomifelt i Utbetalingstidslinjer"

    private lateinit var inntekthistorikk: Inntekthistorikk

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("utbetalinger").forEach { utbetaling ->
                opprettØkonomi(utbetaling.path("utbetalingstidslinje").path("dager"))
            }
            arbeidsgiver.path("forkastede").forEach { periode ->
                opprettØkonomi(periode.path("utbetalingstidslinje").path("dager"))
            }
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                opprettØkonomi(periode.path("utbetalingstidslinje").path("dager"))
            }
        }
    }

    private fun opprettØkonomi(dager: JsonNode?) {
        if (dager == null) return
        inntekthistorikk = inntekthistorikk(dager)
        dager.forEach { dag ->
            opprettØkonomi(dag as ObjectNode, inntekthistorikk)
        }
    }

    private fun inntekthistorikk(dager: JsonNode) = Inntekthistorikk().also { historikk ->
        dager.forEach { dag ->
            dag.path("dagsats").asInt().also { lønn ->
                if (lønn > 0) historikk.add((dag as ObjectNode).dato, UUID.randomUUID(), lønn.toBigDecimal(), INNTEKTSMELDING)
            }
        }
    }

    private fun opprettØkonomi(dag: ObjectNode, inntekthistorikk: Inntekthistorikk) {
        when(dag["type"].textValue()) {
            "Arbeidsdag" -> opprettMedInntekt(dag)
            "ArbeidsgiverperiodeDag" -> opprett(dag, dag.path("dagsats").asInt())
            "AvvistDag" -> opprett(dag, dag.path("dagsats").asInt(), dag.path("grad").asDouble(), dag.path("utbetaling").asInt())
            "Fridag" -> opprettMedInntekt(dag)
            "ForeldetDag" -> opprettMedInntekt(dag)
            "NavDag" -> opprett(dag, dag.path("dagsats").asInt(), dag.path("grad").asDouble(), dag.path("utbetaling").asInt())
            "NavHelgDag" -> opprett(dag, grad = dag.path("grad").asDouble())
            "UkjentDag" -> opprett(dag)
            else -> throw IllegalArgumentException("ukjent utbetalingsdagstype: ${dag.path("type").asText()}")
        }
    }

    private fun opprett(dag: ObjectNode, dagsats: Number = 0, grad: Double = 0.0, utbetaling: Int = 0) {
        dag.remove("dagsats")
        dag.remove("utbetaling")
        dag.remove("grad")
        dag.put("grad", grad.takeUnless(Double::isNaN) ?: 0.0)
        dag.put("arbeidsgiverBetalingProsent", 100.0)
        dag.put("aktuellDagsinntekt", dagsats.toDouble())
        dag.put("dekningsgrunnlag", dagsats.toDouble())
        dag.put("arbeidsgiverbeløp", utbetaling)
        dag.put("personbeløp", 0)
        dag.put("er6GBegrenset", false)
    }

    private fun opprettMedInntekt(
        dag: ObjectNode,
        grad: Double = 0.0,
        utbetaling: Int = 0
    ) = opprett(dag, inntekthistorikk.inntekt(dag.dato)?.toDouble() ?: 0.0, grad, utbetaling)
}

private val ObjectNode.dato get() = LocalDate.parse(this["dato"].textValue(), ISO_DATE)
