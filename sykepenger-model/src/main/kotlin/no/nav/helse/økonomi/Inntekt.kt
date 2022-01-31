package no.nav.helse.økonomi

import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import java.time.LocalDate
import kotlin.math.absoluteValue
import kotlin.math.floor
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
        //8-10 ledd 3
        private const val ARBEIDSDAGER_PER_ÅR = 260

        internal fun vektlagtGjennomsnitt(parene: List<Pair<Prosentdel, Inntekt>>): Prosentdel {
            val total = parene.sumOf { it.second.årlig }
            if (total <= 0.0) return Prosentdel.fraRatio(parene.map { it.first.ratio() }.average())
            return Prosentdel.fraRatio(parene.sumOf { (it.first.ratio() * it.second.årlig) } / total)
        }

        val Number.månedlig get() = Inntekt(this.toDouble() * 12)

        internal val Number.årlig get() = Inntekt(this.toDouble())

        val Number.daglig get() = Inntekt(this.toDouble() * ARBEIDSDAGER_PER_ÅR)

        fun Number.daglig(grad: Prosentdel) = this.daglig / grad

        internal fun List<Inntekt>.summer() = this.fold(INGEN) { acc, inntekt -> acc + inntekt }

        internal val INGEN = 0.daglig
    }

    internal fun <R> reflection(block: (årlig: Double, månedlig: Double, daglig: Double, dagligInt: Int) -> R) = block(
        årlig,
        tilMånedligDouble(),
        tilDagligDouble(),
        tilDagligInt()
    )

    private fun tilDagligInt() = (rundTilDaglig().årlig / ARBEIDSDAGER_PER_ÅR).roundToInt()

    private fun tilDagligDouble() = årlig / ARBEIDSDAGER_PER_ÅR

    private fun tilMånedligDouble() = årlig / 12

    internal fun rundTilDaglig() = Inntekt((årlig / ARBEIDSDAGER_PER_ÅR).roundToInt() * ARBEIDSDAGER_PER_ÅR.toDouble())

    internal fun rundNedTilDaglig() = Inntekt(floor(tilDagligDouble()) * ARBEIDSDAGER_PER_ÅR)

    internal fun dekningsgrunnlag(dagen: LocalDate, regler: ArbeidsgiverRegler, subsumsjonObserver: SubsumsjonObserver): Inntekt {
        val dekningsgrunnlag = Inntekt(this.årlig * regler.dekningsgrad())
        subsumsjonObserver.`§ 8-16 ledd 1`(dagen, regler.dekningsgrad(), this.årlig, dekningsgrunnlag.årlig)
        return dekningsgrunnlag
    }

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

    internal fun avviksprosent(other: Inntekt) =
        Prosent.ratio((this.årlig - other.årlig).absoluteValue / other.årlig)
}
