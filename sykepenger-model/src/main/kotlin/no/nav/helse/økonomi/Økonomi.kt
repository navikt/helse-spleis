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
    private var dagsats: Double? = null,
    private var arbeidsgiversutbetaling: Int? = null,
    private var personUtbetaling: Int? = null,
    private var tilstand: Tilstand = Tilstand.KunGrad()
) {

    companion object {

        internal fun sykdomsgrad(grad: Prosentdel, arbeidsgiverBetalingProsent: Prosentdel = 100.prosent) =
            Økonomi(grad, arbeidsgiverBetalingProsent)

        internal fun arbeidshelse(grad: Prosentdel, arbeidsgiverBetalingProsent: Prosentdel = 100.prosent) =
            Økonomi(!grad, arbeidsgiverBetalingProsent)

        internal fun ikkeSyke() = sykdomsgrad(0.prosent)

        internal fun samletGrad(økonomiList: List<Økonomi>) =
            Prosentdel.vektlagtGjennomsnitt(økonomiList.map { it.grad() to it.dagsats() })

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

    internal fun dagsats(beløp: Number): Økonomi =
        beløp.toDouble().let {
            require(it >= 0) { "dagsats kan ikke være negativ" }
            require(it !in listOf(
                POSITIVE_INFINITY,
                NEGATIVE_INFINITY,
                NaN
            )) { "dagsats må være gyldig positivt nummer" }
            tilstand.dagsats(this, it)
        }

    internal fun lås() = tilstand.lås(this)

    internal fun låsOpp() = tilstand.låsOpp(this)

    internal fun toMap(): Map<String, Any> = tilstand.toMap(this)

    internal fun toIntMap(): Map<String, Any> = tilstand.toIntMap(this)

    @Deprecated("Temporary visibility until Utbetalingstidslinje has Økonomi support")
    internal fun dagsats() = dagsats ?: throw IllegalStateException("Dagsats er ikke satt ennå")

    @Deprecated("Temporary visibility until Utbetalingstidslinje has Økonomi support")
    internal fun grad() = tilstand.grad(this)

    private fun betale() = this.also { tilstand.betale(this) }

    private fun _betale() {
        val total = dagsats() * grad().ratio()
        (total * arbeidsgiverBetalingProsent.ratio()).roundToInt().also {
            arbeidsgiversutbetaling = it
            personUtbetaling = total.roundToInt() - it
        }
    }

    private fun utbetalingMap() = mapOf(
        "arbeidsgiversutbetaling" to arbeidsgiversutbetaling!!,
        "personUtbetaling" to personUtbetaling!!
    )

    internal sealed class Tilstand {

        internal open fun grad(økonomi: Økonomi) = økonomi.grad

        internal open fun dagsats(økonomi: Økonomi, beløp: Double): Økonomi {
            throw IllegalStateException("Kan ikke sette dagsats på dette tidspunktet")
        }

        internal open fun betale(økonomi: Økonomi) {
            throw IllegalStateException("utbetalingen er ikke beregnet ennå")
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

        internal class KunGrad: Tilstand() {

            override fun dagsats(økonomi: Økonomi, beløp: Double) =
                Økonomi(økonomi.grad, økonomi.arbeidsgiverBetalingProsent, beløp)
                    .also { other -> other.tilstand = HarDagsats() }

        }

        internal class HarDagsats: Tilstand() {

            override fun lås(økonomi: Økonomi) = økonomi.also {
                it.tilstand = Låst()
            }

            override fun toMap(økonomi: Økonomi) = super.toMap(økonomi) + mapOf(
                "dagsats" to økonomi.dagsats()
            )

            override fun toIntMap(økonomi: Økonomi) = super.toIntMap(økonomi) + mapOf(
                "dagsats" to økonomi.dagsats().roundToInt()
            )

            override fun betale(økonomi: Økonomi) {
                økonomi._betale()
                økonomi.tilstand = HarUtbetatlinger()
            }
        }

        internal class HarUtbetatlinger: Tilstand() {

            override fun toMap(økonomi: Økonomi) =
                super.toMap(økonomi) +
                    mapOf("dagsats" to økonomi.dagsats()) +
                    økonomi.utbetalingMap()

            override fun toIntMap(økonomi: Økonomi) =
                super.toIntMap(økonomi) +
                    mapOf("dagsats" to økonomi.dagsats().roundToInt()) +
                    økonomi.utbetalingMap()
        }

        internal class Låst: Tilstand() {

            override fun grad(økonomi: Økonomi) = 0.prosent

            override fun låsOpp(økonomi: Økonomi) = økonomi.also { økonomi ->
                økonomi.tilstand = HarDagsats()
            }

            override fun lås(økonomi: Økonomi) = økonomi // Okay to lock twice

            override fun toMap(økonomi: Økonomi) =
                super.toMap(økonomi) + mapOf("dagsats" to økonomi.dagsats())

            override fun toIntMap(økonomi: Økonomi) =
                super.toIntMap(økonomi) + mapOf("dagsats" to økonomi.dagsats().roundToInt())

            override fun betale(økonomi: Økonomi) {
                økonomi.arbeidsgiversutbetaling = 0
                økonomi.personUtbetaling = 0
                økonomi.tilstand = LåstMedUtbetling()
            }
        }

        internal class LåstMedUtbetling: Tilstand() {

            override fun grad(økonomi: Økonomi) = 0.prosent

            override fun lås(økonomi: Økonomi) = økonomi // Okay to lock twice

            override fun låsOpp(økonomi: Økonomi) = økonomi.also { økonomi ->
                økonomi.tilstand = HarDagsats()
            }

            override fun toMap(økonomi: Økonomi) =
                super.toMap(økonomi) +
                    mapOf("dagsats" to økonomi.dagsats()) +
                    økonomi.utbetalingMap()

            override fun toIntMap(økonomi: Økonomi) =
                super.toIntMap(økonomi) +
                    mapOf("dagsats" to økonomi.dagsats().roundToInt()) +
                    økonomi.utbetalingMap()
        }
    }
}

internal fun List<Økonomi>.samletGrad(): Prosentdel = Økonomi.samletGrad(this)

internal fun List<Økonomi>.betale(dato: LocalDate) = Økonomi.betale(this, dato)
