package no.nav.helse.økonomi

import kotlin.math.roundToInt

internal class Inntekt private constructor(private val årlig: Double) : Comparable<Inntekt>{

    companion object {
        internal fun vektlagtGjennomsnitt(parene: List<Pair<Prosentdel, Inntekt>>): Prosentdel {
            val total = parene.sumByDouble { it.second.årlig }
            if (total <= 0.0) return Prosentdel(0)
            return Prosentdel(parene.sumByDouble { it.first.ratio() * it.second.årlig } / total)
        }

        internal val Number.månedlig get() = Inntekt(this.toDouble() * 12)
        internal val Number.årlig get() = Inntekt(this.toDouble())
        internal val Number.daglig get() = Inntekt(this.toDouble() * 260)
    }

    internal fun tilDagligInt() = (rundTilDaglig().årlig / 260).roundToInt()

    internal fun tilMånedligDouble() = årlig / 12.0

    internal fun tilÅrligDouble() = årlig

    internal fun rundTilDaglig() = Inntekt((årlig / 260).roundToInt() * 260.0)

    internal operator fun times(scalar: Number) = Inntekt(this.årlig * scalar.toDouble())

    internal operator fun times(prosentdel: Prosentdel) = times(prosentdel.ratio())

    override fun hashCode() = årlig.hashCode()

    override fun equals(other: Any?) = other is Inntekt && this.equals(other)

    private fun equals(other: Inntekt) = this.årlig == other.årlig

    override fun compareTo(other: Inntekt) = if (this == other) 0 else this.årlig.compareTo(other.årlig)

    internal fun tilDagligDouble() = årlig / 260.0

    override fun toString(): String {
        return "[Årlig: $årlig, Måndelig: ${tilMånedligDouble()}, Daglig: ${tilDagligDouble()}]"
    }
}
