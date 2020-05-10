package no.nav.helse.økonomi

internal class Grad private constructor(private val prosentdel: Prosentdel) :
    Comparable<Grad>
{

    companion object {
        internal val GRENSE = sykdomsgrad(20.0)

        internal fun sykdomsgrad(prosentdel: Number) = Grad(Prosentdel(prosentdel))

        internal fun arbeidshelse(prosentdel: Number) = Grad(!Prosentdel(prosentdel))

        internal fun vektlagtGjennomsnitt(beløp: List<Pair<Grad, Double>>): Grad {
            return Grad(Prosentdel.vektlagtGjennomsnitt(beløp.map { it.first.prosentdel to it.second }))
        }
    }

    override fun equals(other: Any?) = other is Grad && this.equals(other)

    private fun equals(other: Grad) = this.prosentdel == other.prosentdel

    override fun hashCode() = prosentdel.hashCode()

    override fun compareTo(other: Grad) = this.prosentdel.compareTo(other.prosentdel)

    override fun toString() = prosentdel.toString()

    internal fun toPercentage() = prosentdel.toDouble()
}
