package no.nav.helse.økonomi

import no.nav.helse.Grunnbeløp
import java.time.LocalDate
import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.NaN
import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.math.roundToInt

internal class Økonomi private constructor(
    private val grad: Prosentdel,
    private val arbeidsgiverBetalingProsent: Prosentdel,
    private var lønn: Double? = null,
    private var arbeidsgiversutbetaling: Int? = null,
    private var personUtbetaling: Int? = null,
    private var tilstand: Tilstand = Tilstand.KunGrad()
) {

    companion object {
        private val GRENSE = 20.prosent

        internal fun sykdomsgrad(grad: Prosentdel, arbeidsgiverBetalingProsent: Prosentdel = 100.prosent) =
            Økonomi(grad, arbeidsgiverBetalingProsent)

        internal fun arbeidshelse(grad: Prosentdel, arbeidsgiverBetalingProsent: Prosentdel = 100.prosent) =
            Økonomi(!grad, arbeidsgiverBetalingProsent)

        internal fun samletGrad(økonomiList: List<Økonomi>) =
            Prosentdel.vektlagtGjennomsnitt(økonomiList.map { it.grad to it.lønn() })

        internal fun betale(økonomiList: List<Økonomi>, dato: LocalDate): List<Økonomi> = økonomiList.also {
            delteUtbetalinger(it)
            justereForGrense(it, utbetalingsgrense(it, dato))
        }

        private fun utbetalingsgrense(økonomiList: List<Økonomi>, dato: LocalDate) =
            (Grunnbeløp.`6G`.dagsats(dato) * samletGrad(økonomiList).ratio()).roundToInt()

        private fun delteUtbetalinger(økonomiList: List<Økonomi>) = økonomiList.forEach { it.betale() }

        private fun justereForGrense(økonomiList: List<Økonomi>, grense: Int) {
            val totalArbeidsgiver = totalArbeidsgiver(økonomiList)
            val totalPerson = totalPerson(økonomiList)
            when {
                totalArbeidsgiver + totalPerson <= grense -> return
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
                it.personUtbetaling = (it.personUtbetaling!! * ratio).toInt()
            }
            (budsjett - totalPerson(økonomiList)).also { remainder ->
                if (remainder == 0) return
                (0..(remainder - 1)).forEach {
                        index -> økonomiList[index].personUtbetaling =
                    økonomiList[index].personUtbetaling!! + 1
                }
            }
            require(budsjett == totalPerson(økonomiList))
        }

        private fun justerArbeidsgiver(økonomiList: List<Økonomi>, total: Int,  budsjett: Int) {
            val ratio = budsjett.toDouble() / total
            økonomiList.forEach {
                it.arbeidsgiversutbetaling = (it.arbeidsgiversutbetaling!! * ratio).toInt()
            }
            (budsjett - totalArbeidsgiver(økonomiList)).also { remainder ->
                if (remainder == 0) return
                (0..(remainder - 1)).forEach {
                        index -> økonomiList[index].arbeidsgiversutbetaling =
                    økonomiList[index].arbeidsgiversutbetaling!! + 1
                }            }
            require(budsjett == totalArbeidsgiver(økonomiList))
        }

        private fun tilbakestillPerson(økonomiList: List<Økonomi>) =
            økonomiList.forEach { it.personUtbetaling = 0 }

        private fun totalArbeidsgiver(økonomiList: List<Økonomi>): Int = økonomiList.sumBy {
            it.arbeidsgiversutbetaling ?: throw IllegalStateException("utbetalinger ennå ikke beregnet") }

        private fun totalPerson(økonomiList: List<Økonomi>): Int = økonomiList.sumBy {
            it.personUtbetaling ?: throw IllegalStateException("utbetalinger ennå ikke beregnet") }
    }

    internal fun erUnderGrensen() = grad.compareTo(GRENSE) < 0

    internal fun lønn(beløp: Number): Økonomi {
        beløp.toDouble().also {
            require(it >= 0) { "lønn kan ikke være negativ" }
            require(it !in listOf(
                POSITIVE_INFINITY,
                NEGATIVE_INFINITY,
                NaN
            )) { "lønn må være gyldig positivt nummer" }
            tilstand.lønn(this, it)
        }
        return this
    }

    internal fun toMap(): Map<String, Any> = tilstand.toMap(this)

    internal fun toIntMap(): Map<String, Any> = tilstand.toIntMap(this)

    internal fun lønn() = lønn ?: throw IllegalStateException("Lønn er ikke satt ennå")

    private fun betale() = this.also { tilstand.betale(this) }

    private fun _betale() {
        val total = lønn() * grad.ratio()
        (total * arbeidsgiverBetalingProsent.ratio()).roundToInt().also {
            arbeidsgiversutbetaling = it
            personUtbetaling = total.roundToInt() - it
        }
    }

    internal abstract sealed class Tilstand {

        internal open fun lønn(økonomi: Økonomi, beløp: Double) {
            throw IllegalStateException("Forsøk å stille lønn igjen")
        }
        internal open fun betale(økonomi: Økonomi) {
            throw IllegalStateException("utbetalingen er ikke beregnet ennå")
        }

        internal open fun toMap(økonomi: Økonomi): Map<String, Any> = mapOf(
            "grad" to økonomi.grad.toDouble(),
            "arbeidsgiverBetalingProsent" to økonomi.arbeidsgiverBetalingProsent.toDouble()
        )

        internal open fun toIntMap(økonomi: Økonomi): Map<String, Any> = mapOf(
            "grad" to økonomi.grad.roundToInt(),
            "arbeidsgiverBetalingProsent" to økonomi.arbeidsgiverBetalingProsent.roundToInt()
        )

        internal class KunGrad: Tilstand() {

            override fun lønn(økonomi: Økonomi, beløp: Double) {
                økonomi.lønn = beløp
                økonomi.tilstand = HarLønn()
            }
        }

        internal class HarLønn: Tilstand() {

            override fun toMap(økonomi: Økonomi) = super.toMap(økonomi) + mapOf(
                "lønn" to økonomi.lønn()
            )

            override fun toIntMap(økonomi: Økonomi) = super.toIntMap(økonomi) + mapOf(
                "lønn" to økonomi.lønn().roundToInt()
            )

            override fun betale(økonomi: Økonomi) {
                økonomi._betale()
                økonomi.tilstand = HarUtbetatlinger()
            }
        }

        internal class HarUtbetatlinger: Tilstand() {

            override fun toMap(økonomi: Økonomi) =
                super.toMap(økonomi) +
                    mapOf("lønn" to økonomi.lønn()) +
                    toUtbetalingMap(økonomi)

            override fun toIntMap(økonomi: Økonomi) =
                super.toIntMap(økonomi) +
                    mapOf("lønn" to økonomi.lønn().roundToInt()) +
                    toUtbetalingMap(økonomi)

            private fun toUtbetalingMap(økonomi: Økonomi) = mapOf(
                "arbeidsgiversutbetaling" to økonomi.arbeidsgiversutbetaling!!,
                "personUtbetaling" to økonomi.personUtbetaling!!
            )
        }

    }
}

internal fun List<Økonomi>.samletGrad(): Prosentdel = Økonomi.samletGrad(this)

internal fun List<Økonomi>.betale(dato: LocalDate) = Økonomi.betale(this, dato)
