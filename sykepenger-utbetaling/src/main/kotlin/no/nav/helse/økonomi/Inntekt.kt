package no.nav.helse.økonomi

import kotlin.math.roundToInt
import no.nav.helse.dto.InntektDto
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.økonomi.Prosentdel.Companion.average

class Inntekt private constructor(val årlig: Double) : Comparable<Inntekt> {

    init {
        require(
            this.årlig !in listOf(
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.NaN
            )
        ) { "inntekt må være gyldig positivt nummer" }
    }

    val månedlig = årlig / 12
    val daglig = årlig / ARBEIDSDAGER_PER_ÅR
    val dagligInt = daglig.toInt()

    fun rundTilDaglig() = daglig.roundToInt().daglig
    fun rundNedTilDaglig() = dagligInt.daglig

    companion object {
        //8-10 ledd 3
        private const val ARBEIDSDAGER_PER_ÅR = 260

        fun vektlagtGjennomsnitt(parene: List<Pair<Prosentdel, Inntekt>>, tilkommet: Inntekt, total: Inntekt): Prosentdel {
            return parene.map { it.first to it.second.årlig }.average(tilkommet.årlig, total.årlig)
        }

        fun fraGradert(inntekt: Inntekt, grad: Prosentdel): Inntekt {
            return grad.gradér(inntekt.daglig).daglig
        }

        val Number.K get() = this.toDouble() * 1000

        val Number.månedlig get() = Inntekt(this.toDouble() * 12)

        val Number.årlig get() = Inntekt(this.toDouble())

        val Number.daglig get() = Inntekt(this.toDouble() * ARBEIDSDAGER_PER_ÅR)

        fun Collection<Inntekt>.summer() = this.fold(INGEN) { acc, inntekt -> acc + inntekt }

        val INGEN = 0.daglig

        fun gjenopprett(dto: InntektbeløpDto): Inntekt {
            return when (dto) {
                is InntektbeløpDto.Årlig -> Inntekt(dto.beløp)
                is InntektbeløpDto.DagligDouble -> dto.beløp.daglig
                is InntektbeløpDto.DagligInt -> dto.beløp.daglig
                is InntektbeløpDto.MånedligDouble -> dto.beløp.månedlig
            }
        }
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
        return "[Årlig: $årlig, Månedlig: ${månedlig}, Daglig: ${daglig}]"
    }

    fun dto() = InntektDto(
        årlig = dtoÅrlig(),
        månedligDouble = dtoMånedligDouble(),
        dagligInt = dtoDagligInt(),
        dagligDouble = dtoDagligDouble()
    )

    private fun dtoÅrlig() = InntektbeløpDto.Årlig(this.årlig)
    fun dtoMånedligDouble() = InntektbeløpDto.MånedligDouble(månedlig)
    private fun dtoDagligDouble() = InntektbeløpDto.DagligDouble(daglig)
    private fun dtoDagligInt() = InntektbeløpDto.DagligInt(dagligInt)
}

interface DekningsgradKilde {
    fun dekningsgrad(): Double
}
