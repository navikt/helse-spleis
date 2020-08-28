package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.person.InntekthistorikkVol2
import no.nav.helse.person.InntekthistorikkVol2.Inntektsendring.Kilde.INNTEKTSMELDING
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_DATE
import java.util.*

internal class V18UtbetalingstidslinjeØkonomi : JsonMigration(version = 18) {
    override val description = "utvide økonomifelt i Utbetalingstidslinjer"

    private lateinit var inntekthistorikk: InntekthistorikkVol2

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
            opprettØkonomi(dag as ObjectNode)
        }
    }

    private fun inntekthistorikk(dager: JsonNode) = InntekthistorikkVol2().also { historikk ->
        dager.forEach { dag ->
            dag.path("dagsats").asInt().also { lønn ->
                if (lønn > 0) historikk.add((dag as ObjectNode).dato, UUID.randomUUID(), lønn.daglig, INNTEKTSMELDING)
            }
        }
    }

    private fun opprettØkonomi(dag: ObjectNode) {
        when(dag["type"].textValue()) {
            "Arbeidsdag" -> opprettMedInntekt(dag)
            "ArbeidsgiverperiodeDag" -> opprett(dag, dag.path("dagsats").asInt().daglig)
            "AvvistDag" -> opprett(dag, dag.path("dagsats").asInt().daglig, dag.path("grad").asDouble(), dag.path("utbetaling").asInt())
            "Fridag" -> opprettMedInntekt(dag)
            "ForeldetDag" -> opprettMedInntekt(dag)
            "NavDag" -> opprett(dag, dag.path("dagsats").asInt().daglig, dag.path("grad").asDouble(), dag.path("utbetaling").asInt())
            "NavHelgDag" -> opprett(dag, grad = dag.path("grad").asDouble())
            "UkjentDag" -> opprett(dag)
            else -> throw IllegalArgumentException("ukjent utbetalingsdagstype: ${dag.path("type").asText()}")
        }
    }

    private fun opprett(dag: ObjectNode, dagsats: Inntekt = INGEN, grad: Double = 0.0, utbetaling: Int = 0) {
        val dagsatsDouble = dagsats.reflection { _, _, daglig, _ -> daglig}
        dag.remove("dagsats")
        dag.remove("utbetaling")
        dag.remove("grad")
        dag.put("grad", grad.takeUnless(Double::isNaN) ?: 0.0)
        dag.put("arbeidsgiverBetalingProsent", 100.0)
        dag.put("aktuellDagsinntekt", dagsatsDouble)
        dag.put("dekningsgrunnlag", dagsatsDouble)
        dag.put("arbeidsgiverbeløp", utbetaling)
        dag.put("personbeløp", 0)
        dag.put("er6GBegrenset", false)
    }

    private fun opprettMedInntekt(
        dag: ObjectNode,
        grad: Double = 0.0,
        utbetaling: Int = 0
    ):Unit = TODO()//opprett(dag, inntekthistorikk.inntekt(dag.dato) ?: INGEN, grad, utbetaling)
}

private val ObjectNode.dato get() = LocalDate.parse(this["dato"].textValue(), ISO_DATE)
