package no.nav.helse.økonomi

import no.nav.helse.Grunnbeløp
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.konverter
import no.nav.helse.økonomi.Inntekt.Companion.summer
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import java.time.LocalDate
import kotlin.math.roundToInt

internal class Økonomi private constructor(
    private val grad: Prosentdel,
    private val arbeidsgiverBetalingProsent: Prosentdel,
    private var aktuellDagsinntekt: Inntekt? = null,
    private var dekningsgrunnlag: Inntekt? = null,
    private var arbeidsgiverbeløp: Inntekt? = null,
    private var personbeløp: Inntekt? = null,
    private var er6GBegrenset: Boolean? = null,
    private var tilstand: Tilstand = Tilstand.KunGrad
) {

    companion object {
        internal val arbeidsgiverBeløp = { økonomi: Økonomi -> økonomi.arbeidsgiverbeløp!! }
        internal val personBeløp = { økonomi: Økonomi -> økonomi.personbeløp!! }

        internal fun sykdomsgrad(grad: Prosentdel, arbeidsgiverBetalingProsent: Prosentdel = 100.prosent) =
            Økonomi(grad, arbeidsgiverBetalingProsent)

        internal fun arbeidshelse(grad: Prosentdel, arbeidsgiverBetalingProsent: Prosentdel = 100.prosent) =
            Økonomi(!grad, arbeidsgiverBetalingProsent)

        internal fun ikkeBetalt() = sykdomsgrad(0.prosent)

        internal fun sykdomsgrad(økonomiList: List<Økonomi>) =
            Inntekt.vektlagtGjennomsnitt(økonomiList.map { it.grad() to it.dekningsgrunnlag!! })

        internal fun betal(økonomiList: List<Økonomi>, beregningsdato: LocalDate): List<Økonomi> = økonomiList.also {
            delteUtbetalinger(it)
            justereForGrense(it, maksbeløp(it, beregningsdato))
        }

        private fun maksbeløp(økonomiList: List<Økonomi>, beregningsdato: LocalDate) =
            (Grunnbeløp.`6G`.dagsats(beregningsdato) * sykdomsgrad(økonomiList).roundToTwoDecimalPlaces()).rundTilDaglig()

        private fun delteUtbetalinger(økonomiList: List<Økonomi>) = økonomiList.forEach { it.betal() }

        private fun justereForGrense(økonomiList: List<Økonomi>, grense: Inntekt) {
            val totalArbeidsgiver = totalArbeidsgiver(økonomiList)
            val totalPerson = totalPerson(økonomiList)
            when {
                (totalArbeidsgiver + totalPerson <= grense).also { isUnlimited ->
                    økonomiList.forEach { økonomi -> økonomi.er6GBegrenset = !isUnlimited }
                } -> return
                totalArbeidsgiver <= grense -> justerPerson(økonomiList, totalPerson, grense - totalArbeidsgiver)
                else -> {
                    justerArbeidsgiver(økonomiList, totalArbeidsgiver, grense)
                    tilbakestillPerson(økonomiList)
                }
            }
        }

        private fun justerPerson(økonomiList: List<Økonomi>, total: Inntekt, budsjett: Inntekt) {
            juster(
                økonomiList,
                total,
                budsjett,
                { it.personbeløp!! },
                { it, beløp -> it.personbeløp = beløp }
            )
            require(budsjett == totalPerson(økonomiList)){"budsjett: $budsjett != totalPerson: ${totalPerson(økonomiList)}"}
        }

        private fun justerArbeidsgiver(økonomiList: List<Økonomi>, total: Inntekt, budsjett: Inntekt) {
            juster(
                økonomiList,
                total,
                budsjett,
                { it.arbeidsgiverbeløp!! },
                { it, beløp -> it.arbeidsgiverbeløp = beløp }
            )
            require(budsjett == totalArbeidsgiver(økonomiList)){"budsjett: $budsjett != totalArbeidsgiver: ${totalArbeidsgiver(økonomiList)}"}
        }

        private fun juster(økonomiList: List<Økonomi>, total: Inntekt, budsjett: Inntekt, get: (Økonomi) -> Inntekt, set: (Økonomi, Inntekt) -> Unit) {
            val sorterteØkonomier = økonomiList.sortedByDescending { get(it) }
            val ratio = budsjett ratio total
            val skalertTotal = sorterteØkonomier.onEach {
                set(it, (get(it) * ratio).rundTilDaglig())
            }.map(get).summer()

            (budsjett - skalertTotal).also { remainder ->
                remainder.juster { teller, justeringen ->
                    (0 until teller).forEach { index ->
                        set(sorterteØkonomier[index], get(sorterteØkonomier[index]) + justeringen)
                    }
                }
            }
        }

        private fun tilbakestillPerson(økonomiList: List<Økonomi>) =
            økonomiList.forEach { it.personbeløp = 0.daglig }

        private fun totalArbeidsgiver(økonomiList: List<Økonomi>): Inntekt =
            økonomiList
                .map { it.arbeidsgiverbeløp ?: throw IllegalStateException("utbetalinger ennå ikke beregnet") }
                .summer()

        private fun totalPerson(økonomiList: List<Økonomi>): Inntekt =
            økonomiList
                .map { it.personbeløp ?: throw IllegalStateException("utbetalinger ennå ikke beregnet") }
                .summer()

        internal fun erUnderInntektsgrensen(økonomiList: List<Økonomi>, alder: Alder, dato: LocalDate): Boolean {
            return økonomiList.map{ it.aktuellDagsinntekt!! }.summer() < alder.minimumInntekt(dato)
        }

        internal fun er6GBegrenset(økonomiList: List<Økonomi>) =
            økonomiList.any { it.er6GBegrenset() }
    }

    internal fun inntekt(aktuellDagsinntekt: Inntekt, dekningsgrunnlag: Inntekt = aktuellDagsinntekt): Økonomi =
        dekningsgrunnlag.let {
            require(it >= INGEN) { "dekningsgrunnlag kan ikke være negativ" }
            tilstand.inntekt(this, aktuellDagsinntekt, it)
        }

    internal fun lås() = tilstand.lås(this)

    internal fun låsOpp() = tilstand.låsOpp(this)

    internal fun <R> reflection(block: (Double, Double, Double?, Double?, Int?, Int?, Boolean?) -> R) =
        tilstand.reflection(this, block)

    internal fun <R> reflectionRounded(block: (Int, Int, Int?, Int?, Int?, Int?, Boolean?) -> R) =
        reflection { grad: Double,
                     arbeidsgiverBetalingProsent: Double,
                     dekningsgrunnlag: Double?,
                     aktuellDagsinntekt: Double?,
                     arbeidsgiverbeløp: Int?,
                     personbeløp: Int?,
                     er6GBegrenset: Boolean? ->
            block(
                grad.roundToInt(),
                arbeidsgiverBetalingProsent.roundToInt(),
                dekningsgrunnlag?.roundToInt(),
                aktuellDagsinntekt?.roundToInt(),
                arbeidsgiverbeløp,
                personbeløp,
                er6GBegrenset
            )
        }

    internal fun reflection(block: (Double, Double?) -> Unit) {
        reflection { grad: Double,
                     _: Double,
                     _: Double?,
                     aktuellDagsinntekt: Double?,
                     _: Int?,
                     _: Int?,
                     _: Boolean? ->
            block(grad, aktuellDagsinntekt)
        }
    }

    internal fun reflectionRounded(block: (Int, Int?) -> Unit) {
        reflectionRounded { grad: Int,
                            _: Int,
                            _: Int?,
                            aktuellDagsinntekt: Int?,
                            _: Int?,
                            _: Int?,
                            _: Boolean? ->
            block(grad, aktuellDagsinntekt)
        }
    }

    private fun <R> beløpReflection(block: (Double, Double, Double?, Double?, Int?, Int?, Boolean?) -> R) =
        block(
            grad.toDouble(),
            arbeidsgiverBetalingProsent.toDouble(),
            dekningsgrunnlag!!.reflection { _, _, daglig, _ -> daglig },
            aktuellDagsinntekt!!.reflection { _, _, daglig, _ -> daglig },
            arbeidsgiverbeløp!!.reflection { _, _, _, daglig-> daglig },
            personbeløp!!.reflection { _, _, _, daglig-> daglig },
            er6GBegrenset
        )

    private fun <R> inntektReflection(block: (Double, Double, Double?, Double?, Int?, Int?, Boolean?) -> R) =
        block(
            grad.toDouble(),
            arbeidsgiverBetalingProsent.toDouble(),
            dekningsgrunnlag!!.reflection { _, _, daglig, _ -> daglig },
            aktuellDagsinntekt!!.reflection { _, _, daglig, _ -> daglig },
            null, null, null
        )

    private fun grad() = tilstand.grad(this)

    private fun betal() = this.also { tilstand.betal(this) }

    internal fun er6GBegrenset() = tilstand.er6GBegrenset(this)

    private fun _betal() {
        val total = dekningsgrunnlag!! * grad().ratio()
        (total * arbeidsgiverBetalingProsent.ratio()).rundTilDaglig().also {
            arbeidsgiverbeløp = it
            personbeløp = total.rundTilDaglig() - it
        }
    }

    private fun prosentMap(): Map<String, Any> = mapOf(
        "grad" to grad.toDouble(),   // Must use instance value here
        "arbeidsgiverBetalingProsent" to arbeidsgiverBetalingProsent.toDouble()
    )

    private fun inntektMap(): Map<String, Double> = mapOf(
        "dekningsgrunnlag" to dekningsgrunnlag!!,
        "aktuellDagsinntekt" to aktuellDagsinntekt!!
    ).konverter()

    private fun prosentIntMap(): Map<String, Int> = mapOf(
        "grad" to grad.roundToInt(),   // Must use instance value here
        "arbeidsgiverBetalingProsent" to arbeidsgiverBetalingProsent.roundToInt()
    )

    private fun inntektIntMap(): Map<String, Int> = inntektMap().mapValues { it.value.roundToInt() }

    private fun utbetalingMap() =
        mapOf(
            "arbeidsgiverbeløp" to arbeidsgiverbeløp!!,
            "personbeløp" to personbeløp!!
        )
            .konverter()
            .mapValues { it.value.roundToInt() }
            .toMutableMap<String, Any>()
            .also { it["er6GBegrenset"] = er6GBegrenset!! }

    internal fun accept(visitor: UtbetalingsdagVisitor, dag: NavDag, dato: LocalDate) =
        visitor.visit(dag, dato, this)

    internal fun accept(visitor: UtbetalingsdagVisitor, dag: AvvistDag, dato: LocalDate) =
        visitor.visit(dag, dato, this)

    internal fun accept(visitor: UtbetalingsdagVisitor, dag: NavHelgDag, dato: LocalDate) =
        visitor.visit(dag, dato, this)

    internal fun accept(visitor: UtbetalingsdagVisitor, dag: ArbeidsgiverperiodeDag, dato: LocalDate) =
        visitor.visit(dag, dato, this)

    internal fun accept(visitor: UtbetalingsdagVisitor, dag: AnnullertDag, dato: LocalDate) =
        visitor.visit(dag, dato, this)

    internal fun accept(visitor: UtbetalingsdagVisitor, dag: Arbeidsdag, dato: LocalDate) =
        visitor.visit(dag, dato, this)

    internal fun accept(
        visitor: SykdomstidslinjeVisitor,
        dag: Dag.Arbeidsgiverdag,
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) =
        visitor.visitDag(dag, dato, this, grad, kilde)

    internal fun accept(
        visitor: SykdomstidslinjeVisitor,
        dag: Dag.ArbeidsgiverHelgedag,
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) =
        visitor.visitDag(dag, dato, this, grad, kilde)

    internal fun accept(
        visitor: SykdomstidslinjeVisitor,
        dag: Dag.Sykedag,
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) =
        visitor.visitDag(dag, dato, this, grad, kilde)

    internal fun accept(
        visitor: SykdomstidslinjeVisitor,
        dag: Dag.SykHelgedag,
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) =
        visitor.visitDag(dag, dato, this, grad, kilde)

    internal fun accept(
        visitor: SykdomstidslinjeVisitor,
        dag: Dag.AnnullertDag,
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) =
        visitor.visitDag(dag, dato, this, kilde)

    internal fun accept(
        visitor: SykdomstidslinjeVisitor,
        dag: Dag.ForeldetSykedag,
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) =
        visitor.visitDag(dag, dato, this, grad, kilde)

    internal sealed class Tilstand {

        internal open fun grad(økonomi: Økonomi) = økonomi.grad

        internal abstract fun <R> reflection(
            økonomi: Økonomi,
            block: (Double, Double, Double?, Double?, Int?, Int?, Boolean?) -> R
        ): R

        internal open fun inntekt(
            økonomi: Økonomi,
            aktuellDagsinntekt: Inntekt,
            dekningsgrunnlag: Inntekt
        ): Økonomi {
            throw IllegalStateException("Kan ikke sette inntekt på dette tidspunktet")
        }

        internal open fun betal(økonomi: Økonomi) {
            throw IllegalStateException("Kan ikke beregne utbetaling på dette tidspunktet")
        }

        internal open fun er6GBegrenset(økonomi: Økonomi): Boolean {
            throw IllegalStateException("Beløp er ikke beregnet ennå")
        }

        internal open fun lås(økonomi: Økonomi): Økonomi {
            throw IllegalStateException("Kan ikke låse Økonomi på dette tidspunktet")
        }

        internal open fun låsOpp(økonomi: Økonomi): Økonomi {
            throw IllegalStateException("Kan ikke låse opp Økonomi på dette tidspunktet")
        }

        internal object KunGrad : Tilstand() {

            override fun inntekt(
                økonomi: Økonomi,
                aktuellDagsinntekt: Inntekt,
                dekningsgrunnlag: Inntekt
            ) =
                Økonomi(økonomi.grad, økonomi.arbeidsgiverBetalingProsent, aktuellDagsinntekt, dekningsgrunnlag)
                    .also { other -> other.tilstand = HarInntekt }

            override fun lås(økonomi: Økonomi) = økonomi

            override fun <R> reflection(
                økonomi: Økonomi,
                block: (Double, Double, Double?, Double?, Int?, Int?, Boolean?) -> R
            ) =
                block(
                    økonomi.grad.toDouble(),
                    økonomi.arbeidsgiverBetalingProsent.toDouble(),
                    null, null, null, null, null
                )
        }

        internal object HarInntekt : Tilstand() {

            override fun lås(økonomi: Økonomi) = økonomi.also {
                it.tilstand = Låst
            }

            override fun <R> reflection(
                økonomi: Økonomi,
                block: (Double, Double, Double?, Double?, Int?, Int?, Boolean?) -> R
            ) = økonomi.inntektReflection(block)

            override fun betal(økonomi: Økonomi) {
                økonomi._betal()
                økonomi.tilstand = HarBeløp
            }
        }

        internal object HarBeløp : Tilstand() {

            override fun er6GBegrenset(økonomi: Økonomi) = økonomi.er6GBegrenset!!

            override fun <R> reflection(
                økonomi: Økonomi,
                block: (Double, Double, Double?, Double?, Int?, Int?, Boolean?) -> R
            ) = økonomi.beløpReflection(block)
        }

        internal object Låst : Tilstand() {

            override fun grad(økonomi: Økonomi) = 0.prosent

            override fun lås(økonomi: Økonomi) = økonomi

            override fun låsOpp(økonomi: Økonomi) = økonomi.apply {
                tilstand = HarInntekt
            }

            override fun <R> reflection(
                økonomi: Økonomi,
                block: (Double, Double, Double?, Double?, Int?, Int?, Boolean?) -> R
            ) = økonomi.inntektReflection(block)

            override fun betal(økonomi: Økonomi) {
                økonomi.arbeidsgiverbeløp = 0.daglig
                økonomi.personbeløp = 0.daglig
                økonomi.tilstand = LåstMedBeløp
            }
        }

        internal object LåstMedBeløp : Tilstand() {

            override fun grad(økonomi: Økonomi) = 0.prosent

            override fun lås(økonomi: Økonomi) = økonomi

            override fun låsOpp(økonomi: Økonomi) = økonomi.apply {
                tilstand = HarInntekt
            }

            override fun er6GBegrenset(økonomi: Økonomi) = false

            override fun <R> reflection(
                økonomi: Økonomi,
                block: (Double, Double, Double?, Double?, Int?, Int?, Boolean?) -> R
            ) = økonomi.beløpReflection(block)
        }
    }
}

internal fun List<Økonomi>.sykdomsgrad(): Prosentdel = Økonomi.sykdomsgrad(this)

internal fun List<Økonomi>.betal(beregningsdato: LocalDate) = Økonomi.betal(this, beregningsdato)

internal fun List<Økonomi>.erUnderInntekstgrensen(
    alder: Alder,
    dato: LocalDate
) = Økonomi.erUnderInntektsgrensen(this, alder, dato)

internal fun List<Økonomi>.er6GBegrenset() = Økonomi.er6GBegrenset(this)
