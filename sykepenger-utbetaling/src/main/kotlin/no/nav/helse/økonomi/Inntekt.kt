package no.nav.helse.økonomi

import java.time.LocalDate
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.dto.InntektDto
import no.nav.helse.memoize
import no.nav.helse.økonomi.Prosentdel.Companion.average
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

        fun vektlagtGjennomsnitt(parene: List<Pair<Prosentdel, Inntekt>>, total: Inntekt): Prosentdel {
            return parene.map { it.first to it.second.årlig }.average(total.årlig)
        }

        fun fraGradert(inntekt: Inntekt, grad: Prosentdel): Inntekt {
            return grad.gradér(inntekt.tilDagligDouble()).daglig
        }

        val Number.månedlig get() = Inntekt(this.toDouble() * 12)

        val Number.årlig get() = Inntekt(this.toDouble())

        val Number.daglig get() = Inntekt(this.toDouble() * ARBEIDSDAGER_PER_ÅR)

        fun Collection<Inntekt>.summer() = this.fold(INGEN) { acc, inntekt -> acc + inntekt }

        val INGEN = 0.daglig

        private val tilDagligDoubleMemoized = { tall: Double -> tall / ARBEIDSDAGER_PER_ÅR }.memoize()
        private val tilMånedligDoubleMemoized = { tall: Double -> tall / 12 }.memoize()
        private val tilDagligIntMemoized = { tall: Double -> tilDagligDoubleMemoized(tall).toInt() }
        private val rundTilDagligMemoized = { tall: Double -> tilDagligDoubleMemoized(tall).roundToInt().daglig }.memoize()
        private val rundNedTilDagligMemoized = { tall: Double -> tilDagligIntMemoized(tall).daglig }.memoize()
    }

    fun <R> reflection(block: (årlig: Double, månedlig: Double, daglig: Double, dagligInt: Int) -> R) = block(
        årlig,
        tilMånedligDouble(),
        tilDagligDouble(),
        tilDagligInt()
    )

    private fun tilDagligInt() = tilDagligIntMemoized(årlig)
    private fun tilDagligDouble() = tilDagligDoubleMemoized(årlig)
    private fun tilMånedligDouble() = tilMånedligDoubleMemoized(årlig)
    fun rundTilDaglig() = rundTilDagligMemoized(årlig)
    fun rundNedTilDaglig() = rundNedTilDagligMemoized(årlig)

    fun dekningsgrunnlag(dagen: LocalDate, regler: DekningsgradKilde, subsumsjonObserver: SubsumsjonObserver): Inntekt {
        val dekningsgrunnlag = Inntekt(this.årlig * regler.dekningsgrad())
        subsumsjonObserver.`§ 8-16 ledd 1`(dagen, regler.dekningsgrad(), this.årlig, dekningsgrunnlag.årlig)
        return dekningsgrunnlag
    }

    operator fun times(scalar: Number) = Inntekt(this.årlig * scalar.toDouble())

    operator fun times(prosentdel: Prosentdel) = prosentdel.times(this.årlig).årlig

    operator fun div(scalar: Number) = Inntekt(this.årlig / scalar.toDouble())

    infix fun ratio(other: Inntekt) = Prosentdel.ratio(this.årlig, other.årlig)

    operator fun plus(other: Inntekt) = Inntekt(this.årlig + other.årlig)

    operator fun minus(other: Inntekt) = Inntekt(this.årlig - other.årlig)

    override fun hashCode() = årlig.hashCode()

    override fun equals(other: Any?) = other is Inntekt && this.equals(other)

    private fun equals(other: Inntekt) = this.årlig == other.årlig

    override fun compareTo(other: Inntekt) = if (this == other) 0 else this.årlig.compareTo(other.årlig)

    override fun toString(): String {
        return "[Årlig: $årlig, Månedlig: ${tilMånedligDouble()}, Daglig: ${tilDagligDouble()}]"
    }

    fun avviksprosent(other: Inntekt) = Avviksprosent.avvik(this.årlig, other.årlig)

    fun dto() = InntektDto(
        årlig = this.årlig,
        månedligDouble = tilMånedligDouble(),
        dagligDouble = tilDagligDouble(),
        dagligInt = tilDagligInt()
    )
}

interface DekningsgradKilde {
    fun dekningsgrad(): Double
}