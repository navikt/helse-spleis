package no.nav.helse.økonomi

import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class Inntekt private constructor(private val årlig: Double) : Comparable<Inntekt> {

    init {
        require(
            this.årlig !in listOf(
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.NaN
            )
        ) { "inntekt må være gyldig positivt nummer" }
    }

    companion object {
        private const val ARBEIDSDAGER_PER_ÅR = 260

        internal fun vektlagtGjennomsnitt(parene: List<Pair<Prosentdel, Inntekt>>): Prosentdel {
            val total = parene.sumByDouble { it.second.årlig }
            if (total <= 0.0) return Prosentdel.fraRatio(0.0)
            return Prosentdel.fraRatio(parene.sumByDouble { (it.first.ratio() * it.second.årlig) } / total)
        }

        val Number.månedlig get() = Inntekt(this.toDouble() * 12)

        internal val Number.årlig get() = Inntekt(this.toDouble())

        internal val Number.daglig get() = Inntekt(this.toDouble() * ARBEIDSDAGER_PER_ÅR)

        internal fun List<Inntekt>.summer() = this.fold(INGEN) { acc, inntekt -> acc + inntekt }

        internal fun List<Inntekt>.årligGjennomsnitt(): Inntekt {
            if (this.isEmpty()) return INGEN
            return this.summer() / 12
        }

        internal fun Map<String, Inntekt>.konverter() = this.mapValues { it.value.tilDagligDouble() }

        internal val INGEN = 0.daglig
    }

    internal fun <R> reflection(block: (Double, Double, Double, Int) -> R) = block(
        årlig,
        tilMånedligDouble(),
        tilDagligDouble(),
        tilDagligInt()
    )

    private fun tilDagligInt() = (rundTilDaglig().årlig / ARBEIDSDAGER_PER_ÅR).roundToInt()

    private fun tilDagligDouble() = årlig / ARBEIDSDAGER_PER_ÅR

    private fun tilMånedligDouble() = årlig / 12

    internal fun rundTilDaglig() = Inntekt((årlig / ARBEIDSDAGER_PER_ÅR).roundToInt() * ARBEIDSDAGER_PER_ÅR.toDouble())

    internal operator fun times(scalar: Number) = Inntekt(this.årlig * scalar.toDouble())

    internal operator fun times(prosentdel: Prosentdel) = times(prosentdel.ratio())

    internal operator fun div(scalar: Number) = Inntekt(this.årlig / scalar.toDouble())

    internal operator fun div(prosentdel: Prosentdel) = this / prosentdel.ratio()

    internal infix fun ratio(other: Inntekt) = this.årlig / other.årlig

    internal operator fun plus(other: Inntekt) = Inntekt(this.årlig + other.årlig)

    internal operator fun minus(other: Inntekt) = Inntekt(this.årlig - other.årlig)

    override fun hashCode() = årlig.hashCode()

    override fun equals(other: Any?) = other is Inntekt && this.equals(other)

    private fun equals(other: Inntekt) = this.årlig == other.årlig

    override fun compareTo(other: Inntekt) = if (this == other) 0 else this.årlig.compareTo(other.årlig)

    override fun toString(): String {
        return "[Årlig: $årlig, Månedlig: ${tilMånedligDouble()}, Daglig: ${tilDagligDouble()}]"
    }

    internal fun juster(block: (Int, Inntekt) -> Unit) {
        tilDagligInt().absoluteValue.also {
            if (it == 0) return
            block(it, (tilDagligInt() / it).daglig)
        }
    }

    internal fun avviksprosent(other: Inntekt) =
        Prosent.ratio((this.årlig - other.årlig).absoluteValue / other.årlig)
}
