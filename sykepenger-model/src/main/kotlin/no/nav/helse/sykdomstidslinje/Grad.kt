package no.nav.helse.sykdomstidslinje

import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.vektlagtGjennomsnitt

internal class Grad private constructor(private val prosentdel: Prosentdel) :
    Comparable<Grad>
{

    companion object {
        internal val GRENSE = sykdomsgrad(20.0)

        internal fun sykdomsgrad(prosentdel: Number) = Grad(Prosentdel(prosentdel))

        internal fun arbeidshelse(prosentdel: Number) = Grad(!Prosentdel(prosentdel))
    }

    override fun equals(other: Any?) = other is Grad && this.equals(other)

    private fun equals(other: Grad) = this.prosentdel == other.prosentdel

    override fun hashCode() = prosentdel.hashCode()

    override fun compareTo(other: Grad) = this.prosentdel.compareTo(other.prosentdel)

    override fun toString() = prosentdel.toString()

    internal fun toPercentage() = prosentdel.toDoublePercentage()

    internal fun lønn(beløp: Number) = LønnGrad(prosentdel, beløp.toDouble())

    internal class LønnGrad(private val prosentdel: Prosentdel, private val beløp: Double) {
        companion object {
            internal fun samletGrad(lønnGrader: List<LønnGrad>) =
                Grad(lønnGrader.map { it.prosentdel to it.beløp }.vektlagtGjennomsnitt())
        }

    }
}

internal fun List<Grad.LønnGrad>.samletGrad() = Grad.LønnGrad.samletGrad(this)
