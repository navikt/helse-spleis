package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.Grunnbeløp
import no.nav.helse.serde.migration.Inntektshistorikk.Inntektsendring.Kilde.INNTEKTSMELDING
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_DATE
import java.util.*

internal class V18UtbetalingstidslinjeØkonomi : JsonMigration(version = 18) {
    override val description = "utvide økonomifelt i Utbetalingstidslinjer"

    private lateinit var inntektshistorikk: Inntektshistorikk

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
        inntektshistorikk = inntekthistorikk(dager)
        dager.forEach { dag ->
            opprettØkonomi(dag as ObjectNode)
        }
    }

    private fun inntekthistorikk(dager: JsonNode) = Inntektshistorikk().also { historikk ->
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
    ) = opprett(dag, inntektshistorikk.inntekt(dag.dato) ?: INGEN, grad, utbetaling)
}

private val ObjectNode.dato get() = LocalDate.parse(this["dato"].textValue(), ISO_DATE)

private class Inntektshistorikk(private val inntekter: MutableList<Inntektsendring> = mutableListOf()) {

    fun add(fom: LocalDate, hendelseId: UUID, beløp: Inntekt, kilde: Inntektsendring.Kilde, tidsstempel: LocalDateTime = LocalDateTime.now()) {
        val nyInntekt = Inntektsendring(fom, hendelseId, beløp, kilde, tidsstempel)
        inntekter.removeAll { it.erRedundantMed(nyInntekt) }
        inntekter.add(nyInntekt)
        inntekter.sort()
    }

    fun inntekt(skjæringstidspunkt: LocalDate) = Inntektsendring.inntekt(inntekter, skjæringstidspunkt)

    fun dekningsgrunnlag(skjæringstidspunkt: LocalDate, regler: ArbeidsgiverRegler): Inntekt =
        inntekt(skjæringstidspunkt)?.times(regler.dekningsgrad()) ?: INGEN

    class Inntektsendring(
        private val fom: LocalDate,
        private val hendelseId: UUID,
        private val beløp: Inntekt,
        private val kilde: Kilde,
        private val tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : Comparable<Inntektsendring> {

        companion object {
            private fun inntektendring(inntekter: List<Inntektsendring>, skjæringstidspunkt: LocalDate) =
                (inntekter.lastOrNull { it.fom <= skjæringstidspunkt } ?: inntekter.firstOrNull())

            internal fun inntekt(inntekter: List<Inntektsendring>, skjæringstidspunkt: LocalDate) =
                inntektendring(inntekter, skjæringstidspunkt)?.beløp

            internal fun sykepengegrunnlag(inntekter: List<Inntektsendring>, skjæringstidspunkt: LocalDate, virkningFra: LocalDate = LocalDate.now()): Inntekt? =
                inntekt(inntekter, skjæringstidspunkt)?.let {
                    listOf(it, Grunnbeløp.`6G`.beløp(skjæringstidspunkt, virkningFra)).minOrNull()
                }
        }

        override fun compareTo(other: Inntektsendring) =
            this.fom.compareTo(other.fom).let {
                if (it == 0) this.kilde.compareTo(other.kilde)
                else it
            }

        internal fun erRedundantMed(annenInntektsendring: Inntektsendring) =
            annenInntektsendring.fom == fom && annenInntektsendring.kilde == kilde

        //Order is significant, compare is used to prioritize records from various sources
        internal enum class Kilde : Comparable<Kilde> {
            SKATT, INFOTRYGD, INNTEKTSMELDING
        }
    }
}
