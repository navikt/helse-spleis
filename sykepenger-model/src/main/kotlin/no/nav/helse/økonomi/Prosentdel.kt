package no.nav.helse.økonomi

import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.roundToLong

internal class Prosentdel private constructor(private val brøkdel: Double): Comparable<Prosentdel> {

    internal constructor(beløp: Number) : this(beløp.toDouble() / 100.0)

    init {
        require(brøkdel.toDouble() in 0.0..1.0) { "Må være prosent mellom 0 og 100" }
    }

    companion object {
        private const val EPSILON = 0.000001
        private const val SIKKER_BRØK = 1.0

        internal fun vektlagtGjennomsnitt(parene: List<Pair<Prosentdel, Double>>): Prosentdel {
            val total = parene.sumByDouble { it.second }
            return Prosentdel(parene.sumByDouble { it.first.brøkdel * it.second } / total)
        }
    }

    override fun equals(other: Any?) = other is Prosentdel && this.equals(other)

    private fun equals(other: Prosentdel) =
        (this.brøkdel - other.brøkdel).absoluteValue < EPSILON

    override fun hashCode() = (brøkdel / EPSILON).roundToLong().hashCode()

    internal operator fun not() = Prosentdel(SIKKER_BRØK - brøkdel)

    override fun compareTo(other: Prosentdel) =
        if (this.equals(other)) 0
        else this.brøkdel.compareTo(other.brøkdel)

    override fun toString(): String {
        return "${(brøkdel * 100).roundToInt()}%"
    }

    internal fun ratio() = brøkdel

    internal fun toDouble() = brøkdel * 100.0

    internal fun roundToInt() = toDouble().roundToInt()
}

internal val Number.prosent get() = Prosentdel(this)
