package no.nav.helse.sykdomstidslinje

import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.roundToLong

internal class Grad private constructor(private val brøkdel: Double): Comparable<Grad> {

    companion object {
        private const val EPSILON = 0.000001
        private const val SIKKER_BRØK = 1.0
        internal val GRENSE = sykdom(20.0)

        internal fun sykdom(prosentdel: Number) = Grad(prosentdel.toDouble() / 100.0)

        internal fun arbeidshelse(prosentdel: Number) = !sykdom(prosentdel)
    }

    override fun equals(other: Any?) = other is Grad && this.equals(other)

    private fun equals(other: Grad) =
        (this.brøkdel - other.brøkdel).absoluteValue < EPSILON

    override fun hashCode() = (brøkdel / EPSILON).roundToLong().hashCode()

    internal operator fun not() = Grad(SIKKER_BRØK - brøkdel)

    override fun compareTo(other: Grad) =
        if (this.equals(other)) 0
        else this.brøkdel.compareTo(other.brøkdel)

    override fun toString(): String {
        return "${(brøkdel * 100).roundToInt()}%"
    }

    internal fun lønn(beløp: Number) = LønnGrad(this.brøkdel, beløp.toDouble())

    internal class LønnGrad(private val brøkdel: Double, private val beløp: Double) {
        companion object {
            internal fun samletGrad(lønnGrader: List<LønnGrad>): Grad {
                val total = lønnGrader.sumByDouble { it.beløp }
                return Grad(lønnGrader.sumByDouble { it.brøkdel * it.beløp / total } )
            }
        }

    }
}

internal fun List<Grad.LønnGrad>.samletGrad() = Grad.LønnGrad.samletGrad(this)
