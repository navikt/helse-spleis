package no.nav.helse.økonomi

import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.BigDecimal.ZERO
import java.math.MathContext
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import kotlin.math.roundToInt

class Prosentdel private constructor(private val brøkdel: BigDecimal): Comparable<Prosentdel> {
    init {
        require(brøkdel.toDouble() in 0.0..1.0) { "Må være prosent mellom 0 og 100" }
    }

    companion object {
        private val mc = MathContext.DECIMAL128
        private val SIKKER_BRØK = 1.0.toBigDecimal(mc)
        private val HUNDRE_PROSENT = 100.0.toBigDecimal(mc)
        private val GRENSE = 20.prosent

        internal fun fraRatio(ratio: Double) = Prosentdel(ratio.toBigDecimal(mc))
        internal fun fraRatio(ratio: String) = Prosentdel(ratio.toBigDecimal(mc))

        internal fun subsumsjon(subsumsjonObserver: SubsumsjonObserver, block: SubsumsjonObserver.(Double) -> Unit) {
            subsumsjonObserver.block(GRENSE.toDouble())
        }

        internal fun Collection<Pair<Prosentdel, BigDecimal>>.average(): Prosentdel {
            val total = this.sumOf { it.second }
            if (total <= ZERO) return map { it.first to ONE }.average()
            return Prosentdel(this.sumOf { it.first.brøkdel.multiply(it.second, mc) }.divide(total, mc))
        }

        val Number.prosent get() = Prosentdel(this.toDouble().toBigDecimal(mc).divide(HUNDRE_PROSENT, mc))
    }

    override fun equals(other: Any?) = other is Prosentdel && this.equals(other)

    private fun equals(other: Prosentdel) = this.brøkdel == other.brøkdel

    override fun hashCode() = brøkdel.hashCode()

    internal operator fun not() = Prosentdel(SIKKER_BRØK - this.brøkdel)

    internal operator fun div(other: Prosentdel) = Prosentdel(this.brøkdel.divide(other.brøkdel, mc))

    override fun compareTo(other: Prosentdel) = this.brøkdel.compareTo(other.brøkdel)

    override fun toString(): String {
        return "${(toDouble())} %"
    }

    internal fun reciproc(other: Inntekt) = other.times(inverse())

    private fun inverse(): BigDecimal = SIKKER_BRØK.divide(this.brøkdel, mc)

    internal fun times(other: Inntekt) = other.times(brøkdel)

    internal fun toDouble() = brøkdel.multiply(HUNDRE_PROSENT, mc).toDouble()

    internal fun roundToInt() = toDouble().roundToInt()

    internal fun erUnderGrensen() = this < GRENSE
}
