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
    private val aktuellDagsinntekt: Inntekt? = null,
    private val dekningsgrunnlag: Inntekt? = null,
    private val skjæringstidspunkt: LocalDate? = null,
    private var totalGrad: Prosentdel? = null,
    private var arbeidsgiverbeløp: Inntekt? = null,
    private var personbeløp: Inntekt? = null,
    private var er6GBegrenset: Boolean? = null,
    private var tilstand: Tilstand = Tilstand.KunGrad,
) {

    companion object {
        internal val arbeidsgiverBeløp = { økonomi: Økonomi -> økonomi.arbeidsgiverbeløp!! }
        internal val personBeløp = { økonomi: Økonomi -> økonomi.personbeløp!! }

        internal fun sykdomsgrad(grad: Prosentdel, arbeidsgiverBetalingProsent: Prosentdel = 100.prosent) =
            Økonomi(grad, arbeidsgiverBetalingProsent)

        internal fun ikkeBetalt() = sykdomsgrad(0.prosent)

        internal fun totalSykdomsgrad(økonomiList: List<Økonomi>) =
            Inntekt.vektlagtGjennomsnitt(økonomiList.map { it.grad() to it.dekningsgrunnlag!! })

        internal fun betal(økonomiList: List<Økonomi>, virkningsdato: LocalDate): List<Økonomi> = økonomiList.also {
            delteUtbetalinger(it)
            økonomiList.forEach { it.totalGrad = totalSykdomsgrad(økonomiList) }
            fordelBeløp(
                it,
                maksbeløp(requireNotNull(it.firstOrNull { it.skjæringstidspunkt != null }) { "Fant ingen økonomiobjekter med skjæringstidspunkt" },
                    virkningsdato
                )
            )
        }

        private fun maksbeløp(økonomi: Økonomi, virkningsdato: LocalDate) =
            (Grunnbeløp.`6G`.dagsats(økonomi.skjæringstidspunkt!!, virkningsdato) * økonomi.totalGrad!!).rundTilDaglig()

        private fun delteUtbetalinger(økonomiList: List<Økonomi>) = økonomiList.forEach { it.betal() }

        private fun fordelBeløp(økonomiList: List<Økonomi>, grense: Inntekt) {
            val totalArbeidsgiver = totalArbeidsgiver(økonomiList)
            val totalPerson = totalPerson(økonomiList)

            økonomiList.forEach { økonomi -> økonomi.er6GBegrenset = totalArbeidsgiver + totalPerson > grense }

            fordelArbeidsgiverbeløp(økonomiList, totalArbeidsgiver, grense)
            fordelPersonbeløp(økonomiList, totalPerson + totalArbeidsgiver - totalArbeidsgiver(økonomiList), grense - totalArbeidsgiver(økonomiList), grense)
        }

        private fun fordelPersonbeløp(økonomiList: List<Økonomi>, total: Inntekt, budsjett: Inntekt, grense: Inntekt) {
            val beregningsresultat = beregnUtbetalingFørAvrunding(
                økonomiList,
                total,
                budsjett,
            ){ it.personbeløp!! }

            beregningsresultat.forEach { it.økonomi.personbeløp = it.utbetalingEtterAvrunding() }

            val totaltRestbeløp =
                (total.coerceAtMost(budsjett) - totalPerson(økonomiList))
                    .reflection { _, _, _, dagligInt -> dagligInt }

            beregningsresultat
                .sortedByDescending { it.differanse() }
                .take(totaltRestbeløp)
                .forEach { it.økonomi.personbeløp = it.økonomi.personbeløp!! + 1.daglig }
        }

        private fun fordelArbeidsgiverbeløp(økonomiList: List<Økonomi>, total: Inntekt, budsjett: Inntekt) {
            val beregningsresultat = beregnUtbetalingFørAvrunding(
                økonomiList,
                total,
                budsjett
            ) { it.arbeidsgiverbeløp!! }

            beregningsresultat.forEach { it.økonomi.arbeidsgiverbeløp = it.utbetalingEtterAvrunding() }

            val totaltRestbeløp =
                (total.coerceAtMost(budsjett) - totalArbeidsgiver(økonomiList))
                    .reflection { _, _, _, dagligInt -> dagligInt }

            beregningsresultat
                .sortedByDescending { it.differanse() }
                .take(totaltRestbeløp)
                .forEach { it.økonomi.arbeidsgiverbeløp = it.økonomi.arbeidsgiverbeløp!! + 1.daglig }
        }

        private fun beregnUtbetalingFørAvrunding(
            økonomiList: List<Økonomi>,
            total: Inntekt,
            budsjett: Inntekt,
            get: (Økonomi) -> Inntekt,
        ): List<Beregningsresultat> {
            if (total <= 0.daglig) {
                return økonomiList.map { Beregningsresultat(økonomi = it, utbetalingFørAvrunding = 0.daglig) }
            }

            val ratio = budsjett ratio total

            return økonomiList
                .map {
                    val utbetalingFørAvrunding = get(it) * ratio.coerceAtMost(1.0)
                    Beregningsresultat(
                        økonomi = it,
                        utbetalingFørAvrunding = utbetalingFørAvrunding
                    )
                }
        }

        data class Beregningsresultat(
            internal val økonomi: Økonomi,
            internal val utbetalingFørAvrunding: Inntekt,
        ) {
            internal fun differanse() = (utbetalingFørAvrunding - utbetalingEtterAvrunding()).reflection { _, _, daglig, _ -> daglig }
            internal fun utbetalingEtterAvrunding() = utbetalingFørAvrunding.rundNedTilDaglig()
        }

        private fun totalArbeidsgiver(økonomiList: List<Økonomi>): Inntekt =
            økonomiList
                .map { it.arbeidsgiverbeløp ?: throw IllegalStateException("utbetalinger ennå ikke beregnet") }
                .summer()

        private fun totalPerson(økonomiList: List<Økonomi>): Inntekt =
            økonomiList
                .map { it.personbeløp ?: throw IllegalStateException("utbetalinger ennå ikke beregnet") }
                .summer()

        private fun totalGradertDekningsgrunnlag(økonomiList: List<Økonomi>) =
            totalArbeidsgiver(økonomiList) + totalPerson(økonomiList)

        internal fun erUnderInntektsgrensen(økonomiList: List<Økonomi>, alder: Alder, dato: LocalDate): Boolean {
            return økonomiList.map { it.aktuellDagsinntekt!! }.summer() < alder.minimumInntekt(dato)
        }

        internal fun er6GBegrenset(økonomiList: List<Økonomi>) =
            økonomiList.any { it.er6GBegrenset() }
    }

    internal fun inntekt(aktuellDagsinntekt: Inntekt, dekningsgrunnlag: Inntekt = aktuellDagsinntekt, skjæringstidspunkt: LocalDate): Økonomi =
        dekningsgrunnlag.let {
            require(it >= INGEN) { "dekningsgrunnlag kan ikke være negativ" }
            tilstand.inntekt(this, aktuellDagsinntekt, it, skjæringstidspunkt)
        }

    internal fun lås() = tilstand.lås(this)

    internal fun låsOpp() = tilstand.låsOpp(this)

    internal fun <R> reflection(
        block: (
            grad: Double,
            arbeidsgiverBetalingProsent: Double,
            dekningsgrunnlag: Double?,
            skjæringstidspunkt: LocalDate?,
            totalGrad: Double?,
            aktuellDagsinntekt: Double?,
            arbeidsgiverbeløp: Double?,
            personbeløp: Double?,
            er6GBegrenset: Boolean?
        ) -> R
    ) =
        tilstand.reflection(this, block)

    internal fun <R> reflectionRounded(
        block: (
            grad: Int,
            arbeidsgiverBetalingProsent: Int,
            dekningsgrunnlag: Int?,
            aktuellDagsinntekt: Int?,
            arbeidsgiverbeløp: Int?,
            personbeløp: Int?,
            er6GBegrenset: Boolean?
        ) -> R
    ) =
        reflection { grad: Double,
                     arbeidsgiverBetalingProsent: Double,
                     dekningsgrunnlag: Double?,
                     _: LocalDate?,
                     _: Double?,
                     aktuellDagsinntekt: Double?,
                     arbeidsgiverbeløp: Double?,
                     personbeløp: Double?,
                     er6GBegrenset: Boolean? ->
            block(
                grad.roundToInt(),
                arbeidsgiverBetalingProsent.roundToInt(),
                dekningsgrunnlag?.roundToInt(),
                aktuellDagsinntekt?.roundToInt(),
                arbeidsgiverbeløp?.roundToInt(),
                personbeløp?.roundToInt(),
                er6GBegrenset
            )
        }

    internal fun reflection(block: (grad: Double, aktuellDagsinntekt: Double?) -> Unit) {
        reflection { grad: Double,
                     _: Double,
                     _: Double?,
                     _: LocalDate?,
                     _: Double?,
                     aktuellDagsinntekt: Double?,
                     _: Double?,
                     _: Double?,
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

    private fun <R> beløpReflection(
        block: (
            grad: Double,
            arbeidsgiverBetalingProsent: Double,
            dekningsgrunnlag: Double?,
            skjæringstidspunkt: LocalDate?,
            totalGrad: Double?,
            aktuellDagsinntekt: Double?,
            arbeidsgiverbeløp: Double?,
            personbeløp: Double?,
            er6GBegrenset: Boolean?
        ) -> R
    ) =
        block(
            grad.toDouble(),
            arbeidsgiverBetalingProsent.toDouble(),
            dekningsgrunnlag!!.reflection { _, _, daglig, _ -> daglig },
            skjæringstidspunkt,
            totalGrad?.toDouble(),
            aktuellDagsinntekt!!.reflection { _, _, daglig, _ -> daglig },
            arbeidsgiverbeløp!!.reflection { _, _, daglig, _ -> daglig },
            personbeløp!!.reflection { _, _, daglig, _ -> daglig },
            er6GBegrenset
        )

    private fun <R> inntektReflection(
        block: (
            grad: Double,
            arbeidsgiverBetalingProsent: Double,
            dekningsgrunnlag: Double?,
            skjæringstidspunkt: LocalDate?,
            totalGrad: Double?,
            aktuellDagsinntekt: Double?,
            arbeidsgiverbeløp: Double?,
            personbeløp: Double?,
            er6GBegrenset: Boolean?
        ) -> R
    ) =
        block(
            grad.toDouble(),
            arbeidsgiverBetalingProsent.toDouble(),
            dekningsgrunnlag!!.reflection { _, _, daglig, _ -> daglig },
            skjæringstidspunkt,
            totalGrad?.toDouble(),
            aktuellDagsinntekt!!.reflection { _, _, daglig, _ -> daglig },
            null, null, null
        )

    private fun grad() = tilstand.grad(this)

    private fun betal() = this.also { tilstand.betal(this) }

    internal fun er6GBegrenset() = tilstand.er6GBegrenset(this)

    private fun _betal() {
        val total = dekningsgrunnlag!! * grad().ratio()
        (total * arbeidsgiverBetalingProsent.ratio()).also {
            arbeidsgiverbeløp = it
            personbeløp = total - arbeidsgiverbeløp!!
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

    internal fun accept(visitor: UtbetalingsdagVisitor, dag: Arbeidsdag, dato: LocalDate) =
        visitor.visit(dag, dato, this)

    internal fun accept(
        visitor: SykdomstidslinjeVisitor,
        dag: Dag.Arbeidsgiverdag,
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) =
        visitor.visitDag(dag, dato, this, kilde)

    internal fun accept(
        visitor: SykdomstidslinjeVisitor,
        dag: Dag.ArbeidsgiverHelgedag,
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) =
        visitor.visitDag(dag, dato, this, kilde)

    internal fun accept(
        visitor: SykdomstidslinjeVisitor,
        dag: Dag.Sykedag,
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) =
        visitor.visitDag(dag, dato, this, kilde)

    internal fun accept(
        visitor: SykdomstidslinjeVisitor,
        dag: Dag.SykHelgedag,
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
        visitor.visitDag(dag, dato, this, kilde)

    internal sealed class Tilstand {

        internal open fun grad(økonomi: Økonomi) = økonomi.grad

        internal abstract fun <R> reflection(
            økonomi: Økonomi,
            block: (
                grad: Double,
                arbeidsgiverBetalingProsent: Double,
                dekningsgrunnlag: Double?,
                skjæringstidspunkt: LocalDate?,
                totalGrad: Double?,
                aktuellDagsinntekt: Double?,
                arbeidsgiverbeløp: Double?,
                personbeløp: Double?,
                er6GBegrenset: Boolean?
            ) -> R
        ): R

        internal open fun inntekt(
            økonomi: Økonomi,
            aktuellDagsinntekt: Inntekt,
            dekningsgrunnlag: Inntekt,
            skjæringstidspunkt: LocalDate?
        ): Økonomi {
            throw IllegalStateException("Kan ikke sette inntekt i tilstand ${this::class.simpleName}")
        }

        internal open fun betal(økonomi: Økonomi) {
            throw IllegalStateException("Kan ikke beregne utbetaling i tilstand ${this::class.simpleName}")
        }

        internal open fun er6GBegrenset(økonomi: Økonomi): Boolean {
            throw IllegalStateException("Beløp er ikke beregnet ennå")
        }

        internal open fun lås(økonomi: Økonomi): Økonomi {
            throw IllegalStateException("Kan ikke låse Økonomi i tilstand ${this::class.simpleName}")
        }

        internal open fun låsOpp(økonomi: Økonomi): Økonomi {
            throw IllegalStateException("Kan ikke låse opp Økonomi i tilstand ${this::class.simpleName}")
        }

        internal object KunGrad : Tilstand() {

            override fun inntekt(
                økonomi: Økonomi,
                aktuellDagsinntekt: Inntekt,
                dekningsgrunnlag: Inntekt,
                skjæringstidspunkt: LocalDate?
            ) =
                Økonomi(økonomi.grad, økonomi.arbeidsgiverBetalingProsent, aktuellDagsinntekt, dekningsgrunnlag, skjæringstidspunkt)
                    .also { other -> other.tilstand = HarInntekt }

            override fun lås(økonomi: Økonomi) = økonomi

            override fun <R> reflection(
                økonomi: Økonomi,
                block: (
                    grad: Double,
                    arbeidsgiverBetalingProsent: Double,
                    dekningsgrunnlag: Double?,
                    skjæringstidspunkt: LocalDate?,
                    totalGrad: Double?,
                    aktuellDagsinntekt: Double?,
                    arbeidsgiverbeløp: Double?,
                    personbeløp: Double?,
                    er6GBegrenset: Boolean?
                ) -> R
            ) =
                block(
                    økonomi.grad.toDouble(),
                    økonomi.arbeidsgiverBetalingProsent.toDouble(),
                    null, null, null, null, null, null, null
                )
        }

        internal object HarInntekt : Tilstand() {

            override fun lås(økonomi: Økonomi) = økonomi.also {
                it.tilstand = Låst
            }

            override fun <R> reflection(
                økonomi: Økonomi,
                block: (
                    grad: Double,
                    arbeidsgiverBetalingProsent: Double,
                    dekningsgrunnlag: Double?,
                    skjæringstidspunkt: LocalDate?,
                    totalGrad: Double?,
                    aktuellDagsinntekt: Double?,
                    arbeidsgiverbeløp: Double?,
                    personbeløp: Double?,
                    er6GBegrenset: Boolean?
                ) -> R
            ) = økonomi.inntektReflection(block)

            override fun betal(økonomi: Økonomi) {
                økonomi._betal()
                økonomi.tilstand = HarBeløp
            }
        }

        internal object HarBeløp : Tilstand() {

            override fun er6GBegrenset(økonomi: Økonomi) = økonomi.er6GBegrenset!!

            override fun betal(økonomi: Økonomi) {
                økonomi._betal()
            }

            override fun <R> reflection(
                økonomi: Økonomi,
                block: (
                    grad: Double,
                    arbeidsgiverBetalingProsent: Double,
                    dekningsgrunnlag: Double?,
                    skjæringstidspunkt: LocalDate?,
                    totalGrad: Double?,
                    aktuellDagsinntekt: Double?,
                    arbeidsgiverbeløp: Double?,
                    personbeløp: Double?,
                    er6GBegrenset: Boolean?
                ) -> R
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
                block: (
                    grad: Double,
                    arbeidsgiverBetalingProsent: Double,
                    dekningsgrunnlag: Double?,
                    skjæringstidspunkt: LocalDate?,
                    totalGrad: Double?,
                    aktuellDagsinntekt: Double?,
                    arbeidsgiverbeløp: Double?,
                    personbeløp: Double?,
                    er6GBegrenset: Boolean?
                ) -> R
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

            override fun betal(økonomi: Økonomi) {}

            override fun er6GBegrenset(økonomi: Økonomi) = false

            override fun <R> reflection(
                økonomi: Økonomi,
                block: (
                    grad: Double,
                    arbeidsgiverBetalingProsent: Double,
                    dekningsgrunnlag: Double?,
                    skjæringstidspunkt: LocalDate?,
                    totalGrad: Double?,
                    aktuellDagsinntekt: Double?,
                    arbeidsgiverbeløp: Double?,
                    personbeløp: Double?,
                    er6GBegrenset: Boolean?
                ) -> R
            ) = økonomi.beløpReflection(block)
        }
    }
}

internal fun List<Økonomi>.totalSykdomsgrad(): Prosentdel = Økonomi.totalSykdomsgrad(this)

internal fun List<Økonomi>.betal(virkningsdato: LocalDate) = Økonomi.betal(this, virkningsdato)

internal fun List<Økonomi>.erUnderInntekstgrensen(
    alder: Alder,
    dato: LocalDate
) = Økonomi.erUnderInntektsgrensen(this, alder, dato)

internal fun List<Økonomi>.er6GBegrenset() = Økonomi.er6GBegrenset(this)
