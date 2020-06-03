package no.nav.helse.økonomi

import no.nav.helse.Grunnbeløp
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag
import java.time.LocalDate
import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.NaN
import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.math.roundToInt

internal class Økonomi private constructor(
    private val grad: Prosentdel,
    private val arbeidsgiverBetalingProsent: Prosentdel,
    private var aktuellDagsinntekt: Double? = null,
    private var dekningsgrunnlag: Double? = null,
    private var arbeidsgiverbeløp: Int? = null,
    private var personbeløp: Int? = null,
    private var er6GBegrenset: Boolean? = null,
    private var tilstand: Tilstand = Tilstand.KunGrad()
) {

    companion object {

        internal fun sykdomsgrad(grad: Prosentdel, arbeidsgiverBetalingProsent: Prosentdel = 100.prosent) =
            Økonomi(grad, arbeidsgiverBetalingProsent)

        internal fun arbeidshelse(grad: Prosentdel, arbeidsgiverBetalingProsent: Prosentdel = 100.prosent) =
            Økonomi(!grad, arbeidsgiverBetalingProsent)

        internal fun ikkeBetalt() = sykdomsgrad(0.prosent)

        internal fun sykdomsgrad(økonomiList: List<Økonomi>) =
            Prosentdel.vektlagtGjennomsnitt(økonomiList.map { it.grad() to it.dekningsgrunnlag() })

        internal fun betal(økonomiList: List<Økonomi>, dato: LocalDate): List<Økonomi> = økonomiList.also {
            delteUtbetalinger(it)
            justereForGrense(it, maksbeløp(it, dato))
        }

        private fun maksbeløp(økonomiList: List<Økonomi>, dato: LocalDate) =
            (Grunnbeløp.`6G`.dagsats(dato) * sykdomsgrad(økonomiList).ratio()).roundToInt()

        private fun delteUtbetalinger(økonomiList: List<Økonomi>) = økonomiList.forEach { it.betal() }

        private fun justereForGrense(økonomiList: List<Økonomi>, grense: Int) {
            val totalArbeidsgiver = totalArbeidsgiver(økonomiList)
            val totalPerson = totalPerson(økonomiList)
            when {
                (totalArbeidsgiver + totalPerson <= grense).also {
                    økonomiList.forEach { økonomi ->
                        økonomi.er6GBegrenset = !it
                    }
                } -> return
                totalArbeidsgiver <= grense -> justerPerson(økonomiList, totalPerson, grense - totalArbeidsgiver)
                else -> {
                    justerArbeidsgiver(økonomiList, totalArbeidsgiver, grense)
                    tilbakestillPerson(økonomiList)
                }
            }
        }

        private fun justerPerson(økonomiList: List<Økonomi>, total: Int, budsjett: Int) {
            val ratio = budsjett.toDouble() / total
            økonomiList.forEach {
                it.personbeløp = (it.personbeløp!! * ratio).toInt()
            }
            (budsjett - totalPerson(økonomiList)).also { remainder ->
                if (remainder == 0) return
                (0..(remainder - 1)).forEach { index ->
                    økonomiList[index].personbeløp =
                        økonomiList[index].personbeløp!! + 1
                }
            }
            require(budsjett == totalPerson(økonomiList))
        }

        private fun justerArbeidsgiver(økonomiList: List<Økonomi>, total: Int, budsjett: Int) {
            val ratio = budsjett.toDouble() / total
            økonomiList.forEach {
                it.arbeidsgiverbeløp = (it.arbeidsgiverbeløp!! * ratio).toInt()
            }
            (budsjett - totalArbeidsgiver(økonomiList)).also { remainder ->
                if (remainder == 0) return
                (0..(remainder - 1)).forEach { index ->
                    økonomiList[index].arbeidsgiverbeløp =
                        økonomiList[index].arbeidsgiverbeløp!! + 1
                }
            }
            require(budsjett == totalArbeidsgiver(økonomiList))
        }

        private fun tilbakestillPerson(økonomiList: List<Økonomi>) =
            økonomiList.forEach { it.personbeløp = 0 }

        private fun totalArbeidsgiver(økonomiList: List<Økonomi>): Int = økonomiList.sumBy {
            it.arbeidsgiverbeløp ?: throw IllegalStateException("utbetalinger ennå ikke beregnet")
        }

        private fun totalPerson(økonomiList: List<Økonomi>): Int = økonomiList.sumBy {
            it.personbeløp ?: throw IllegalStateException("utbetalinger ennå ikke beregnet")
        }

        internal fun erUnderInntektsgrensen(økonomiList: List<Økonomi>, alder: Alder, dato: LocalDate): Boolean {
            return økonomiList.sumByDouble { it.dekningsgrunnlag() } < alder.minimumInntekt(dato)
        }

        internal fun er6GBegrenset(økonomiList: List<Økonomi>) =
            økonomiList.fold(false) { result, økonomi -> result || økonomi.er6GBegrenset() }
    }

    internal fun inntekt(aktuellDagsinntekt: Number, dekningsgrunnlag: Number = aktuellDagsinntekt): Økonomi =
        dekningsgrunnlag.toDouble().let {
            require(it >= 0) { "dekningsgrunnlag kan ikke være negativ" }
            require(
                it !in listOf(
                    POSITIVE_INFINITY,
                    NEGATIVE_INFINITY,
                    NaN
                )
            ) { "dekningsgrunnlag må være gyldig positivt nummer" }
            tilstand.inntekt(this, aktuellDagsinntekt.toDouble(), it)
        }

    internal fun lås() = tilstand.lås(this)

    internal fun låsOpp() = tilstand.låsOpp(this)

    internal fun toMap(): Map<String, Any> = tilstand.toMap(this)

    internal fun toIntMap(): Map<String, Any> = tilstand.toIntMap(this)

    @Deprecated("Temporary visibility until Utbetalingstidslinje has Økonomi support")
    internal fun dekningsgrunnlag() =
        dekningsgrunnlag ?: throw IllegalStateException("Dekningsgrunnlag er ikke satt ennå")

    @Deprecated("Temporary visibility until Utbetalingstidslinje has Økonomi support")
    internal fun grad() = tilstand.grad(this)

    @Deprecated("Temporary visibility until Utbetalingstidslinje has Økonomi support")
    internal fun arbeidsgiverbeløp() = tilstand.arbeidsgiverbeløp(this)

    private fun betal() = this.also { tilstand.betal(this) }

    internal fun er6GBegrenset() = tilstand.er6GBegrenset(this)

    private fun _betal() {
        val total = dekningsgrunnlag() * grad().ratio()
        (total * arbeidsgiverBetalingProsent.ratio()).roundToInt().also {
            arbeidsgiverbeløp = it
            personbeløp = total.roundToInt() - it
        }
    }

    private fun utbetalingMap() = mapOf(
        "arbeidsgiverbeløp" to arbeidsgiverbeløp!!,
        "personbeløp" to personbeløp!!,
        "er6GBegrenset" to er6GBegrenset!!
    )

    internal fun accept(visitor: UtbetalingsdagVisitor, dag: NavDag, dato: LocalDate) =
        visitor.visit(dag, dato, this, grad, dekningsgrunnlag, arbeidsgiverbeløp, personbeløp)

    internal sealed class Tilstand {

        internal open fun grad(økonomi: Økonomi) = økonomi.grad

        internal open fun inntekt(
            økonomi: Økonomi,
            aktuellDagsinntekt: Double,
            dekningsgrunnlag: Double
        ): Økonomi {
            throw IllegalStateException("Kan ikke sette inntekt på dette tidspunktet")
        }

        internal open fun betal(økonomi: Økonomi) {
            throw IllegalStateException("Kan ikke beregne utbetaling på dette tidspunktet")
        }

        internal open fun er6GBegrenset(økonomi: Økonomi): Boolean {
            throw IllegalStateException("Beløp er ikke beregnet ennå")
        }

        internal open fun arbeidsgiverbeløp(økonomi: Økonomi): Int {
            throw IllegalStateException("Arbeidsgiverbeløp er ikke beregnet ennå")
        }

        internal open fun lås(økonomi: Økonomi): Økonomi {
            throw IllegalStateException("Kan ikke låse Økonomi på dette tidspunktet")
        }


        internal open fun låsOpp(økonomi: Økonomi): Økonomi {
            throw IllegalStateException("Kan ikke låse opp Økonomi på dette tidspunktet")
        }

        internal open fun toMap(økonomi: Økonomi): Map<String, Any> = mapOf(
            "grad" to økonomi.grad.toDouble(),   // Must use instance value here
            "arbeidsgiverBetalingProsent" to økonomi.arbeidsgiverBetalingProsent.toDouble()
        )

        internal open fun toIntMap(økonomi: Økonomi): Map<String, Any> = mapOf(
            "grad" to økonomi.grad.roundToInt(),   // Must use instance value here
            "arbeidsgiverBetalingProsent" to økonomi.arbeidsgiverBetalingProsent.roundToInt()
        )

        internal class KunGrad : Tilstand() {

            override fun inntekt(
                økonomi: Økonomi,
                aktuellDagsinntekt: Double,
                dekningsgrunnlag: Double
            ) =
                Økonomi(økonomi.grad, økonomi.arbeidsgiverBetalingProsent, aktuellDagsinntekt, dekningsgrunnlag)
                    .also { other -> other.tilstand = HarInntekt() }

        }

        internal class HarInntekt : Tilstand() {

            override fun lås(økonomi: Økonomi) = økonomi.also {
                it.tilstand = Låst()
            }

            override fun toMap(økonomi: Økonomi) = super.toMap(økonomi) + mapOf(
                "dekningsgrunnlag" to økonomi.dekningsgrunnlag!!,
                "aktuellDagsinntekt" to økonomi.aktuellDagsinntekt!!
            )

            override fun toIntMap(økonomi: Økonomi) = super.toIntMap(økonomi) + mapOf(
                "dekningsgrunnlag" to økonomi.dekningsgrunnlag!!.roundToInt(),
                "aktuellDagsinntekt" to økonomi.aktuellDagsinntekt!!.roundToInt()
            )

            override fun betal(økonomi: Økonomi) {
                økonomi._betal()
                økonomi.tilstand = HarBeløp()
            }
        }

        internal class HarBeløp : Tilstand() {

            override fun arbeidsgiverbeløp(økonomi: Økonomi) = økonomi.arbeidsgiverbeløp!!

            override fun er6GBegrenset(økonomi: Økonomi) = økonomi.er6GBegrenset!!

            override fun toMap(økonomi: Økonomi) =
                super.toMap(økonomi) +
                    mapOf(
                        "dekningsgrunnlag" to økonomi.dekningsgrunnlag!!,
                        "aktuellDagsinntekt" to økonomi.aktuellDagsinntekt!!
                    ) +
                    økonomi.utbetalingMap()

            override fun toIntMap(økonomi: Økonomi) =
                super.toIntMap(økonomi) +
                    mapOf(
                        "dekningsgrunnlag" to økonomi.dekningsgrunnlag!!.roundToInt(),
                        "aktuellDagsinntekt" to økonomi.aktuellDagsinntekt!!.roundToInt()
                    ) +
                    økonomi.utbetalingMap()
        }

        internal class Låst : Tilstand() {

            override fun grad(økonomi: Økonomi) = 0.prosent

            override fun låsOpp(økonomi: Økonomi) = økonomi.also { økonomi ->
                økonomi.tilstand = HarInntekt()
            }

            override fun lås(økonomi: Økonomi) = økonomi // Okay to lock twice

            override fun toMap(økonomi: Økonomi) =
                super.toMap(økonomi) + mapOf(
                    "dekningsgrunnlag" to økonomi.dekningsgrunnlag!!,
                    "aktuellDagsinntekt" to økonomi.aktuellDagsinntekt!!
                )

            override fun toIntMap(økonomi: Økonomi) =
                super.toIntMap(økonomi) + mapOf(
                    "dekningsgrunnlag" to økonomi.dekningsgrunnlag!!.roundToInt(),
                    "aktuellDagsinntekt" to økonomi.aktuellDagsinntekt!!.roundToInt()
                )

            override fun betal(økonomi: Økonomi) {
                økonomi.arbeidsgiverbeløp = 0
                økonomi.personbeløp = 0
                økonomi.tilstand = LåstMedBeløp()
            }
        }

        internal class LåstMedBeløp : Tilstand() {

            override fun grad(økonomi: Økonomi) = 0.prosent

            override fun lås(økonomi: Økonomi) = økonomi // Okay to lock twice

            override fun låsOpp(økonomi: Økonomi) = økonomi.also { økonomi ->
                økonomi.tilstand = HarInntekt()
            }

            override fun arbeidsgiverbeløp(økonomi: Økonomi) = økonomi.arbeidsgiverbeløp!!

            override fun er6GBegrenset(økonomi: Økonomi) = false

            override fun toMap(økonomi: Økonomi) =
                super.toMap(økonomi) +
                    mapOf(
                        "dekningsgrunnlag" to økonomi.dekningsgrunnlag!!,
                        "aktuellDagsinntekt" to økonomi.aktuellDagsinntekt!!
                    ) +
                    økonomi.utbetalingMap()

            override fun toIntMap(økonomi: Økonomi) =
                super.toIntMap(økonomi) +
                    mapOf(
                        "dekningsgrunnlag" to økonomi.dekningsgrunnlag!!.roundToInt(),
                        "aktuellDagsinntekt" to økonomi.aktuellDagsinntekt!!.roundToInt()
                    ) +
                    økonomi.utbetalingMap()
        }
    }
}

internal fun List<Økonomi>.sykdomsgrad(): Prosentdel = Økonomi.sykdomsgrad(this)

internal fun List<Økonomi>.betal(dato: LocalDate) = Økonomi.betal(this, dato)

internal fun List<Økonomi>.erUnderInntekstgrensen(
    alder: Alder,
    dato: LocalDate
) = Økonomi.erUnderInntektsgrensen(this, alder, dato)

internal fun List<Økonomi>.er6GBegrenset() = Økonomi.er6GBegrenset(this)
