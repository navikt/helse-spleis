package no.nav.helse.økonomi

import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.NaN
import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.math.roundToInt

internal class Økonomi private constructor(private val lønn: Double, private val grad: Grad) {

    init {
        require(lønn >= 0) { "lønn kan ikke være negativ" }
        require(lønn !in listOf(
            POSITIVE_INFINITY,
            NEGATIVE_INFINITY,
            NaN
        )) { "lønn må være gyldig positivt nummer" }
    }

    companion object {

        internal fun lønn(beløp: Number, grad: Grad) = Økonomi(beløp.toDouble(), grad)

        internal fun samletGrad(økonomier: List<Økonomi>) =
            Grad.vektlagtGjennomsnitt(økonomier.map { it.grad to it.lønn })
    }

    internal fun toMap(): Map<String, Number> =
        mapOf(
            "grad" to grad.toPercentage(),
            "lønn" to lønn
        )

    internal fun toIntMap(): Map<String, Number> =
        mapOf(
            "grad" to grad.roundToInt(),
            "lønn" to lønn.roundToInt()
        )

    internal fun betalte(prosentdel: Prosentdel): Utbetalinger {
        return Utbetalinger(prosentdel)
    }

    internal inner class Utbetalinger(prosentdel: Prosentdel) {
        private val totalUtbetaling = lønn * grad.ratio()
        private val arbeidsgiverutbetaling = (totalUtbetaling * prosentdel.ratio()).roundToInt()

        internal fun arbeidsgiverutbetaling() = arbeidsgiverutbetaling

        internal fun personUtbetaling() = totalUtbetaling.roundToInt() - arbeidsgiverutbetaling

        internal fun toMap(): Map<String, Number> =
            this@Økonomi.toMap() + mapOf(
                "arbeidsgiverutbetaling" to arbeidsgiverutbetaling(),
                "personutbetaling" to personUtbetaling()
            )

        internal fun toIntMap(): Map<String, Number> =
            this@Økonomi.toIntMap() + mapOf(
                "arbeidsgiverutbetaling" to arbeidsgiverutbetaling(),
                "personutbetaling" to personUtbetaling()
            )
    }
}

internal fun List<Økonomi>.samletGrad() = Økonomi.samletGrad(this)

