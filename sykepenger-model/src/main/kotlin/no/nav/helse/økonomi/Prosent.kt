package no.nav.helse.økonomi

import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class Prosent private constructor(private val desimal: Double) : Comparable<Prosent> {

    companion object {
        private const val EPSILON = 0.000001
        internal val MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT = 25.prosent

        internal fun ratio(ratio: Double) = Prosent(ratio)
        internal fun prosent(prosent: Double) = Prosent(prosent / 100)

        val Double.ratio get() = ratio(this)
        val Number.prosent get() = prosent(this.toDouble())
    }

    override fun equals(other: Any?) = other is Prosent && this.equals(other)

    private fun equals(other: Prosent) =
        (this.desimal - other.desimal).absoluteValue < EPSILON

    override fun hashCode() = (desimal / EPSILON).roundToLong().hashCode()

    override fun compareTo(other: Prosent) =
        if (this.equals(other)) 0
        else this.desimal.compareTo(other.desimal)

    override fun toString() = "${roundToInt()}%"

    internal fun ratio() = desimal

    internal fun prosent() = desimal * 100.0

    internal fun roundToInt() = prosent().roundToInt()
}
