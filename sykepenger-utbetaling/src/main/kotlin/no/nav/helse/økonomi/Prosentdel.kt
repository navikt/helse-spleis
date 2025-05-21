package no.nav.helse.økonomi

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.roundToInt
import no.nav.helse.dto.ProsentdelDto
import no.nav.helse.etterlevelse.Subsumsjonslogg

class Prosentdel private constructor(private val brøkdel: BigDecimal) : Comparable<Prosentdel> {
    init {
        require(brøkdel.toDouble() in 0.0..1.0) {
            "Må være prosent mellom 0 og 100 var ${brøkdel.toDouble()}"
        }
    }

    companion object {
        private val mc = MathContext.DECIMAL128
        private val SIKKER_BRØK = 1.0.toBigDecimal(mc)
        private val HUNDRE_PROSENT = 100.0.toBigDecimal(mc)
        private val GRENSE = 20.prosent
        private val EPSILON = BigDecimal("0.00001")

        internal fun ratio(a: Double, b: Double) =
            Prosentdel(if (a < b) a.toBigDecimal(mc).divide(b.toBigDecimal(mc), mc) else SIKKER_BRØK)

        fun subsumsjon(subsumsjonslogg: Subsumsjonslogg, block: Subsumsjonslogg.(Double) -> Unit) {
            subsumsjonslogg.block(GRENSE.toDouble())
        }

        internal fun Collection<Pair<Prosentdel, Double>>.average(inntektjustering: Double): Prosentdel {
            return map { it.first to it.second.toBigDecimal(mc) }.average(inntektjustering.toBigDecimal(mc))
        }

        private fun Collection<Pair<Prosentdel, BigDecimal>>.average(inntektjustering: BigDecimal): Prosentdel {
            val total = this.sumOf { it.second }
            require(total > BigDecimal.ZERO) { "Kan ikke dele på 0" }
            val inntekter = this.sumOf { it.first.brøkdel.multiply(it.second, mc) }
            val teller = inntekter - inntektjustering.coerceAtMost(inntekter)
            val ratio = teller.divide(total, mc)
            return Prosentdel(ratio)
        }
        val Number.prosent get() = Prosentdel(this.toDouble().toBigDecimal(mc).divide(HUNDRE_PROSENT, mc))

        fun gjenopprett(dto: ProsentdelDto) = dto.prosent.prosent
    }

    override fun equals(other: Any?) = other is Prosentdel && this.equals(other)

    private fun equals(other: Prosentdel) = (this.brøkdel - other.brøkdel).abs() < EPSILON

    override fun hashCode() = brøkdel.hashCode()

    operator fun not() = Prosentdel(SIKKER_BRØK - this.brøkdel)

    internal operator fun div(other: Prosentdel) = Prosentdel(this.brøkdel.divide(other.brøkdel, mc))

    override fun compareTo(other: Prosentdel) = if (this.equals(other)) 0 else this.brøkdel.compareTo(other.brøkdel)

    override fun toString(): String {
        return "${(toDouble())} %"
    }

    internal fun gradér(beløp: Double) = beløp.toBigDecimal(mc).divide(this.brøkdel, mc).toDouble().roundToInt().toDouble()

    internal fun times(other: Double) = (other.toBigDecimal(mc) * brøkdel).toDouble()

    fun toDouble() = brøkdel.multiply(HUNDRE_PROSENT, mc).toDouble()

    internal fun erUnderGrensen() = this < GRENSE

    fun dto() = ProsentdelDto(prosent = toDouble())
}
