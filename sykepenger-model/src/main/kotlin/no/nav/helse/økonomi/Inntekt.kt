package no.nav.helse.økonomi

import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class Inntekt : Comparable<Inntekt>{

    private val årlig: Double

    private constructor(årlig: Double) {
        this.årlig = årlig
        require(
            this.årlig !in listOf(
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.NaN
            )
        ) { "inntekt må være gyldig positivt nummer" }
    }

    companion object {
        internal fun vektlagtGjennomsnitt(parene: List<Pair<Prosentdel, Inntekt>>): Prosentdel {
            val total = parene.sumByDouble { it.second.årlig }
            if (total <= 0.0) return Prosentdel.fraRatio(0.0)
            return Prosentdel.fraRatio(parene.sumByDouble { (it.first.ratio() * it.second.årlig) } / total)
        }

        val Number.månedlig get() = Inntekt(this.toDouble() * 12)
        internal val Number.årlig get() = Inntekt(this.toDouble())
        internal val Number.daglig get() = Inntekt(this.toDouble() * 260)

        internal fun List<Inntekt>.summer(): Inntekt {
            return this.reduce { acc, inntekt -> Inntekt(acc.årlig + inntekt.årlig) }
        }

        internal fun Map<String, Any>.konverter(): Map<String, Any> {
            return this
                .toMutableMap()
                .also {
                    it.forEach { (key, value) ->
                        if (value is Inntekt) it[key] = value.tilDagligDouble()
                    }
                }
                .toMap()
        }
    }

    internal fun tilDagligInt() = (rundTilDaglig().årlig / 260).roundToInt()

    internal fun tilMånedligDouble() = årlig / 12.0

    internal fun tilÅrligDouble() = årlig

    internal fun rundTilDaglig() = Inntekt((årlig / 260).roundToInt() * 260.0)

    internal operator fun times(scalar: Number) = Inntekt(this.årlig * scalar.toDouble())

    internal operator fun div(prosentdel: Prosentdel) = Inntekt(this.årlig / prosentdel.ratio())

    internal operator fun div(other: Inntekt) = this.årlig / other.årlig

    internal operator fun minus(other: Inntekt) = Inntekt(this.årlig - other.årlig)

    internal operator fun plus(other: Inntekt) = Inntekt(this.årlig + other.årlig)

    internal operator fun times(prosentdel: Prosentdel) = times(prosentdel.ratio())

    override fun hashCode() = årlig.hashCode()

    override fun equals(other: Any?) = other is Inntekt && this.equals(other)

    private fun equals(other: Inntekt) = this.årlig == other.årlig

    override fun compareTo(other: Inntekt) = if (this == other) 0 else this.årlig.compareTo(other.årlig)

    internal fun tilDagligDouble() = årlig / 260.0

    override fun toString(): String {
        return "[Årlig: $årlig, Måndelig: ${tilMånedligDouble()}, Daglig: ${tilDagligDouble()}]"
    }

    internal fun juster(block: (Int, Inntekt) -> Unit) {
        tilDagligInt().absoluteValue.also {
            if(it == 0) return
            block(it, (tilDagligInt() / it).daglig)
        }
    }
}
