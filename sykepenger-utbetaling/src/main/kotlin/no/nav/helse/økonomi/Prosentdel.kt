package no.nav.helse.økonomi

import java.math.BigDecimal
import java.math.MathContext
import no.nav.helse.etterlevelse.SubsumsjonObserver
import kotlin.math.roundToInt

class Prosentdel private constructor(private val brøkdel: BigDecimal): Comparable<Prosentdel> {
    init {
        require(brøkdel.toDouble() in 0.0..1.0) {
            "Må være prosent mellom 0 og 100"
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

        fun subsumsjon(subsumsjonObserver: SubsumsjonObserver, block: SubsumsjonObserver.(Double) -> Unit) {
            subsumsjonObserver.block(GRENSE.toDouble())
        }

        internal fun Collection<Pair<Prosentdel, Double>>.average(total: Double): Prosentdel {
            return map { it.first to it.second.toBigDecimal(mc) }.average(total.toBigDecimal(mc))
        }

        private fun Collection<Pair<Prosentdel, BigDecimal>>.average(total: BigDecimal): Prosentdel {
            if (total <= BigDecimal.ZERO) return map { it.first to BigDecimal.ONE }.average(size.toBigDecimal())
            val teller = this.sumOf { it.first.not().brøkdel.multiply(it.second, mc) }
            val totalInntektsbevaringsgrad = teller.divide(total, mc).coerceAtMost(BigDecimal.ONE)
            return Prosentdel(totalInntektsbevaringsgrad).not()
        }

        val Number.prosent get() = Prosentdel(this.toDouble().toBigDecimal(mc).divide(HUNDRE_PROSENT, mc))
    }

    override fun equals(other: Any?) = other is Prosentdel && this.equals(other)

    private fun equals(other: Prosentdel) = (this.brøkdel - other.brøkdel).abs() < EPSILON

    override fun hashCode() = brøkdel.hashCode()

    operator fun not() = Prosentdel(SIKKER_BRØK - this.brøkdel)

    internal operator fun div(other: Prosentdel) = Prosentdel(this.brøkdel.divide(other.brøkdel, mc))

    override fun compareTo(other: Prosentdel) = if(this.equals(other)) 0 else this.brøkdel.compareTo(other.brøkdel)

    override fun toString(): String {
        return "${(toDouble())} %"
    }

    internal fun gradér(beløp: Double) = beløp.toBigDecimal(mc).divide(this.brøkdel, mc).toDouble().roundToInt().toDouble()

    internal fun times(other: Double) = (other.toBigDecimal(mc) * brøkdel).toDouble()

    fun toDouble() = brøkdel.multiply(HUNDRE_PROSENT, mc).toDouble()

    internal fun erUnderGrensen() = this < GRENSE
}